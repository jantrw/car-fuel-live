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

    final LocationSearchResponse response = locationSearchService.suggest("Bern", "DE", 8);

    assertThat(ids(response)).containsExactly("950001");
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
                population
            ) VALUES (:geonameId, :countryCode, :name, :name, :normalizedName, :normalizedAsciiName, 1.0, 2.0, :featureClass, 'PPL', :population)
            """)
        .param("geonameId", Long.parseLong(geonameId))
        .param("countryCode", countryCode)
        .param("name", name)
        .param("normalizedName", normalizedName)
        .param("normalizedAsciiName", normalizedAsciiName)
        .param("featureClass", featureClass)
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
