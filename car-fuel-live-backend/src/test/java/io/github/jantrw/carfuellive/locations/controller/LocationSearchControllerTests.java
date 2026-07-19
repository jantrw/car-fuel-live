package io.github.jantrw.carfuellive.locations.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jantrw.carfuellive.locations.support.LocationSearchTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LocationSearchControllerTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void setUpLocationRows() {
    LocationSearchTestData.resetDefaultFixture(jdbcClient);
  }

  @Test
  void should_returnTypedLocationResults_when_queryMatchesSeededRows() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Ber").param("limit", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2950159"))
        .andExpect(jsonPath("$.items[0].label").value("Berlin, Germany"))
        .andExpect(jsonPath("$.items[0].countryCode").value("DE"))
        .andExpect(jsonPath("$.items[0].latitude").value(52.52437))
        .andExpect(jsonPath("$.items[0].longitude").value(13.41053))
        .andExpect(jsonPath("$.items[0].directResolution").value(false));
  }

  @Test
  void should_returnCountryResult_when_queryMatchesCountry() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Belgium"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("country"))
        .andExpect(jsonPath("$.items[0].id").value("BE"))
        .andExpect(jsonPath("$.items[0].latitude").value(nullValue()))
        .andExpect(jsonPath("$.items[0].longitude").value(nullValue()))
        .andExpect(jsonPath("$.items[0].directResolution").value(false));
  }

  @Test
  void should_markSingleExactPlace_asDirectlyResolvable() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Berlin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].directResolution").value(true));
  }

  @Test
  void should_rankPlacesBeforeCountries_when_partialTextSuggestionsAreRequested() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Be").param("limit", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(3)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2950159"))
        .andExpect(jsonPath("$.items[1].type").value("place"))
        .andExpect(jsonPath("$.items[1].id").value("3169070"))
        .andExpect(jsonPath("$.items[2].type").value("country"))
        .andExpect(jsonPath("$.items[2].id").value("BE"));
  }

  @Test
  void should_prioritizeBoostedCountryPlaces_when_suggestionsReceiveCountryContext()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Be").param("countryCode", "de"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(3)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2950159"))
        .andExpect(jsonPath("$.items[1].type").value("place"))
        .andExpect(jsonPath("$.items[1].id").value("3169070"))
        .andExpect(jsonPath("$.items[2].type").value("country"))
        .andExpect(jsonPath("$.items[2].id").value("BE"));
  }

  @Test
  void should_treatBlankCountryCodeAsAbsent_when_suggestionsReceiveNoContext() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "Belgium").param("countryCode", " "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("country"))
        .andExpect(jsonPath("$.items[0].id").value("BE"));
  }

  @Test
  void should_returnPlaceResult_when_queryMatchesAlias() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Berlino"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].label").value("Berlin, Germany"))
        .andExpect(jsonPath("$.items[0].directResolution").value(true));
  }

  @Test
  void should_returnGermanPlace_when_queryUsesExpandedUmlautPrefix() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "koe").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2886242"))
        .andExpect(jsonPath("$.items[0].label").value("Köln, Germany"));
  }

  @Test
  void should_returnGermanPlace_when_queryUsesFoldedUmlautName() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "koln").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2886242"))
        .andExpect(jsonPath("$.items[0].label").value("Köln, Germany"));
  }

  @Test
  void should_returnGermanPlace_when_queryUsesFoldedUmlautPrefix() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "mue").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2867714"))
        .andExpect(jsonPath("$.items[0].label").value("München, Germany"));
  }

  @Test
  void should_returnSwissPlace_when_queryUsesFoldedUmlautName() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "zurich").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2657896"))
        .andExpect(jsonPath("$.items[0].label").value("Zürich, Switzerland"));
  }

  @Test
  void should_returnDanishPlace_when_queryUsesNordicAlias() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "aarhus").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2624652"))
        .andExpect(jsonPath("$.items[0].label").value("Århus, Denmark"));
  }

  @Test
  void should_returnDanishPlace_when_queryUsesNordicPrefix() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "aarh").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2624652"))
        .andExpect(jsonPath("$.items[0].label").value("Århus, Denmark"));
  }

  @Test
  void should_returnPolishPlace_when_queryUsesAsciiFallbackName() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "wroclaw").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("3081368"))
        .andExpect(jsonPath("$.items[0].label").value("Wrocław, Poland"));
  }

  @Test
  void should_returnPolishPlace_when_queryUsesNativeDiacriticName() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "wrocław").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("3081368"))
        .andExpect(jsonPath("$.items[0].label").value("Wrocław, Poland"));
  }

  @Test
  void should_returnSpanishPlace_when_queryUsesExactEuropeanName() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "sevilla").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("2510911"))
        .andExpect(jsonPath("$.items[0].label").value("Sevilla, Spain"));
  }

  @Test
  void should_returnCzechPlace_when_queryUsesAlternateLanguageAlias() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "praha").param("countryCode", "DE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value("3067696"))
        .andExpect(jsonPath("$.items[0].label").value("Prague, Czechia"));
  }

  @Test
  void should_returnPostalCodeResult_when_queryMatchesGermanPostalCode() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "10115"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("postalCode"))
        .andExpect(jsonPath("$.items[0].postalCode").value("10115"))
        .andExpect(jsonPath("$.items[0].label").value("10115 Berlin, Germany"))
        .andExpect(jsonPath("$.items[0].directResolution").value(true));
  }

  @Test
  void should_returnPostalCodeResult_when_queryMatchesGermanPostalCodePrefix() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "5375"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("postalCode"))
        .andExpect(jsonPath("$.items[0].postalCode").value("53757"))
        .andExpect(jsonPath("$.items[0].label").value("53757 Sankt Augustin, Germany"))
        .andExpect(jsonPath("$.items[0].directResolution").value(false));
  }

  @Test
  void should_returnPostalCodeResult_when_queryContainsStandaloneGermanPostalCode()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Street 1 53757"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("postalCode"))
        .andExpect(jsonPath("$.items[0].postalCode").value("53757"))
        .andExpect(jsonPath("$.items[0].label").value("53757 Sankt Augustin, Germany"));
  }

  @Test
  void should_notReturnPostalCodeResults_when_queryContainsOnlyArbitraryDigitFragment()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "A1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void should_returnSinglePlace_when_textQueryMatchesPlaceAndAdministrativeDuplicate()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Sankt Augustin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2841648"))
        .andExpect(jsonPath("$.items[0].label").value("Sankt Augustin, Germany"));
  }

  @Test
  void should_returnDistinctSameNamePlaces_when_queryMatchesMultiplePlacesInSameCountry()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Neustadt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2864067"))
        .andExpect(jsonPath("$.items[0].label").value("Neustadt, Bremen, Germany"))
        .andExpect(jsonPath("$.items[0].latitude").value(53.55196))
        .andExpect(jsonPath("$.items[0].directResolution").value(false))
        .andExpect(jsonPath("$.items[1].type").value("place"))
        .andExpect(jsonPath("$.items[1].id").value("8379207"))
        .andExpect(jsonPath("$.items[1].label").value("Neustadt, Niedersachsen, Germany"))
        .andExpect(jsonPath("$.items[1].latitude").value(52.26799))
        .andExpect(jsonPath("$.items[1].directResolution").value(false));
  }

  @Test
  void should_notMarkLimitedAmbiguousPlaces_asDirectlyResolvable() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Neustadt").param("limit", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].directResolution").value(false));
  }

  @Test
  void should_notMarkLimitedAmbiguousPostalCodes_asDirectlyResolvable() throws Exception {
    jdbcClient
        .sql(
            """
            INSERT INTO german_postal_codes (
                country_code, postal_code, place_name, normalized_place_name, admin1_name, latitude, longitude, accuracy
            ) VALUES ('DE', '10115', 'Berlin Mitte', 'berlin mitte', 'Berlin', 52.53, 13.39, 6)
            """)
        .update();

    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "10115").param("limit", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].directResolution").value(false));
  }

  @Test
  void should_returnOnlyPostalCodeResults_when_queryContainsMatchingPostalCode() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Sankt Augustin 53757"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("postalCode"))
        .andExpect(jsonPath("$.items[0].postalCode").value("53757"));
  }

  @Test
  void should_returnEmptyItems_when_queryHasNoMatch() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "DoesNotExist"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void should_returnValidationError_when_queryIsTooShort() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "b"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("query"))
        .andExpect(
            jsonPath("$.details[0].message").value("Query must be between 2 and 80 characters."));
  }

  @Test
  void should_returnValidationError_when_trimmedQueryIsTooShort() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", " b "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("query"))
        .andExpect(
            jsonPath("$.details[0].message").value("Query must be between 2 and 80 characters."));
  }

  @Test
  void should_returnValidationError_when_trimmedQueryIsTooLong() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", " " + "a".repeat(81) + " "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("query"))
        .andExpect(
            jsonPath("$.details[0].message").value("Query must be between 2 and 80 characters."));
  }

  @Test
  void should_returnValidationError_when_limitIsTooLarge() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Berlin").param("limit", "9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void should_returnValidationError_when_limitIsMalformed() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Berlin").param("limit", "foo"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("limit"))
        .andExpect(
            jsonPath("$.details[0].message")
                .value("Request parameter must use the expected type."));
  }

  @Test
  void should_returnValidationError_when_suggestionsCountryCodeIsInvalid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/locations/suggestions").param("q", "Berlin").param("countryCode", "DEU"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("countryCode"))
        .andExpect(
            jsonPath("$.details[0].message").value("Country code must be a two-letter ISO code."));
  }

  @Test
  void should_returnValidationError_when_suggestionsQueryUsesLegacyParameterName()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("query", "Berlin"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("q"))
        .andExpect(
            jsonPath("$.details[0].message").value("Required request parameter is missing."));
  }

  @Test
  void should_fillRequestedLimit_when_exactAliasRowsContainDuplicateSemanticMatches()
      throws Exception {
    LocationSearchTestData.resetAliasOverflowFixture(jdbcClient);

    mockMvc
        .perform(get("/api/v1/locations/suggestions").param("q", "Santa Maria").param("limit", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(8)))
        .andExpect(jsonPath("$.items[0].id").value("3167551"))
        .andExpect(
            jsonPath(
                "$.items[*].id",
                containsInAnyOrder(
                    "3167551", "3115177", "3108165", "3108000", "2511138", "2739118", "3116729",
                    "2976139")));
  }
}
