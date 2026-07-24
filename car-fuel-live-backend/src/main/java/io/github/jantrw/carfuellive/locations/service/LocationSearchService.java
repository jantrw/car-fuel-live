package io.github.jantrw.carfuellive.locations.service;

import io.github.jantrw.carfuellive.locations.dto.LocationSearchResponse;
import io.github.jantrw.carfuellive.locations.dto.LocationSearchResultResponse;
import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import io.github.jantrw.carfuellive.locations.repository.LocationSearchRepository;
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

/**
 * Orchestrates location suggestion lookup across exact, prefix, alias, and postal-code paths.
 *
 * <p>The service merges repository result sets, applies cross-query ranking rules, and removes
 * semantic duplicates before projecting the final public response.
 */
@Service
public class LocationSearchService {

  private static final Map<String, String> GERMAN_ADMIN1_NAMES = germanAdmin1Names();
  private static final int SHORT_PREFIX_MAX_LENGTH = 3;
  private static final long CLEAR_EXACT_PLACE_POPULARITY_MIN = 10_000;
  private static final long CLEAR_EXACT_ALIAS_POPULARITY_MIN = 100_000;
  private static final long TOP_GERMAN_CITY_POPULARITY_MIN = 100_000;
  private static final double SAME_LOCATION_COORDINATE_DELTA = 0.01;
  private static final int CONTEXT_VISIBLE_TARGET = 5;
  private static final int GLOBAL_VISIBLE_TARGET = 3;
  private static final int SEARCH_CANDIDATE_MULTIPLIER = 8;
  private static final int MAX_SEARCH_CANDIDATES = 64;

  private final LocationSearchRepository locationSearchRepository;

  public LocationSearchService(LocationSearchRepository locationSearchRepository) {
    this.locationSearchRepository = locationSearchRepository;
  }

  @Transactional(readOnly = true)
  public LocationSearchResponse suggest(String query, String countryCode, int limit) {
    return searchLocationSuggestions(query, limit, normalizeCountryCode(countryCode));
  }

  /**
   * Builds a bounded, user-visible suggestion list from the local dataset.
   *
   * <p>Postal-code input has priority. Other input first gathers exact matches, then adds bounded
   * prefix and near-prefix candidates only when no clear exact intent exists. Candidate families
   * are ranked together and semantically deduplicated only after database retrieval so distinct
   * same-name towns remain selectable.
   */
  private LocationSearchResponse searchLocationSuggestions(
      String query, int limit, Optional<String> boostedCountryCode) {
    final String normalizedQuery = normalize(query);
    final String prefixEnd = prefixEnd(normalizedQuery);
    final int candidateLimit = searchCandidateLimit(limit);
    final Map<String, LocationSearchResult> uniqueResults = new LinkedHashMap<>();
    final Comparator<LocationSearchResult> comparator =
        createSearchResultComparator(normalizedQuery, boostedCountryCode);
    final boolean shouldUsePrefixFallback;

    final Optional<List<LocationSearchResult>> postalCodeResults =
        searchGermanPostalCodeResults(normalizedQuery, candidateLimit, comparator);
    if (postalCodeResults.isPresent()) {
      return toSearchResponse(
          postalCodeResults.get().stream().limit(limit).toList(),
          hasDirectResolution(postalCodeResults.get(), normalizedQuery));
    }

    addExactResults(uniqueResults, normalizedQuery, candidateLimit, comparator, boostedCountryCode);
    addKnownTopCityVariantResults(
        uniqueResults, normalizedQuery, candidateLimit, comparator, boostedCountryCode);
    shouldUsePrefixFallback =
        shouldAddPrefixFallback(uniqueResults.values(), normalizedQuery, boostedCountryCode);
    if (shouldUsePrefixFallback) {
      addPrefixResults(
          uniqueResults,
          normalizedQuery,
          prefixEnd,
          candidateLimit,
          limit,
          comparator,
          boostedCountryCode);
      addNearPrefixResults(
          uniqueResults, normalizedQuery, candidateLimit, comparator, boostedCountryCode);
    }

    final List<LocationSearchResult> filteredResults =
        filterConfusingExactAliases(
            uniqueResults.values().stream().sorted(comparator).toList(), normalizedQuery);
    final List<LocationSearchResult> deduplicatedResults =
        collectVisibleResults(filteredResults, filteredResults.size());
    return toSearchResponse(
        limitVisibleResults(
            deduplicatedResults,
            limit,
            normalizedQuery,
            boostedCountryCode,
            shouldUsePrefixFallback),
        hasDirectResolution(deduplicatedResults, normalizedQuery));
  }

