package io.github.jantrw.carfuellive.locations.service;

import io.github.jantrw.carfuellive.locations.dto.LocationSearchResponse;
import io.github.jantrw.carfuellive.locations.dto.LocationSearchResultResponse;
import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import io.github.jantrw.carfuellive.locations.repository.LocationSearchRepository;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationSearchService {

  private final LocationSearchRepository locationSearchRepository;

  public LocationSearchService(LocationSearchRepository locationSearchRepository) {
    this.locationSearchRepository = locationSearchRepository;
  }

  @Transactional(readOnly = true)
  public LocationSearchResponse search(String query, int limit) {
    final String normalizedQuery = normalize(query);
    final String likePrefix = escapeLikePattern(normalizedQuery) + "%";
    final Map<String, LocationSearchResult> uniqueResults = new LinkedHashMap<>();

    final Optional<String> postalCodeQuery = firstDigitSequence(normalizedQuery);
    if (postalCodeQuery.isPresent()) {
      // Postal-code input is more specific than a same-name city. If a concrete PLZ exists,
      // return only postal-code coordinates instead of mixing place and postal-code results.
      final String postalCode = postalCodeQuery.get();
      final List<LocationSearchResult> postalCodeResults =
          locationSearchRepository.searchGermanPostalCodesExact(postalCode, limit);
      if (!postalCodeResults.isEmpty()) {
        return toSearchResponse(
            postalCodeResults.stream().sorted(resultComparator()).limit(limit).toList());
      }
    }

    // Full-name searches should hit B-tree indexes first. Prefix fallback exists for partial input,
    // but can scan large GeoNames tables until pattern indexes are added.
    addExactResults(uniqueResults, normalizedQuery, limit);
    if (uniqueResults.isEmpty()) {
      addPrefixResults(uniqueResults, normalizedQuery, likePrefix, limit);
    }

    return toSearchResponse(
        uniqueResults.values().stream().sorted(resultComparator()).limit(limit).toList());
  }

  private void addExactResults(
      Map<String, LocationSearchResult> uniqueResults, String normalizedQuery, int limit) {
    // Query families stay separate because each table has different ranking and dedupe rules.
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlacesExact(normalizedQuery, limit),
        LocationSearchService::dedupeKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliasesExact(normalizedQuery, limit),
        LocationSearchService::dedupeKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountriesExact(normalizedQuery, limit),
        LocationSearchService::idKey);
  }

  private void addPrefixResults(
      Map<String, LocationSearchResult> uniqueResults,
      String normalizedQuery,
      String likePrefix,
      int limit) {
    // Prefix fallback keeps short input useful while preserving fast exact lookups for full names.
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaces(normalizedQuery, likePrefix, limit),
        LocationSearchService::dedupeKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchPlaceAliases(normalizedQuery, likePrefix, limit),
        LocationSearchService::dedupeKey);
    addBestResults(
        uniqueResults,
        locationSearchRepository.searchCountries(normalizedQuery, likePrefix, limit),
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

  private static LocationSearchResult best(
      LocationSearchResult current, LocationSearchResult candidate) {
    // The same visible location can arrive through primary names and aliases. Keep the best ranked
    // candidate after semantic dedupe.
    return resultComparator().compare(candidate, current) < 0 ? candidate : current;
  }

  private static Comparator<LocationSearchResult> resultComparator() {
    // SQL assigns matchRank by match quality. Java applies shared tie-breakers so results from
    // place, alias, country, and postal-code queries are ranked consistently.
    return Comparator.comparingInt(LocationSearchResult::matchRank)
        .thenComparing(LocationSearchService::featureClassRank)
        .thenComparing(Comparator.comparingLong(LocationSearchResult::popularity).reversed())
        .thenComparing(LocationSearchResult::label)
        .thenComparing(LocationSearchResult::id);
  }

  private static LocationSearchResponse toSearchResponse(List<LocationSearchResult> results) {
    final List<LocationSearchResultResponse> items =
        results.stream().map(LocationSearchService::toResponse).toList();

    return new LocationSearchResponse(items);
  }

  private static String idKey(LocationSearchResult result) {
    return result.type() + ":" + result.id();
  }

  private static String dedupeKey(LocationSearchResult result) {
    // GeoNames can store the same visible place as both a populated place and an admin area.
    // Dedupe by displayed identity so users do not see duplicates.
    return result.type() + ":" + result.countryCode() + ":" + normalize(result.label());
  }

  private static int featureClassRank(LocationSearchResult result) {
    return "P".equals(result.featureClass()) ? 0 : 1;
  }

  private static Optional<String> firstDigitSequence(String value) {
    // German postal codes are numeric. The first digit run captures input such as
    // "Sankt Augustin 53757" without treating free text as a postal-code lookup.
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
    return new LocationSearchResultResponse(
        result.type(),
        result.id(),
        result.label(),
        result.countryCode(),
        result.latitude(),
        result.longitude(),
        result.postalCode());
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
