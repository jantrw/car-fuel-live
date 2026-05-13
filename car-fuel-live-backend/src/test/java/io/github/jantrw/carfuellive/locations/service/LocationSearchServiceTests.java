package io.github.jantrw.carfuellive.locations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.jantrw.carfuellive.locations.dto.LocationSearchResponse;
import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import io.github.jantrw.carfuellive.locations.repository.LocationSearchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationSearchServiceTests {

  @Mock private LocationSearchRepository locationSearchRepository;

  private LocationSearchService locationSearchService;

  @BeforeEach
  void setUp() {
    locationSearchService = new LocationSearchService(locationSearchRepository);
  }

  @Test
  void should_rankPlacesBeforeCountries_when_prefixSuggestionsAreRequested() {
    when(locationSearchRepository.searchPlacesExact("be", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("be", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("be", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaces("be", "be%", 64))
        .thenReturn(
            List.of(
                place("2950159", "Berlin, Germany", "DE", 2, 3_426_354),
                place("3169070", "Bernau bei Berlin, Germany", "DE", 2, 40_000)));
    when(locationSearchRepository.searchPlaceAliases("be", "be%", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountries("be", "be%", 64))
        .thenReturn(List.of(country("BE", "Belgium", 1, 11_500_000)));

    final LocationSearchResponse response = locationSearchService.suggest("Be", null, 8);

    assertThat(response.items()).extracting("id").containsExactly("2950159", "3169070", "BE");
  }

  @Test
  void should_applyCountryBoost_when_sameRankPlacesExistAcrossCountries() {
    when(locationSearchRepository.searchPlacesExact("sa", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("sa", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("sa", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaces("sa", "sa%", 64))
        .thenReturn(
            List.of(
                place("1", "Salzburg, Austria", "AT", 2, 10_000),
                place("2", "Saarbrucken, Germany", "DE", 2, 10_000)));
    when(locationSearchRepository.searchPlaceAliases("sa", "sa%", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountries("sa", "sa%", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Sa", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2", "1");
  }

  @Test
  void should_omitCountryCoordinates_when_suggestionsReturnCountryItems() {
    when(locationSearchRepository.searchPlacesExact("belgium", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("belgium", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("belgium", 64))
        .thenReturn(List.of(country("BE", "Belgium", 4, 11_500_000)));

    final LocationSearchResponse response = locationSearchService.suggest("Belgium", null, 8);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().latitude()).isNull();
    assertThat(response.items().getFirst().longitude()).isNull();
  }

  private static LocationSearchResult place(
      String id, String label, String countryCode, int matchRank, long popularity) {
    return new LocationSearchResult(
        "place",
        id,
        label,
        countryCode,
        1.0,
        2.0,
        null,
        "P",
        null,
        null,
        null,
        null,
        matchRank,
        popularity);
  }

  private static LocationSearchResult country(
      String id, String label, int matchRank, long popularity) {
    return new LocationSearchResult(
        "country",
        id,
        label,
        id,
        1.0,
        2.0,
        null,
        null,
        null,
        null,
        null,
        null,
        matchRank,
        popularity);
  }
}