  // Exact lookups should usually short-circuit to stay index-friendly. Keep the prefix fallback
  // only when the current exact set does not contain a clear full-intent match, otherwise
  // unfinished input such as "berli" gets trapped behind obscure exact rows like "Berli".
  private static boolean shouldAddPrefixFallback(
      java.util.Collection<LocationSearchResult> exactResults,
      String normalizedQuery,
      Optional<String> boostedCountryCode) {
    if (exactResults.isEmpty()) {
      return true;
    }

    if (shouldKeepLookingForLocalPrefix(exactResults, normalizedQuery, boostedCountryCode)) {
      return true;
    }

    return exactResults.stream().noneMatch(result -> isClearExactIntent(result, normalizedQuery));
  }

  private static boolean shouldKeepLookingForLocalPrefix(
      java.util.Collection<LocationSearchResult> exactResults,
      String normalizedQuery,
      Optional<String> boostedCountryCode) {
    return boostedCountryCode.isPresent()
        && normalizedQuery.length() > SHORT_PREFIX_MAX_LENGTH + 1
        && exactResults.stream()
            .noneMatch(
                result ->
                    boostedCountryCode.get().equalsIgnoreCase(result.countryCode())
                        && isClearExactIntent(result, normalizedQuery));
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
      Comparator<LocationSearchResult> comparator,
      Optional<String> boostedCountryCode) {
    addContextCountryExactResults(
        uniqueResults, normalizedQuery, candidateLimit, comparator, boostedCountryCode);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesExact(normalizedQuery, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesExact(normalizedQuery, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountriesExact(normalizedQuery, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
  }

  // Prefix fallback keeps short input useful while preserving fast exact lookups for full names.
  private void addPrefixResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      String prefixEnd,
      int candidateLimit,
      int visibleLimit,
      Comparator<LocationSearchResult> comparator,
      Optional<String> boostedCountryCode) {
    addContextCountryPrefixResults(
        uniqueResults, normalizedQuery, prefixEnd, candidateLimit, comparator, boostedCountryCode);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaces(normalizedQuery, prefixEnd, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountries(normalizedQuery, prefixEnd, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    if (uniqueResults.size() < visibleLimit) {
      addBestResults(
          uniqueResults,
          locationSearchRepository.searchPlaceAliases(normalizedQuery, prefixEnd, candidateLimit),
          LocationSearchService::resultIdentityKey,
          comparator);
    }
  }

  private void addNearPrefixResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      int candidateLimit,
      Comparator<LocationSearchResult> comparator,
      Optional<String> boostedCountryCode) {
    if (normalizedQuery.length() <= SHORT_PREFIX_MAX_LENGTH
        || boostedCountryCode.isEmpty()
        || !"DE".equals(boostedCountryCode.get())) {
      return;
    }

    final String nearPrefix = normalizedQuery.substring(0, normalizedQuery.length() - 1);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesInCountry(
            normalizedQuery, prefixEnd(nearPrefix), "DE", candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesInCountry(
            normalizedQuery, prefixEnd(nearPrefix), "DE", candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
  }

  private void addKnownTopCityVariantResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      int candidateLimit,
      Comparator<LocationSearchResult> comparator,
      Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isEmpty() || !"DE".equals(boostedCountryCode.get())) {
      return;
    }

    final Optional<String> variant = knownTopCityVariant(normalizedQuery);
    if (variant.isEmpty()) {
      return;
    }

    final String variantQuery = variant.get();
    final String variantPrefixEnd = prefixEnd(variantQuery);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesInCountry(
            variantQuery, variantPrefixEnd, "DE", candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesInCountry(
            variantQuery, variantPrefixEnd, "DE", candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
  }

  // The context country must influence candidate collection, not only final sorting. Otherwise a
  // bounded global overfetch window can still exclude locally relevant places before the boost is
  // applied.
  private void addContextCountryExactResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      int candidateLimit,
      Comparator<LocationSearchResult> comparator,
      Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isEmpty()) {
      return;
    }

    final String countryCode = boostedCountryCode.get();
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesExactInCountry(
            normalizedQuery, countryCode, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesExactInCountry(
            normalizedQuery, countryCode, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
  }

  private void addContextCountryPrefixResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      String prefixEnd,
      int candidateLimit,
      Comparator<LocationSearchResult> comparator,
      Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isEmpty()) {
      return;
    }

    final String countryCode = boostedCountryCode.get();
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesInCountry(
            normalizedQuery, prefixEnd, countryCode, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesInCountry(
            normalizedQuery, prefixEnd, countryCode, candidateLimit),
        LocationSearchService::resultIdentityKey,
        comparator);
  }

  private static void addBestResults(
      Map<String, LocationSearchResult> uniqueResults,
      List<LocationSearchResult> results,
      java.util.function.Function<LocationSearchResult, String> keyFactory,
      Comparator<LocationSearchResult> comparator) {
    if (results == null || results.isEmpty()) {
      return;
    }

    for (LocationSearchResult result : results) {
      uniqueResults.merge(
          keyFactory.apply(result),
          result,
          (current, candidate) -> selectHigherRankedResult(current, candidate, comparator));
    }
  }

  // The same visible location can arrive through primary names and aliases. Keep the best ranked
  // candidate after semantic dedupe.
  private static LocationSearchResult selectHigherRankedResult(
      LocationSearchResult current,
      LocationSearchResult candidate,
      Comparator<LocationSearchResult> comparator) {
    return comparator.compare(candidate, current) < 0 ? candidate : current;
  }

  // SQL assigns matchRank by match quality. Java applies shared tie-breakers so results from
  // place, alias, country, and postal-code queries are ranked consistently.
  private static Comparator<LocationSearchResult> createSearchResultComparator(
      String normalizedQuery, Optional<String> boostedCountryCode) {
    return Comparator.<LocationSearchResult>comparingInt(
            result -> intentRank(result, normalizedQuery, boostedCountryCode))
        .thenComparingInt(result -> suggestionRankingRank(result, normalizedQuery))
        .thenComparingInt(result -> countryBoostRank(result, normalizedQuery, boostedCountryCode))
        .thenComparing(LocationSearchService::featureClassRank)
        .thenComparing(Comparator.comparingLong(LocationSearchResult::popularity).reversed())
        .thenComparing(LocationSearchResult::label)
        .thenComparing(LocationSearchResult::id);
  }

  // A literal exact row is not automatically clear user intent. Obscure low-population places
  // such as "Berli", "Mei", or "Pari" should not outrank an unfinished stronger prefix such as
  // "Berlin", "Meissen", or "Paris" just because GeoNames contains the shorter exact string.
  // Treat only exact countries, postal codes, and materially populated exact places as clear
  // full-name intent; otherwise allow stronger prefix candidates to lead.
  private static int intentRank(
      LocationSearchResult result, String normalizedQuery, Optional<String> boostedCountryCode) {
    if (isClearExactIntent(result, normalizedQuery)
        && !isLongForeignExactMatch(result, normalizedQuery, boostedCountryCode)) {
      return 0;
    }
    if (isKnownTopCityVariantMatch(result, normalizedQuery)) {
      return 1;
    }
    if (isContextPreferredPrefix(result, normalizedQuery, boostedCountryCode)) {
      return 1;
    }
    if (isNearTopGermanCityPrefix(result, normalizedQuery, boostedCountryCode)) {
      return 1;
    }
    if (isStrongPrefixContinuation(result, normalizedQuery)) {
      return 2;
    }
    return 3;
  }

  private static int suggestionRankingRank(LocationSearchResult result, String normalizedQuery) {
    if ("postalCode".equals(result.type())) {
      return result.matchRank();
    }

    if (isKnownTopCityVariantMatch(result, normalizedQuery)) {
      return "place".equals(result.type()) ? 0 : 5;
    }

    if ((result.matchRank() == 4 || result.matchRank() == 5)
        && isPrimaryNamePrefixMatch(result, normalizedQuery)) {
      return "place".equals(result.type()) ? 2 : 5;
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
      LocationSearchResult result, String normalizedQuery, Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isEmpty()
        || !boostedCountryCode.get().equalsIgnoreCase(result.countryCode())) {
      return 1;
    }

    if (normalizedQuery.length() <= SHORT_PREFIX_MAX_LENGTH
        || isExactNameMatch(result, normalizedQuery)) {
      return 0;
    }

    return 1;
  }

  private static boolean isClearExactIntent(LocationSearchResult result, String normalizedQuery) {
    if ("postalCode".equals(result.type())) {
      return normalizedQuery.equals(result.postalCode());
    }
    if ("country".equals(result.type())) {
      return normalize(result.label()).equals(normalizedQuery);
    }
    if (!"place".equals(result.type())) {
      return false;
    }

    if (isExactAliasIntent(result, normalizedQuery)) {
      return true;
    }

    return "P".equals(result.featureClass())
        && isExactNameMatch(result, normalizedQuery)
        && result.popularity() >= CLEAR_EXACT_PLACE_POPULARITY_MIN;
  }

  private static boolean isContextPreferredPrefix(
      LocationSearchResult result, String normalizedQuery, Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isEmpty()
        || !"place".equals(result.type())
        || !boostedCountryCode.get().equalsIgnoreCase(result.countryCode())) {
      return false;
    }

    return isPrimaryNamePrefixMatch(result, normalizedQuery);
  }

  private static boolean isLowConfidenceExactMatch(
      LocationSearchResult result, String normalizedQuery) {
    return "place".equals(result.type())
        && isExactNameMatch(result, normalizedQuery)
        && !isClearExactIntent(result, normalizedQuery);
  }

  private static boolean isLongForeignExactMatch(
      LocationSearchResult result, String normalizedQuery, Optional<String> boostedCountryCode) {
    return boostedCountryCode.isPresent()
        && normalizedQuery.length() > SHORT_PREFIX_MAX_LENGTH + 1
        && !boostedCountryCode.get().equalsIgnoreCase(result.countryCode())
        && result.popularity() < TOP_GERMAN_CITY_POPULARITY_MIN
        && isClearExactIntent(result, normalizedQuery);
  }

  private static boolean isExactAliasIntent(LocationSearchResult result, String normalizedQuery) {
    if (!"place".equals(result.type()) || (result.matchRank() != 2 && result.matchRank() != 3)) {
      return false;
    }

    if (normalize(primaryName(result)).startsWith(normalizedQuery)) {
      return false;
    }

    if (matchesFoldedPrimaryVariant(result, normalizedQuery)) {
      return result.popularity() >= CLEAR_EXACT_PLACE_POPULARITY_MIN;
    }

    return normalizedQuery.length() > SHORT_PREFIX_MAX_LENGTH + 1
        && result.popularity() >= CLEAR_EXACT_ALIAS_POPULARITY_MIN;
  }

  private static boolean matchesFoldedPrimaryVariant(
      LocationSearchResult result, String normalizedQuery) {
    return LocationSearchNormalizer.normalizeFolded(primaryName(result)).equals(normalizedQuery);
  }

  private static boolean isStrongPrefixContinuation(
      LocationSearchResult result, String normalizedQuery) {
    if (!"place".equals(result.type())) {
      return false;
    }

    return isPrimaryNamePrefixMatch(result, normalizedQuery)
        && "P".equals(result.featureClass())
        && result.popularity() >= CLEAR_EXACT_PLACE_POPULARITY_MIN;
  }

  private static boolean isNearTopGermanCityPrefix(
      LocationSearchResult result, String normalizedQuery, Optional<String> boostedCountryCode) {
    if (boostedCountryCode.isEmpty()
        || !"DE".equals(boostedCountryCode.get())
        || !"DE".equals(result.countryCode())
        || !"P".equals(result.featureClass())
        || result.popularity() < TOP_GERMAN_CITY_POPULARITY_MIN) {
      return false;
    }

    return isNearPrimaryNamePrefixMatch(result, normalizedQuery);
  }

  private static boolean isPrimaryNamePrefixMatch(
      LocationSearchResult result, String normalizedQuery) {
    final String primaryName = primaryName(result);
    final String normalizedPrimaryName = normalize(primaryName);
    final String foldedPrimaryName = LocationSearchNormalizer.normalizeFolded(primaryName);
    return (normalizedPrimaryName.startsWith(normalizedQuery)
            || foldedPrimaryName.startsWith(normalizedQuery))
        && !normalizedPrimaryName.equals(normalizedQuery)
        && !foldedPrimaryName.equals(normalizedQuery);
  }

  private static boolean isNearPrimaryNamePrefixMatch(
      LocationSearchResult result, String normalizedQuery) {
    final String primaryName = primaryName(result);
    return hasOneEditPrefix(LocationSearchNormalizer.normalize(primaryName), normalizedQuery)
        || hasOneEditPrefix(LocationSearchNormalizer.normalizeFolded(primaryName), normalizedQuery);
  }

  private static boolean hasOneEditPrefix(String candidate, String normalizedQuery) {
    if (candidate.length() <= normalizedQuery.length()) {
      return false;
    }

    final String candidatePrefix = candidate.substring(0, normalizedQuery.length());
    int differences = 0;
    for (int index = 0; index < normalizedQuery.length(); index++) {
      if (candidatePrefix.charAt(index) != normalizedQuery.charAt(index)) {
        differences++;
        if (differences > 1) {
          return false;
        }
      }
    }
    return differences == 1;
  }

  private static Optional<String> knownTopCityVariant(String normalizedQuery) {
    return switch (normalizedQuery) {
      case "munc", "munch" -> Optional.of("munic");
      default -> Optional.empty();
    };
  }

  private static boolean isKnownTopCityVariantMatch(
      LocationSearchResult result, String normalizedQuery) {
    return knownTopCityVariant(normalizedQuery)
        .filter(
            variant ->
                "DE".equals(result.countryCode())
                    && "P".equals(result.featureClass())
                    && result.popularity() >= TOP_GERMAN_CITY_POPULARITY_MIN
                    && normalize(primaryName(result)).startsWith(variant))
        .isPresent();
  }

  // The context country should strongly steer short ambiguous prefixes such as "Wi". For longer
  // searches, keep the country preference only as a tie-breaker for true exact name matches such
  // as multiple places called "Paris" so clear user intent still wins.
  private static boolean isExactNameMatch(LocationSearchResult result, String normalizedQuery) {
    if ("postalCode".equals(result.type())) {
      return normalizedQuery.equals(result.postalCode());
    }

    return normalize(primaryName(result)).equals(normalizedQuery);
  }

  private static String primaryName(LocationSearchResult result) {
    if ("country".equals(result.type())) {
      return result.label();
    }

    final int separatorIndex = result.label().indexOf(", ");
    return separatorIndex < 0 ? result.label() : result.label().substring(0, separatorIndex);
  }

  // German same-label places need one extra disambiguator in the visible label; other countries
  // already remain understandable with the default place + country text.
  private static LocationSearchResponse toSearchResponse(
      List<LocationSearchResult> results, boolean hasDirectResolution) {
    final Map<String, Long> visibleGermanPlaceCounts = visibleGermanPlaceCounts(results);
    final List<LocationSearchResultResponse> items =
        results.stream()
            .map(
                result ->
                    toResponse(
                        result,
                        visibleGermanPlaceCounts.getOrDefault(result.id(), 0L) > 1,
                        hasDirectResolution))
            .toList();

    return new LocationSearchResponse(items);
  }

  private static boolean hasDirectResolution(
      List<LocationSearchResult> results, String normalizedQuery) {
    return results.size() == 1 && isDirectlyResolvable(results.getFirst(), normalizedQuery);
  }

  // The client must not infer intent from display labels. Only an unambiguous, clear exact place
  // or postal-code match is safe to resolve immediately after the user's explicit submit.
  private static boolean isDirectlyResolvable(LocationSearchResult result, String normalizedQuery) {
    return !"country".equals(result.type())
        && result.latitude() != null
        && result.longitude() != null
        && isClearExactIntent(result, normalizedQuery);
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

  private static String resultIdentityKey(LocationSearchResult result) {
    return result.type() + ":" + result.id();
  }

  private static int searchCandidateLimit(int visibleLimit) {
    return Math.min(
        MAX_SEARCH_CANDIDATES, Math.max(visibleLimit, visibleLimit * SEARCH_CANDIDATE_MULTIPLIER));
  }

  private static List<LocationSearchResult> limitVisibleResults(
      List<LocationSearchResult> sortedResults,
      int limit,
      String normalizedQuery,
      Optional<String> boostedCountryCode,
      boolean mixedPrefixLayout) {
    if (!mixedPrefixLayout || boostedCountryCode.isEmpty()) {
      return collectVisibleResults(sortedResults, limit);
    }

    return collectMixedVisibleResults(sortedResults, limit, normalizedQuery, boostedCountryCode);
  }

  private static List<LocationSearchResult> filterConfusingExactAliases(
      List<LocationSearchResult> sortedResults, String normalizedQuery) {
    if (sortedResults.stream()
        .noneMatch(result -> isDominantExactPrimaryPlace(result, normalizedQuery))) {
      return sortedResults;
    }

    return sortedResults.stream()
        .filter(result -> !isLowPopularityExactAlias(result, normalizedQuery))
        .toList();
  }

  private static boolean isDominantExactPrimaryPlace(
      LocationSearchResult result, String normalizedQuery) {
    return "place".equals(result.type())
        && result.matchRank() == 0
        && isExactNameMatch(result, normalizedQuery)
        && result.popularity() >= CLEAR_EXACT_ALIAS_POPULARITY_MIN;
  }

  private static boolean isLowPopularityExactAlias(
      LocationSearchResult result, String normalizedQuery) {
    return "place".equals(result.type())
        && (result.matchRank() == 2 || result.matchRank() == 3)
        && !isExactAliasIntent(result, normalizedQuery);
  }

  // Prefix searches should not collapse into a DE-only list. Build the visible slice in phases:
  // first context-heavy local continuations, then a bounded global window for direct or near-direct
  // alternatives, then fill any remaining slots from the regular ranked order.
  private static List<LocationSearchResult> collectMixedVisibleResults(
      List<LocationSearchResult> sortedResults,
      int limit,
      String normalizedQuery,
      Optional<String> boostedCountryCode) {
    final List<LocationSearchResult> clearExactIntents = new ArrayList<>();
    final List<LocationSearchResult> contextPreferredPrefixes = new ArrayList<>();
    final List<LocationSearchResult> lowConfidenceExactMatches = new ArrayList<>();
    final List<LocationSearchResult> globalStrongPrefixes = new ArrayList<>();
    final List<LocationSearchResult> rest = new ArrayList<>();

    for (LocationSearchResult result : sortedResults) {
      if (isClearExactIntent(result, normalizedQuery)
          && !isLongForeignExactMatch(result, normalizedQuery, boostedCountryCode)) {
        clearExactIntents.add(result);
      } else if (isKnownTopCityVariantMatch(result, normalizedQuery)) {
        contextPreferredPrefixes.add(result);
      } else if (isNearTopGermanCityPrefix(result, normalizedQuery, boostedCountryCode)) {
        contextPreferredPrefixes.add(result);
      } else if (isContextPreferredPrefix(result, normalizedQuery, boostedCountryCode)) {
        contextPreferredPrefixes.add(result);
      } else if (isLowConfidenceExactMatch(result, normalizedQuery)) {
        lowConfidenceExactMatches.add(result);
      } else if (isStrongPrefixContinuation(result, normalizedQuery)) {
        globalStrongPrefixes.add(result);
      } else {
        rest.add(result);
      }
    }

    final List<LocationSearchResult> visibleResults = new ArrayList<>();
    appendDistinctResultsFromBucket(visibleResults, clearExactIntents, limit, limit);

    final int contextSlots = Math.min(limit - visibleResults.size(), CONTEXT_VISIBLE_TARGET);
    appendDistinctResultsFromBucket(visibleResults, contextPreferredPrefixes, contextSlots, limit);

    final int globalSlots = Math.min(limit - visibleResults.size(), GLOBAL_VISIBLE_TARGET);
    final int reservedLowConfidenceExactSlots =
        lowConfidenceExactMatches.isEmpty() ? 0 : Math.min(1, globalSlots);
    appendDistinctResultsFromBucket(
        visibleResults, lowConfidenceExactMatches, reservedLowConfidenceExactSlots, limit);
    appendDistinctResultsFromBucket(
        visibleResults, globalStrongPrefixes, globalSlots - reservedLowConfidenceExactSlots, limit);
    appendDistinctResultsFromBucket(
        visibleResults,
        lowConfidenceExactMatches,
        globalSlots - reservedLowConfidenceExactSlots,
        limit);

    appendDistinctResultsFromBucket(visibleResults, contextPreferredPrefixes, limit, limit);
    appendDistinctResultsFromBucket(visibleResults, globalStrongPrefixes, limit, limit);
    appendDistinctResultsFromBucket(visibleResults, lowConfidenceExactMatches, limit, limit);
    appendDistinctResultsFromBucket(visibleResults, rest, limit, limit);
    return visibleResults;
  }

  private static List<LocationSearchResult> collectVisibleResults(
      List<LocationSearchResult> sortedResults, int limit) {
    final List<LocationSearchResult> visibleResults = new ArrayList<>();
    appendDistinctResultsFromBucket(visibleResults, sortedResults, limit, limit);
    return visibleResults;
  }

  private static void appendDistinctResultsFromBucket(
      List<LocationSearchResult> visibleResults,
      List<LocationSearchResult> candidates,
      int maxFromBucket,
      int limit) {
    if (maxFromBucket <= 0 || visibleResults.size() >= limit) {
      return;
    }

    int addedFromBucket = 0;
    for (LocationSearchResult candidate : candidates) {
      if (addedFromBucket == maxFromBucket || visibleResults.size() == limit) {
        return;
      }
      if (containsSameResultIdentity(visibleResults, candidate)) {
        continue;
      }
      if (isSemanticPlaceDuplicateOfAny(visibleResults, candidate)) {
        continue;
      }

      visibleResults.add(candidate);
      addedFromBucket++;
    }
  }

  private static boolean containsSameResultIdentity(
      List<LocationSearchResult> visibleResults, LocationSearchResult candidate) {
    for (LocationSearchResult visibleResult : visibleResults) {
      if (Objects.equals(visibleResult.type(), candidate.type())
          && Objects.equals(visibleResult.id(), candidate.id())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSemanticPlaceDuplicateOfAny(
      List<LocationSearchResult> visibleResults, LocationSearchResult candidate) {
    for (LocationSearchResult visibleResult : visibleResults) {
      if (isSameVisiblePlace(visibleResult, candidate)
          || isAdministrativePlaceDuplicate(visibleResult, candidate)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSameVisiblePlace(
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

    if (!"DE".equals(current.countryCode())) {
      return true;
    }

    return (current.admin1Code() != null
            && Objects.equals(current.admin1Code(), candidate.admin1Code()))
        || (hasAdministrativePath(current)
            && hasAdministrativePath(candidate)
            && hasSameAdministrativePath(current, candidate))
        || hasNearbyCoordinates(current, candidate);
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

  private static boolean hasNearbyCoordinates(
      LocationSearchResult current, LocationSearchResult candidate) {
    if (current.latitude() == null
        || current.longitude() == null
        || candidate.latitude() == null
        || candidate.longitude() == null) {
      return false;
    }

    return Math.abs(current.latitude() - candidate.latitude()) <= SAME_LOCATION_COORDINATE_DELTA
        && Math.abs(current.longitude() - candidate.longitude()) <= SAME_LOCATION_COORDINATE_DELTA;
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
      LocationSearchResult result, boolean includeGermanAdmin1Name, boolean directResolution) {
    return new LocationSearchResultResponse(
        result.type(),
        result.id(),
        displayLabel(result, includeGermanAdmin1Name),
        result.countryCode(),
        responseLatitude(result),
        responseLongitude(result),
        result.postalCode(),
        directResolution);
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

    if (result.admin1Code() == null) {
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

  // GeoNames admin1 codes are terse numeric strings for Germany. Expand them only when they are
  // needed to distinguish same-label visible results.
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
    return LocationSearchNormalizer.normalize(value);
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

  // Prefix search still relies on SQL LIKE. Escape wildcard characters so literal user input does
  // not silently widen the query.
  private static String escapeLikePattern(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static String prefixEnd(String prefix) {
    return prefix + Character.MAX_VALUE;
  }
}
