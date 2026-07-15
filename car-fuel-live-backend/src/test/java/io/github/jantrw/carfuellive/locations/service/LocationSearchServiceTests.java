package io.github.jantrw.carfuellive.locations.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jantrw.carfuellive.locations.dto.LocationSearchResponse;
import io.github.jantrw.carfuellive.locations.dto.LocationSearchResultResponse;
import io.github.jantrw.carfuellive.locations.support.LocationSearchTestData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class LocationSearchServiceTests {

  @Autowired private LocationSearchService locationSearchService;

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void setUpLocationRows() {
    LocationSearchTestData.resetDefaultFixture(jdbcClient);
  }

  @Test
  void should_rankPlacesBeforeCountries_when_prefixSuggestionsAreRequested() {
    final LocationSearchResponse response = locationSearchService.suggest("Be", "DE", 8);

    assertThat(ids(response)).containsExactly("2950159", "3169070", "BE");
  }

  @Test
  void should_applyCountryBoost_when_sameRankPlacesExistAcrossCountries() {
    insertCountry("AT", "Austria", "austria", 9000000);
    insertPlace("910001", "DE", "Alpha", "alpha", "alpha", "P", 10000);
    insertPlace("910002", "AT", "Alpine", "alpine", "alpine", "P", 10000);

    final LocationSearchResponse response = locationSearchService.suggest("Al", "DE", 8);

    assertThat(ids(response)).startsWith("910001", "910002");
  }

  @Test
  void should_preferStrongLocalPrefixOverZeroPopulationExactMatch_when_queryIsStillIncomplete() {
    insertCountry("RU", "Russia", "russia", 140000000);
    insertPlace("920001", "RU", "Berli", "berli", "berli", "P", 0);

    final LocationSearchResponse response = locationSearchService.suggest("Berli", "DE", 8);

    assertThat(ids(response)).containsExactly("2950159", "920001");
  }

  @Test
  void should_notLetLowPopulationExactMatchesSuppressStrongerGlobalContinuation() {
    insertCountry("FR", "France", "france", 68000000);
    insertCountry("IT", "Italy", "italy", 59000000);
    insertCountry("EE", "Estonia", "estonia", 1300000);
    insertPlace("930001", "DE", "Parin", "parin", "parin", "P", 900);
    insertPlace("930002", "FR", "Paris", "paris", "paris", "P", 2138551);
    insertPlace("930003", "IT", "Pari", "pari", "pari", "P", 204);
    insertPlace("930004", "EE", "Pari", "pari", "pari", "P", 475);

    final LocationSearchResponse response = locationSearchService.suggest("Pari", "DE", 8);

    assertThat(ids(response)).containsSubsequence("930001", "930004", "930002");
  }

  @Test
  void should_treatPopularExactAliasAsClearIntent_when_queryUsesFoldedUmlautName() {
    final LocationSearchResponse response = locationSearchService.suggest("koln", "DE", 8);

    assertThat(ids(response)).containsExactly("2886242");
  }

  @Test
  void should_notTreatShortForeignExactAliasAsClearIntent_when_queryIsOnlyPrefixLength() {
    insertCountry("FR", "France", "france", 68000000);
    insertPlace("940001", "FR", "Coëx", "coex", "coex", "P", 3200);
    insertAlias("940001", "koe", "koe");

    final LocationSearchResponse response = locationSearchService.suggest("koe", "DE", 8);

    assertThat(ids(response)).containsSubsequence("2886242", "940001");
  }

  @Test
  void should_notLetCountryPreferenceOverrideClearExactPlaceIntent_when_queryIsLonger() {
    insertPlace("950001", "CH", "Bern", "bern", "bern", "P", 133000);
    insertPlace("950002", "DE", "Bernau", "bernau", "bernau", "P", 41000);

    final LocationSearchResponse response = locationSearchService.suggest("Bern", "DE", 8);

    assertThat(ids(response)).startsWith("950001");
  }

  @Test
  void should_useCountryPreferenceAsTieBreaker_when_exactPlacesShareTheSameName() {
    insertCountry("FR", "France", "france", 68000000);
    insertCountry("RU", "Russia", "russia", 140000000);
    insertPlace("960001", "FR", "Paris", "paris", "paris", "P", 2048000);
    insertPlace("960002", "RU", "Paris", "paris", "paris", "P", 12000);

    final LocationSearchResponse response = locationSearchService.suggest("Paris", "FR", 8);

    assertThat(ids(response)).containsExactly("960001", "960002");
  }

  @Test
  void should_preferFoldedLocalCityPrefix_when_queryOmitsUmlautExpansion() {
    insertPlace("970001", "DE", "D\u00fcsseldorf", "duesseldorf", "dusseldorf", "P", 620000);
    insertPlace("970002", "DE", "Dusslingen", "dusslingen", "dusslingen", "P", 6200);
    insertPlace("970003", "DE", "N\u00fcrnberg", "nuernberg", "nurnberg", "P", 500000);
    insertPlace("970004", "DE", "Nurn", "nurn", "nurn", "P", 1200);
    insertAlias("970001", "Dusseldorf", "dusseldorf");
    insertAlias("970003", "Nurnberg", "nurnberg");

    assertThat(ids(locationSearchService.suggest("Duss", "DE", 8))).startsWith("970001");
    assertThat(ids(locationSearchService.suggest("Nurn", "DE", 8))).startsWith("970003");
  }

  @Test
  void should_preferStrongLocalPrefixOverLowPopularityExactAlias() {
    insertPlace("971001", "DE", "Stuttgart", "stuttgart", "stuttgart", "P", 630000);
    insertPlace("971002", "DE", "Stuettgen", "stuettgen", "stuettgen", "P", 300);
    insertAlias("971002", "Stutt", "stutt");

    assertThat(ids(locationSearchService.suggest("Stutt", "DE", 8))).startsWith("971001");
  }

  @Test
  void should_preferTopGermanCity_when_shortExactAliasWouldBlockPrefixFallback() {
    insertCountry("NL", "The Netherlands", "the netherlands", 18000000);
    insertPlace("975001", "DE", "Dortmund", "dortmund", "dortmund", "P", 587000);
    insertPlace("975002", "NL", "Dordrecht", "dordrecht", "dordrecht", "P", 119000);
    insertAlias("975002", "Dort", "dort");

    assertThat(ids(locationSearchService.suggest("Dort", "DE", 8))).startsWith("975001");
  }

  @Test
  void should_preferTopGermanCity_when_queryHasOneCharacterPrefixTypo() {
    insertPlace("976001", "DE", "Munich", "munich", "munich", "P", 1260000);
    insertPlace("976002", "DE", "Munchberg", "munchberg", "munchberg", "P", 10000);

    assertThat(ids(locationSearchService.suggest("Munc", "DE", 8)).getFirst())
        .isIn("2867714", "976001");
    assertThat(ids(locationSearchService.suggest("Munch", "DE", 8)).getFirst())
        .isIn("2867714", "976001");
  }

  @Test
  void should_continueWithLocalPrefixFallback_when_longForeignExactMatchExists() {
    insertCountry("AT", "Austria", "austria", 9000000);
    insertPlace("977001", "AT", "Essling", "essling", "essling", "P", 10000);
    insertPlace("977002", "DE", "Esslingen", "esslingen", "esslingen", "P", 95000);

    assertThat(ids(locationSearchService.suggest("Essling", "DE", 8))).startsWith("977002");
  }

  @Test
  void should_keepPopularForeignExactIntent_when_localContextExists() {
    insertCountry("IT", "Italy", "italy", 59000000);
    insertPlace("978001", "IT", "Milan", "milan", "milan", "P", 1300000);
    insertPlace("978002", "DE", "Milanoweg", "milanoweg", "milanoweg", "P", 120);

    assertThat(ids(locationSearchService.suggest("Milan", "DE", 8))).startsWith("978001");
  }

  @Test
  void should_notShowLowPopularityAliasRowsForClearCityName() {
    insertCountry("UA", "Ukraine", "ukraine", 37000000);
    insertPlace("972001", "UA", "Khmelevoye", "khmelevoye", "khmelevoye", "P", 600);
    insertAlias("972001", "Berlin", "berlin");

    final LocationSearchResponse response = locationSearchService.suggest("Berlin", "DE", 8);

    assertThat(ids(response)).contains("2950159");
    assertThat(ids(response)).doesNotContain("972001");
  }

  @Test
  void should_keepPopularAlternateLanguageAlias() {
    final LocationSearchResponse response = locationSearchService.suggest("Praha", "DE", 8);

    assertThat(ids(response)).contains("3067696");
  }

  @Test
  void should_collapseDuplicateVisiblePlaceRows_when_rowsRepresentSameLocation() {
    insertCountry("IT", "Italy", "italy", 59000000);
    insertPlace("974001", "IT", "Milan", "milan", "milan", "P", 1300000, 45.4642, 9.19, "09");
    insertPlace("974002", "IT", "Milan", "milan", "milan", "P", 1200000, 45.4643, 9.1901, "09");
    insertPlace("974003", "IT", "Milan", "milan", "milan", "P", 900000, 45.4641, 9.1899, "09");

    final LocationSearchResponse response = locationSearchService.suggest("Milan", "DE", 8);

    assertThat(ids(response)).contains("974001");
    assertThat(ids(response)).doesNotContain("974002", "974003");
  }

  @Test
  void should_omitCountryCoordinates_when_suggestionsReturnCountryItems() {
    final LocationSearchResponse response = locationSearchService.suggest("Belgium", "DE", 8);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().latitude()).isNull();
    assertThat(response.items().getFirst().longitude()).isNull();
  }

  private static List<String> ids(LocationSearchResponse response) {
    return response.items().stream().map(LocationSearchResultResponse::id).toList();
  }

  private void insertCountry(
      String countryCode, String name, String normalizedName, long population) {
    jdbcClient
        .sql(
            """
            INSERT INTO location_countries (
                country_code,
                geoname_id,
                name,
                normalized_name,
                iso3_code,
                numeric_code,
                capital_name,
                continent_code,
                latitude,
                longitude,
                population
            ) VALUES (:countryCode, :geonameId, :name, :normalizedName, :iso3Code, :numericCode, :capitalName, 'EU', 1.0, 2.0, :population)
            """)
        .param("countryCode", countryCode)
        .param("geonameId", Math.abs(countryCode.hashCode()) + 1_000_000)
        .param("name", name)
        .param("normalizedName", normalizedName)
        .param("iso3Code", countryCode + "X")
        .param("numericCode", Math.abs(countryCode.hashCode()) % 1000)
        .param("capitalName", name + " City")
        .param("population", population)
        .update();
  }

  private void insertPlace(
      String geonameId,
      String countryCode,
      String name,
      String normalizedName,
      String normalizedAsciiName,
      String featureClass,
      long population) {
    insertPlace(
        geonameId,
        countryCode,
        name,
        normalizedName,
        normalizedAsciiName,
        featureClass,
        population,
        1.0,
        2.0,
        null);
  }

  private void insertPlace(
      String geonameId,
      String countryCode,
      String name,
      String normalizedName,
      String normalizedAsciiName,
      String featureClass,
      long population,
      double latitude,
      double longitude,
      String admin1Code) {
    jdbcClient
        .sql(
            """
            INSERT INTO location_places (
                geoname_id,
                country_code,
                name,
                ascii_name,
                normalized_name,
                normalized_ascii_name,
                latitude,
                longitude,
                feature_class,
                feature_code,
                admin1_code,
                population
            ) VALUES (:geonameId, :countryCode, :name, :name, :normalizedName, :normalizedAsciiName, :latitude, :longitude, :featureClass, 'PPL', :admin1Code, :population)
            """)
        .param("geonameId", Long.parseLong(geonameId))
        .param("countryCode", countryCode)
        .param("name", name)
        .param("normalizedName", normalizedName)
        .param("normalizedAsciiName", normalizedAsciiName)
        .param("latitude", latitude)
        .param("longitude", longitude)
        .param("featureClass", featureClass)
        .param("admin1Code", admin1Code)
        .param("population", population)
        .update();
  }

  private void insertAlias(String placeGeonameId, String aliasName, String normalizedAliasName) {
    jdbcClient
        .sql(
            """
            INSERT INTO location_place_aliases (
                place_geoname_id,
                alias_name,
                normalized_alias_name
            ) VALUES (:placeGeonameId, :aliasName, :normalizedAliasName)
            """)
        .param("placeGeonameId", Long.parseLong(placeGeonameId))
        .param("aliasName", aliasName)
        .param("normalizedAliasName", normalizedAliasName)
        .update();
  }
}
