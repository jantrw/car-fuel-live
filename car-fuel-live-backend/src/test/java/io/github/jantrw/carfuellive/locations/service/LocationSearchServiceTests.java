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
                place("4", "Beaune, France", "FR", 2, 20_551)));
    when(locationSearchRepository.searchPlaceAliases("be", "be%", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountries("be", "be%", 64))
        .thenReturn(List.of(country("BE", "Belgium", 1, 11_500_000)));

    final LocationSearchResponse response = locationSearchService.suggest("Be", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2950159", "4", "3", "BE");
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
  void should_preferStrongLocalPrefixOverLowPopulationExactMatch_when_queryIsMei() {
    when(locationSearchRepository.searchPlacesExactInCountry("mei", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("mei", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("mei", 64))
        .thenReturn(
            List.of(
                place("10", "Mei, Portugal", "PT", 0, 0),
                administrativePlace("11", "Mei, Portugal", "PT", 1, 118)));
    when(locationSearchRepository.searchPlaceAliasesExact("mei", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("mei", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("mei", "mei%", "DE", 64))
        .thenReturn(
            List.of(
                place("2872347", "Meiderich, Germany", "DE", 2, 45_297),
                place("2867302", "Meissen, Germany", "DE", 2, 28_492),
                place("2873427", "Meinerzhagen, Germany", "DE", 2, 21_982),
                place("2873467", "Meiningen, Germany", "DE", 2, 21_580),
                place("2872126", "Meitingen, Germany", "DE", 2, 11_201)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("mei", "mei%", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaces("mei", "mei%", 64))
        .thenReturn(
            List.of(
                place("2867302", "Meissen, Germany", "DE", 2, 28_492),
                place("2873427", "Meinerzhagen, Germany", "DE", 2, 21_982),
                place("2873467", "Meiningen, Germany", "DE", 2, 21_580),
                place("10", "Mei, Portugal", "PT", 0, 0),
                place("15", "Meise, Belgium", "BE", 2, 18_497),
                place("16", "Meilen, Switzerland", "CH", 2, 14_207)));
    when(locationSearchRepository.searchPlaceAliases("mei", "mei%", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountries("mei", "mei%", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Mei", "DE", 8);

    assertThat(response.items())
        .extracting("id")
        .containsExactly("2872347", "2867302", "2873427", "2873467", "2872126", "10", "15", "16");
  }

  @Test
  void should_notLetLowPopulationExactMatchesSuppressStrongerGlobalContinuation_when_queryIsPari() {
    when(locationSearchRepository.searchPlacesExactInCountry("pari", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("pari", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("pari", 64))
        .thenReturn(
            List.of(
                place("12", "Pari, Italy", "IT", 0, 204),
                place("13", "Pari, Estonia", "EE", 0, 475)));
    when(locationSearchRepository.searchPlaceAliasesExact("pari", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountriesExact("pari", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("pari", "pari%", "DE", 64))
        .thenReturn(
            List.of(
                place("2855426", "Parin, Germany", "DE", 2, 900),
                place("2855422", "Paring, Germany", "DE", 2, 800),
                place("2855423", "Paring, Germany", "DE", 2, 700),
                place("2855421", "Parishof, Germany", "DE", 2, 600),
                place("18", "Parin Neu, Germany", "DE", 2, 500)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("pari", "pari%", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaces("pari", "pari%", 64))
        .thenReturn(
            List.of(
                place("2988507", "Paris, France", "FR", 2, 2_138_551),
                place("14", "Parikkala, Finland", "FI", 2, 5_000),
                place("12", "Pari, Italy", "IT", 0, 204),
                place("13", "Pari, Estonia", "EE", 0, 475)));
    when(locationSearchRepository.searchPlaceAliases("pari", "pari%", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchCountries("pari", "pari%", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("Pari", "DE", 8);

    assertThat(response.items())
        .extracting("id")
        .containsExactly("2855426", "2855422", "2855423", "2855421", "18", "13", "2988507", "14");
  }

  @Test
  void should_treatPopularExactAliasAsClearIntent_when_queryUsesFoldedUmlautName() {
    when(locationSearchRepository.searchPlacesExactInCountry("koln", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("koln", "DE", 64))
        .thenReturn(List.of(place("2886242", "Köln, Germany", "DE", 2, 1_080_000)));
    when(locationSearchRepository.searchPlacesExact("koln", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("koln", 64))
        .thenReturn(List.of(place("2886242", "Köln, Germany", "DE", 2, 1_080_000)));
    when(locationSearchRepository.searchCountriesExact("koln", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("koln", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2886242");
  }

  @Test
  void should_notTreatShortForeignExactAliasAsClearIntent_when_queryIsOnlyPrefixLength() {
    when(locationSearchRepository.searchPlacesExactInCountry("koe", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExactInCountry("koe", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlacesExact("koe", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlaceAliasesExact("koe", 64))
        .thenReturn(List.of(place("3024447", "Coëx, France", "FR", 2, 3_200)));
    when(locationSearchRepository.searchCountriesExact("koe", 64)).thenReturn(List.of());
    when(locationSearchRepository.searchPlacesInCountry("koe", "koe%", "DE", 64))
        .thenReturn(
            List.of(
                place("2886242", "Köln, Germany", "DE", 2, 1_080_000),
                place("2885656", "Köpenick, Germany", "DE", 2, 67_148)));
    when(locationSearchRepository.searchPlaceAliasesInCountry("koe", "koe%", "DE", 64))
        .thenReturn(List.of());
    when(locationSearchRepository.searchPlaces("koe", "koe%", 64))
        .thenReturn(List.of(place("2886242", "Köln, Germany", "DE", 2, 1_080_000)));
    when(locationSearchRepository.searchPlaceAliases("koe", "koe%", 64))
        .thenReturn(List.of(place("3024447", "Coëx, France", "FR", 2, 3_200)));
    when(locationSearchRepository.searchCountries("koe", "koe%", 64)).thenReturn(List.of());

    final LocationSearchResponse response = locationSearchService.suggest("koe", "DE", 8);

    assertThat(response.items()).extracting("id").containsExactly("2886242", "2885656", "3024447");
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

  private static LocationSearchResult administrativePlace(
      String id, String label, String countryCode, int matchRank, long popularity) {
    return new LocationSearchResult(
        "place",
        id,
        label,
        countryCode,
        1.0,
        2.0,
        null,
        "A",
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
