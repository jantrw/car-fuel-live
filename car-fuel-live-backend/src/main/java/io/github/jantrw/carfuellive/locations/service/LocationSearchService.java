package io.github.jantrw.carfuellive.locations.service;

import io.github.jantrw.carfuellive.locations.dto.LocationSearchResponse;
import io.github.jantrw.carfuellive.locations.dto.LocationSearchResultResponse;
import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import io.github.jantrw.carfuellive.locations.repository.LocationSearchRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationSearchService {

  private static final Map<String, String> GERMAN_ADMIN1_NAMES = germanAdmin1Names();
  private static final int SEARCH_CANDIDATE_MULTIPLIER = 8;
  private static final int MAX_SEARCH_CANDIDATES = 64;

  private final LocationSearchRepository locationSearchRepository;

  public LocationSearchService(LocationSearchRepository locationSearchRepository) {
    this.locationSearchRepository = locationSearchRepository;
  }

  @Transactional(readOnly = true)
  public LocationSearchResponse suggest(String query, String countryCode, int limit) {
    return lookup(query, limit, normalizeCountryCode(countryCode));
  }

  // Prefer exact postal-code matches when input contains digits, then use exact name lookups
  // before prefix fallback so indexed queries win whenever the user provides a full name.
  private LocationSearchResponse lookup(
      String query, int limit, Optional<String> boostedCountryCode) {
    final String normalizedQuery = normalize(query);
    final String likePrefix = escapeLikePattern(normalizedQuery) + "%";
    final int candidateLimit = searchCandidateLimit(limit);
    final Map<String, LocationSearchResult> uniqueResults = new LinkedHashMap<>();
    final Comparator<LocationSearchResult> comparator = resultComparator(boostedCountryCode);

    final Optional<List<LocationSearchResult>> postalCodeResults =
        searchGermanPostalCodeResults(normalizedQuery, limit, comparator);
    if (postalCodeResults.isPresent()) {
      return toSearchResponse(postalCodeResults.get());
    }

    addExactResults(uniqueResults, normalizedQuery, candidateLimit, comparator);
    if (uniqueResults.isEmpty()) {
      addPrefixResults(uniqueResults, normalizedQuery, likePrefix, candidateLimit, comparator);
    }

    return toSearchResponse(
        limitVisibleResults(uniqueResults.values().stream().sorted(comparator).toList(), limit));
  }

  // Numeric-only input should keep the PLZ prefix path for incremental search. Mixed input should
  // prefer postal-code results only when it contains a standalone five-digit German PLZ, otherwise
  // arbitrary digit fragments such as house numbers would hide relevant place and country matches.
  private Optional<List<LocationSearchResult>> searchGermanPostalCodeResults(
      String normalizedQuery, int limit, Comparator<LocationSearchResult> comparator) {
    final Optional<String> numericOnlyPostalCodeQuery =
        numericOnlyPostalCodePrefix(normalizedQuery);
    if (numericOnlyPostalCodeQuery.isPresent()) {
      return searchGermanPostalCodePrefixResults(
          numericOnlyPostalCodeQuery.get(), limit, comparator);
    }

    return standaloneGermanPostalCode(normalizedQuery)
        .map(
            postalCodeQuery ->
                locationSearchRepository.searchGermanPostalCodesExact(postalCodeQuery, limit))
        .filter(results -> !results.isEmpty())
        .map(results -> results.stream().sorted(comparator).limit(limit).toList());
  }

  private Optional<List<LocationSearchResult>> searchGermanPostalCodePrefixResults(
      String postalCodeQuery, int limit, Comparator<LocationSearchResult> comparator) {
    final List<LocationSearchResult> exactMatches =
        locationSearchRepository.searchGermanPostalCodesExact(postalCodeQuery, limit);
    if (!exactMatches.isEmpty()) {
      return Optional.of(exactMatches.stream().sorted(comparator).limit(limit).toList());
    }

    final List<LocationSearchResult> prefixMatches =
        locationSearchRepository.searchGermanPostalCodes(
            postalCodeQuery, escapeLikePattern(postalCodeQuery) + "%", limit);
    if (prefixMatches.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(prefixMatches.stream().sorted(comparator).limit(limit).toList());
  }

  // Query families stay separate because each table has different ranking rules. Place rows use
  // geoname id identity first so same-name towns stay selectable before semantic duplicate
  // filtering collapses same-place admin/place overlaps. Each query overfetches a bounded
  // candidate window so the user-facing limit is enforced only after cross-query dedupe.
  private void addExactResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      int candidateLimit,
      Comparator<LocationSearchResult> comparator) {
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesExact(normalizedQuery, candidateLimit),
        LocationSearchService::idKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesExact(normalizedQuery, candidateLimit),
        LocationSearchService::idKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountriesExact(normalizedQuery, candidateLimit),
        LocationSearchService::idKey,
        comparator);
  }

  // Prefix fallback keeps short input useful while preserving fast exact lookups for full names.
  private void addPrefixResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      String likePrefix,
      int candidateLimit,
      Comparator<LocationSearchResult> comparator) {
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaces(normalizedQuery, likePrefix, candidateLimit),
        LocationSearchService::idKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliases(normalizedQuery, likePrefix, candidateLimit),
        LocationSearchService::idKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountries(normalizedQuery, likePrefix, candidateLimit),
        LocationSearchService::idKey,
        comparator);
  }

  private static void addBestResults(
      Map<String, LocationSearchResult> uniqueResults,
      List<LocationSearchResult> results,
      java.util.function.Function<LocationSearchResult, String> keyFactory,
      Comparator<LocationSearchResult> comparator) {
    for (LocationSearchResult result : results) {
      uniqueResults.merge(
          keyFactory.apply(result),
          result,
          (current, candidate) -> best(current, candidate, comparator));
    }
  }

  // The same visible location can arrive through primary names and aliases. Keep the best ranked
  // candidate after semantic dedupe.
  private static LocationSearchResult best(
      LocationSearchResult current,
      LocationSearchResult candidate,
      Comparator<LocationSearchResult> comparator) {
    return comparator.compare(candidate, current) < 0 ? candidate : current;
  }

  // SQL assigns matchRank by match quality. Java applies shared tie-breakers so results from
  // place, alias, country, and postal-code queries are ranked consistently.
  private static Comparator<LocationSearchResult> resultComparator(
      Optional<String> boostedCountryCode) {
    return Comparator.<LocationSearchResult>comparingInt(
            LocationSearchService::suggestionRankingRank)
        .thenComparingInt(result -> countryBoostRank(result, boostedCountryCode))
        .thenComparing(LocationSearchService::featureClassRank)
        .thenComparing(Comparator.comparingLong(LocationSearchResult::popularity).reversed())
        .thenComparing(LocationSearchResult::label)
        .thenComparing(LocationSearchResult::id);
  }

  private static int suggestionRankingRank(LocationSearchResult result) {
    if ("postalCode".equals(result.type())) {
      return result.matchRank();
    }

    return switch (result.matchRank()) {
      case 0 -> "place".equals(result.type()) ? 0 : 2;
      case 1 -> "place".equals(result.type()) ? 1 : 5;
      case 2 -> 3;
      case 3 -> 4;
      case 4 -> 6;
      case 5 -> 7;
      default -> result.matchRank();
    };
  }

  private static int countryBoostRank(
      LocationSearchResult result, Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isPresent()
        && boostedCountryCode.get().equalsIgnoreCase(result.countryCode())) {
      return 0;
    }
    return 1;
  }

  private static LocationSearchResponse toSearchResponse(List<LocationSearchResult> results) {
    final Map<String, Long> visibleGermanPlaceCounts = visibleGermanPlaceCounts(results);
    final List<LocationSearchResultResponse> items =
        results.stream()
            .map(
                result ->
                    toResponse(result, visibleGermanPlaceCounts.getOrDefault(result.id(), 0L) > 1))
            .toList();

    return new LocationSearchResponse(items);
  }

  private static Map<String, Long> visibleGermanPlaceCounts(List<LocationSearchResult> results) {
    final Map<String, Long> duplicateCountsByLabel = new HashMap<>();
    for (LocationSearchResult result : results) {
      if (!isGermanPlace(result)) {
        continue;
      }

      duplicateCountsByLabel.merge(result.label(), 1L, Long::sum);
    }

    final Map<String, Long> countsById = new HashMap<>();
    for (LocationSearchResult result : results) {
      if (!isGermanPlace(result)) {
        continue;
      }

      countsById.put(result.id(), duplicateCountsByLabel.getOrDefault(result.label(), 0L));
    }
    return countsById;
  }

  private static String idKey(LocationSearchResult result) {
    return result.type() + ":" + result.id();
  }

  private static int searchCandidateLimit(int visibleLimit) {
    return Math.min(
        MAX_SEARCH_CANDIDATES, Math.max(visibleLimit, visibleLimit * SEARCH_CANDIDATE_MULTIPLIER));
  }

  private static List<LocationSearchResult> limitVisibleResults(
      List<LocationSearchResult> sortedResults, int limit) {
    final List<LocationSearchResult> visibleResults = new ArrayList<>();
    for (LocationSearchResult candidate : sortedResults) {
      if (isSemanticPlaceDuplicateOfAny(visibleResults, candidate)) {
        continue;
      }

      visibleResults.add(candidate);
      if (visibleResults.size() == limit) {
        return visibleResults;
      }
    }
    return visibleResults;
  }

  private static boolean isSemanticPlaceDuplicateOfAny(
      List<LocationSearchResult> visibleResults, LocationSearchResult candidate) {
    for (LocationSearchResult visibleResult : visibleResults) {
      if (isAdministrativePlaceDuplicate(visibleResult, candidate)) {
        return true;
      }
    }
    return false;
  }

  // GeoNames can emit the same municipality as both a populated place and an administrative row.
  // Collapse only that mixed P/non-P pair; distinct same-name places keep separate ids.
  private static boolean isAdministrativePlaceDuplicate(
      LocationSearchResult current, LocationSearchResult candidate) {
    if (!"place".equals(current.type()) || !"place".equals(candidate.type())) {
      return false;
    }
    if (!Objects.equals(current.countryCode(), candidate.countryCode())) {
      return false;
    }
    if (!normalize(current.label()).equals(normalize(candidate.label()))) {
      return false;
    }
    if (!hasAdministrativePath(current) || !hasAdministrativePath(candidate)) {
      return false;
    }
    if (!hasSameAdministrativePath(current, candidate)) {
      return false;
    }

    return !Objects.equals(current.featureClass(), candidate.featureClass())
        && ("P".equals(current.featureClass()) || "P".equals(candidate.featureClass()));
  }

  private static boolean hasAdministrativePath(LocationSearchResult result) {
    return result.admin1Code() != null
        || result.admin2Code() != null
        || result.admin3Code() != null
        || result.admin4Code() != null;
  }

  private static boolean hasSameAdministrativePath(
      LocationSearchResult current, LocationSearchResult candidate) {
    return Objects.equals(current.admin1Code(), candidate.admin1Code())
        && Objects.equals(current.admin2Code(), candidate.admin2Code())
        && Objects.equals(current.admin3Code(), candidate.admin3Code())
        && Objects.equals(current.admin4Code(), candidate.admin4Code());
  }

  private static int featureClassRank(LocationSearchResult result) {
    return "P".equals(result.featureClass()) ? 0 : 1;
  }

  // Incremental PLZ search should only trigger when the whole query is a plausible postal-code
  // prefix. Mixed text such as "A1" or "Neustadt 2" must stay on the regular name lookup path.
  private static Optional<String> numericOnlyPostalCodePrefix(String value) {
    if (value.isEmpty() || value.length() > 5) {
      return Optional.empty();
    }

    for (int index = 0; index < value.length(); index++) {
      if (!Character.isDigit(value.charAt(index))) {
        return Optional.empty();
      }
    }

    return Optional.of(value);
  }

  // Free-text input may still carry a real embedded PLZ such as "Sankt Augustin 53757". Only a
  // standalone five-digit token should take the postal-code fast path.
  private static Optional<String> standaloneGermanPostalCode(String value) {
    final StringBuilder digits = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      final char character = value.charAt(index);
      if (Character.isDigit(character)) {
        digits.append(character);
        continue;
      }

      final Optional<String> postalCode = exactGermanPostalCodeToken(digits);
      if (postalCode.isPresent()) {
        return postalCode;
      }

      digits.setLength(0);
    }
    return exactGermanPostalCodeToken(digits);
  }

  private static Optional<String> exactGermanPostalCodeToken(StringBuilder digits) {
    return digits.length() == 5 ? Optional.of(digits.toString()) : Optional.empty();
  }

  private static LocationSearchResultResponse toResponse(
      LocationSearchResult result, boolean includeGermanAdmin1Name) {
    return new LocationSearchResultResponse(
        result.type(),
        result.id(),
        displayLabel(result, includeGermanAdmin1Name),
        result.countryCode(),
        responseLatitude(result),
        responseLongitude(result),
        result.postalCode());
  }

  private static Double responseLatitude(LocationSearchResult result) {
    if ("country".equals(result.type())) {
      return null;
    }
    return result.latitude();
  }

  private static Double responseLongitude(LocationSearchResult result) {
    if ("country".equals(result.type())) {
      return null;
    }
    return result.longitude();
  }

  private static String displayLabel(LocationSearchResult result, boolean includeGermanAdmin1Name) {
    if (!includeGermanAdmin1Name || !isGermanPlace(result)) {
      return result.label();
    }

    final String admin1Name = GERMAN_ADMIN1_NAMES.get(result.admin1Code());
    if (admin1Name == null) {
      return result.label();
    }

    final int separatorIndex = result.label().lastIndexOf(", ");
    if (separatorIndex < 0) {
      return result.label();
    }

    final String placeName = result.label().substring(0, separatorIndex);
    final String countryName = result.label().substring(separatorIndex + 2);
    return placeName + ", " + admin1Name + ", " + countryName;
  }

  private static boolean isGermanPlace(LocationSearchResult result) {
    return "place".equals(result.type()) && "DE".equals(result.countryCode());
  }

  private static Map<String, String> germanAdmin1Names() {
    final Map<String, String> admin1Names = new HashMap<>();
    admin1Names.put("01", "Baden-Württemberg");
    admin1Names.put("02", "Bayern");
    admin1Names.put("03", "Bremen");
    admin1Names.put("04", "Hamburg");
    admin1Names.put("05", "Hessen");
    admin1Names.put("06", "Niedersachsen");
    admin1Names.put("07", "Nordrhein-Westfalen");
    admin1Names.put("08", "Rheinland-Pfalz");
    admin1Names.put("09", "Saarland");
    admin1Names.put("10", "Schleswig-Holstein");
    admin1Names.put("11", "Brandenburg");
    admin1Names.put("12", "Mecklenburg-Vorpommern");
    admin1Names.put("13", "Sachsen");
    admin1Names.put("14", "Sachsen-Anhalt");
    admin1Names.put("15", "Thüringen");
    admin1Names.put("16", "Berlin");
    return Map.copyOf(admin1Names);
  }

  private static String normalize(String value) {
    final String withoutMarks =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return withoutMarks.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private static Optional<String> normalizeCountryCode(String countryCode) {
    if (countryCode == null) {
      return Optional.empty();
    }

    final String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(normalized);
  }

  private static String escapeLikePattern(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
