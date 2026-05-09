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

  // Prefer exact postal-code matches when input contains digits, then use exact name lookups
  // before prefix fallback so indexed queries win whenever the user provides a full name.
  @Transactional(readOnly = true)
  public LocationSearchResponse search(String query, int limit) {
    final String normalizedQuery = normalize(query);
    final String likePrefix = escapeLikePattern(normalizedQuery) + "%";
    final int candidateLimit = searchCandidateLimit(limit);
    final Map<String, LocationSearchResult> uniqueResults = new LinkedHashMap<>();

    final Optional<List<LocationSearchResult>> postalCodeResults =
        searchGermanPostalCodeResults(normalizedQuery, limit);
    if (postalCodeResults.isPresent()) {
      return toSearchResponse(postalCodeResults.get());
    }

    addExactResults(uniqueResults, normalizedQuery, candidateLimit);
    if (uniqueResults.isEmpty()) {
      addPrefixResults(uniqueResults, normalizedQuery, likePrefix, candidateLimit);
    }

    return toSearchResponse(
        limitVisibleResults(
            uniqueResults.values().stream().sorted(resultComparator()).toList(), limit));
  }

  // Numeric input should prefer German PLZ matches before generic GeoNames prefix lookup so
  // prefixes such as "101" or embedded fragments such as "Sankt Augustin 5375" stay on the
  // postal-code path instead of surfacing unrelated European place names.
  private Optional<List<LocationSearchResult>> searchGermanPostalCodeResults(
      String normalizedQuery, int limit) {
    return firstDigitSequence(normalizedQuery)
        .map(
            postalCodeQuery -> {
              final List<LocationSearchResult> exactMatches =
                  locationSearchRepository.searchGermanPostalCodesExact(postalCodeQuery, limit);
              if (!exactMatches.isEmpty()) {
                return exactMatches;
              }

              return locationSearchRepository.searchGermanPostalCodes(
                  postalCodeQuery, escapeLikePattern(postalCodeQuery) + "%", limit);
            })
        .filter(results -> !results.isEmpty())
        .map(results -> results.stream().sorted(resultComparator()).limit(limit).toList());
  }

  // Query families stay separate because each table has different ranking rules. Place rows use
  // geoname id identity first so same-name towns stay selectable before semantic duplicate
  // filtering collapses same-place admin/place overlaps. Each query overfetches a bounded
  // candidate window so the user-facing limit is enforced only after cross-query dedupe.
  private void addExactResults(
      Map<String, LocationSearchResult> uniqueResults, String normalizedQuery, int candidateLimit) {
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesExact(normalizedQuery, candidateLimit),
        LocationSearchService::idKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesExact(normalizedQuery, candidateLimit),
        LocationSearchService::idKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountriesExact(normalizedQuery, candidateLimit),
        LocationSearchService::idKey);
  }

  // Prefix fallback keeps short input useful while preserving fast exact lookups for full names.
  private void addPrefixResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      String likePrefix,
      int candidateLimit) {
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaces(normalizedQuery, likePrefix, candidateLimit),
        LocationSearchService::idKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliases(normalizedQuery, likePrefix, candidateLimit),
        LocationSearchService::idKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountries(normalizedQuery, likePrefix, candidateLimit),
        LocationSearchService::idKey);
  }

  private static void addBestResults(
      Map<String, LocationSearchResult> uniqueResults,
      List<LocationSearchResult> results,
      java.util.function.Function<LocationSearchResult, String> keyFactory) {
    for (LocationSearchResult result : results) {
      uniqueResults.merge(keyFactory.apply(result), result, LocationSearchService::best);
    }
  }

  // The same visible location can arrive through primary names and aliases. Keep the best ranked
  // candidate after semantic dedupe.
  private static LocationSearchResult best(
      LocationSearchResult current, LocationSearchResult candidate) {
    return resultComparator().compare(candidate, current) < 0 ? candidate : current;
  }

  // SQL assigns matchRank by match quality. Java applies shared tie-breakers so results from
  // place, alias, country, and postal-code queries are ranked consistently.
  private static Comparator<LocationSearchResult> resultComparator() {
    return Comparator.comparingInt(LocationSearchResult::matchRank)
        .thenComparing(LocationSearchService::featureClassRank)
        .thenComparing(Comparator.comparingLong(LocationSearchResult::popularity).reversed())
        .thenComparing(LocationSearchResult::label)
        .thenComparing(LocationSearchResult::id);
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

  // German postal codes are numeric. The first digit run captures input such as
  // "Sankt Augustin 53757" without treating free text as a postal-code lookup.
  private static Optional<String> firstDigitSequence(String value) {
    final StringBuilder digits = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      final char character = value.charAt(index);
      if (Character.isDigit(character)) {
        digits.append(character);
      } else if (!digits.isEmpty()) {
        return Optional.of(digits.toString());
      }
    }
    return digits.isEmpty() ? Optional.empty() : Optional.of(digits.toString());
  }

  private static LocationSearchResultResponse toResponse(LocationSearchResult result) {
    return toResponse(result, false);
  }

  private static LocationSearchResultResponse toResponse(
      LocationSearchResult result, boolean includeGermanAdmin1Name) {
    return new LocationSearchResultResponse(
        result.type(),
        result.id(),
        displayLabel(result, includeGermanAdmin1Name),
        result.countryCode(),
        result.latitude(),
        result.longitude(),
        result.postalCode());
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

  private static String escapeLikePattern(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
