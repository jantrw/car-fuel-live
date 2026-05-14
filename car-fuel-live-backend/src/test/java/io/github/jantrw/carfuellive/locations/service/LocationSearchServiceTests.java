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
    when(locationSearchRepository.searchPlacesExactInCountry("be", "DE", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("be", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("be", "be%", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesInCountry("be", "be%", "DE", 64))
        .thenReturn(List.of());
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

    final LocationSearchResponse response = locationSearchService.suggest("Be", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2950159", "3169070", "BE");
  }

  @Test
  void should_applyCountryBoost_when_sameRankPlacesExistAcrossCountries() {
    when(locationSearchRepository.searchPlacesExactInCountry("sa", "DE", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("sa", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("sa", "sa%", "DE", 64))
        .thenReturn(List.of(place("2", "Saarbrucken, Germany", "DE", 2, 10_000)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("sa", "sa%", "DE", 64))
        .thenReturn(List.of());
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
  void should_collectContextCountryCandidatesBeforeGlobalWindow_when_localPrefixMatchesExist() {
    when(locationSearchRepository.searchPlacesExactInCountry("be", "DE", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("be", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("be", "be%", "DE", 64))
        .thenReturn(List.of(place("2950159", "Berlin, Germany", "DE", 2, 3_426_354)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("be", "be%", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("be", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("be", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("be", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaces("be", "be%", 64))
        .thenReturn(
            List.of(
                place("3", "Bex, Switzerland", "CH", 2, 6_900),
                place("4", "Baix, France", "FR", 2, 6_100)));
    when(locationSearchRepository.searchPlaceAliases("be", "be%", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountries("be", "be%", 64))
        .thenReturn(List.of(country("BE", "Belgium", 1, 11_500_000)));

    final LocationSearchResponse response = locationSearchService.suggest("Be", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2950159", "3", "4", "BE");
  }

  @Test
  void should_preferStrongLocalPrefixOverZeroPopulationExactMatch_when_queryIsStillIncomplete() {
    when(locationSearchRepository.searchPlacesExactInCountry("berli", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("berli", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("berli", 64))
        .thenReturn(List.of(place("8", "Berli, Russia", "RU", 0, 0)));
    when(locationSearchRepository.searchPlaceAliasesExact("berli", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("berli", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("berli", "berli%", "DE", 64))
        .thenReturn(
            List.of(
                place("2950159", "Berlin, Germany", "DE", 2, 3_426_354),
                place("6547383", "Berlin Köpenick, Germany", "DE", 2, 59_561)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("berli", "berli%", "DE", 64))
        .thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Berli", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2950159", "6547383", "8");
  }

  @Test
  void
      should_preferStrongLocalPrefixOverZeroPopulationExactMatch_when_queryEqualsShortObscurePlace() {
    when(locationSearchRepository.searchPlacesExactInCountry("berl", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("berl", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("berl", 64))
        .thenReturn(List.of(place("9", "Berl, Germany", "DE", 0, 0)));
    when(locationSearchRepository.searchPlaceAliasesExact("berl", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("berl", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("berl", "berl%", "DE", 64))
        .thenReturn(List.of(place("2950159", "Berlin, Germany", "DE", 2, 3_426_354)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("berl", "berl%", "DE", 64))
        .thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Berl", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2950159", "9");
  }

  @Test
  void should_notLetCountryPreferenceOverrideClearExactPlaceIntent_when_queryIsLonger() {
    when(locationSearchRepository.searchPlacesExactInCountry("bern", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("bern", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("bern", 64))
        .thenReturn(List.of(place("5", "Bern, Switzerland", "CH", 0, 133_000)));
    when(locationSearchRepository.searchPlaceAliasesExact("bern", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("bern", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Bern", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("5");
  }

  @Test
  void should_useCountryPreferenceAsTieBreaker_when_exactPlacesShareTheSameName() {
    when(locationSearchRepository.searchPlacesExactInCountry("paris", "FR", 64))
        .thenReturn(List.of(place("6", "Paris, France", "FR", 0, 2_048_000)));
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("paris", "FR", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("paris", 64))
        .thenReturn(
            List.of(
                place("7", "Paris, Russia", "RU", 0, 12_000),
                place("6", "Paris, France", "FR", 0, 2_048_000)));
    when(locationSearchRepository.searchPlaceAliasesExact("paris", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("paris", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Paris", "FR", 8);

    assertThat(response.items()).extracting("id").containsExactly("6", "7");
  }

  @Test
  void should_omitCountryCoordinates_when_suggestionsReturnCountryItems() {
    when(locationSearchRepository.searchPlacesExactInCountry("belgium", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("belgium", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("belgium", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("belgium", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("belgium", 64))
        .thenReturn(List.of(country("BE", "Belgium", 4, 11_500_000)));

    final LocationSearchResponse response = locationSearchService.suggest("Belgium", "DE", 8);

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
