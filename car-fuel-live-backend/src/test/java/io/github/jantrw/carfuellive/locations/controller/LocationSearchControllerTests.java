package io.github.jantrw.carfuellive.locations.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    jdbcClient.sql("DELETE FROM location_place_aliases").update();
    jdbcClient.sql("DELETE FROM german_postal_codes").update();
    jdbcClient.sql("DELETE FROM location_places").update();
    jdbcClient.sql("DELETE FROM location_countries").update();

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
            ) VALUES
                ('DE', 2921044, 'Germany', 'germany', 'DEU', 276, 'Berlin', 'EU', 51.1657, 10.4515, 84000000),
                ('BE', 2802361, 'Belgium', 'belgium', 'BEL', 56, 'Brussels', 'EU', 50.5039, 4.4699, 11500000)
            """)
        .update();

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
                admin2_code,
                admin3_code,
                admin4_code,
                population
            ) VALUES
                (2950159, 'DE', 'Berlin', 'Berlin', 'berlin', 'berlin', 52.52437, 13.41053, 'P', 'PPLC', '16', '11000', NULL, NULL, 3426354),
                (3169070, 'DE', 'Bernau bei Berlin', 'Bernau bei Berlin', 'bernau bei berlin', 'bernau bei berlin', 52.67982, 13.58708, 'P', 'PPL', '11', '12060', NULL, NULL, 40000),
                (2841648, 'DE', 'Sankt Augustin', 'Sankt Augustin', 'sankt augustin', 'sankt augustin', 50.77538, 7.197, 'P', 'PPLA4', '05', '05382', '05382056', '053820056056', 56094),
                (6557568, 'DE', 'Sankt Augustin', 'Sankt Augustin', 'sankt augustin', 'sankt augustin', 50.77935, 7.18682, 'A', 'ADM4', '05', '05382', '05382056', '053820056056', 56521),
                (2864067, 'DE', 'Neustadt', 'Neustadt', 'neustadt', 'neustadt', 53.55196, 9.98558, 'P', 'PPL', '03', '03359', '03359038', NULL, 12689),
                (8379207, 'DE', 'Neustadt', 'Neustadt', 'neustadt', 'neustadt', 52.26799, 10.52001, 'P', 'PPL', '06', '03158', '03158037', NULL, 2386)
            """)
        .update();

    jdbcClient
        .sql(
            """
            INSERT INTO location_place_aliases (
                place_geoname_id,
                alias_name,
                normalized_alias_name
            ) VALUES
                (2950159, 'Berlino', 'berlino')
            """)
        .update();

    jdbcClient
        .sql(
            """
            INSERT INTO german_postal_codes (
                country_code,
                postal_code,
                place_name,
                normalized_place_name,
                admin1_name,
                latitude,
                longitude,
                accuracy
            ) VALUES
                ('DE', '10115', 'Berlin', 'berlin', 'Berlin', 52.532, 13.3849, 6),
                ('DE', '53757', 'Sankt Augustin', 'sankt augustin', 'Nordrhein-Westfalen', 50.7754, 7.197, 4)
            """)
        .update();
  }

  @Test
  void should_returnTypedLocationResults_when_queryMatchesSeededRows() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Ber").param("limit", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2950159"))
        .andExpect(jsonPath("$.items[0].label").value("Berlin, Germany"))
        .andExpect(jsonPath("$.items[0].countryCode").value("DE"))
        .andExpect(jsonPath("$.items[0].latitude").value(52.52437))
        .andExpect(jsonPath("$.items[0].longitude").value(13.41053));
  }

  @Test
  void should_returnCountryResult_when_queryMatchesCountry() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Belgium"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("country"))
        .andExpect(jsonPath("$.items[0].id").value("BE"))
        .andExpect(jsonPath("$.items[0].latitude").value(50.5039));
  }

  @Test
  void should_returnPlaceResult_when_queryMatchesAlias() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Berlino"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].label").value("Berlin, Germany"));
  }

  @Test
  void should_returnPostalCodeResult_when_queryMatchesGermanPostalCode() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "10115"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("postalCode"))
        .andExpect(jsonPath("$.items[0].postalCode").value("10115"))
        .andExpect(jsonPath("$.items[0].label").value("10115 Berlin, Germany"));
  }

  @Test
  void should_returnSinglePlace_when_textQueryMatchesPlaceAndAdministrativeDuplicate()
      throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Sankt Augustin"))
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
        .perform(get("/api/v1/locations/search").param("query", "Neustadt"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(2)))
        .andExpect(jsonPath("$.items[0].type").value("place"))
        .andExpect(jsonPath("$.items[0].id").value("2864067"))
        .andExpect(jsonPath("$.items[0].label").value("Neustadt, Bremen, Germany"))
        .andExpect(jsonPath("$.items[0].latitude").value(53.55196))
        .andExpect(jsonPath("$.items[1].type").value("place"))
        .andExpect(jsonPath("$.items[1].id").value("8379207"))
        .andExpect(jsonPath("$.items[1].label").value("Neustadt, Niedersachsen, Germany"))
        .andExpect(jsonPath("$.items[1].latitude").value(52.26799));
  }

  @Test
  void should_returnOnlyPostalCodeResults_when_queryContainsMatchingPostalCode() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Sankt Augustin 53757"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].type").value("postalCode"))
        .andExpect(jsonPath("$.items[0].postalCode").value("53757"));
  }

  @Test
  void should_returnEmptyItems_when_queryHasNoMatch() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "DoesNotExist"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void should_returnValidationError_when_queryIsTooShort() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "b"))
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
        .perform(get("/api/v1/locations/search").param("query", "Berlin").param("limit", "9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void should_returnValidationError_when_limitIsMalformed() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Berlin").param("limit", "foo"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(1)))
        .andExpect(jsonPath("$.details[0].field").value("limit"))
        .andExpect(
            jsonPath("$.details[0].message")
                .value("Request parameter must use the expected type."));
  }

  @Test
  void should_fillRequestedLimit_when_exactAliasRowsContainDuplicateSemanticMatches()
      throws Exception {
    jdbcClient.sql("DELETE FROM location_place_aliases").update();
    jdbcClient.sql("DELETE FROM location_places").update();
    jdbcClient.sql("DELETE FROM location_countries").update();

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
            ) VALUES
                ('IT', 3175395, 'Italy', 'italy', 'ITA', 380, 'Rome', 'EU', 41.8719, 12.5674, 59000000),
                ('FR', 3017382, 'France', 'france', 'FRA', 250, 'Paris', 'EU', 46.2276, 2.2137, 68000000),
                ('ES', 2510769, 'Spain', 'spain', 'ESP', 724, 'Madrid', 'EU', 40.4637, -3.7492, 47000000),
                ('PT', 2264397, 'Portugal', 'portugal', 'PRT', 620, 'Lisbon', 'EU', 39.3999, -8.2245, 10300000)
            """)
        .update();

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
                admin2_code,
                admin3_code,
                admin4_code,
                population
            ) VALUES
                (3167551, 'IT', 'Santa Maria', 'Santa Maria', 'santa maria', 'santa maria', 39.5, 16.0, 'P', 'PPL', NULL, NULL, NULL, NULL, 3784),
                (2976139, 'FR', 'Village de Santa-Maria', 'Village de Santa-Maria', 'village de santa maria', 'village de santa maria', 42.7, 9.3, 'P', 'PPL', NULL, NULL, NULL, NULL, 75),
                (3115177, 'ES', 'Oleiros', 'Oleiros', 'oleiros', 'oleiros', 43.3, -8.3, 'P', 'PPL', NULL, NULL, NULL, NULL, 35559),
                (3108165, 'ES', 'Teo', 'Teo', 'teo', 'teo', 42.8, -8.5, 'P', 'PPL', NULL, NULL, NULL, NULL, 17807),
                (3108000, 'ES', 'Tomiño', 'Tomino', 'tomino', 'tomino', 41.98, -8.75, 'P', 'PPL', NULL, NULL, NULL, NULL, 13315),
                (2511138, 'ES', 'Santa Maria del Camí', 'Santa Maria del Cami', 'santa maria del cami', 'santa maria del cami', 39.65, 2.78, 'P', 'PPL', NULL, NULL, NULL, NULL, 6007),
                (2739118, 'PT', 'Galegos', 'Galegos', 'galegos', 'galegos', 41.45, -8.61, 'P', 'PPL', NULL, NULL, NULL, NULL, 5404),
                (3116729, 'ES', 'Miño', 'Mino', 'mino', 'mino', 43.35, -8.2, 'P', 'PPL', NULL, NULL, NULL, NULL, 5092)
            """)
        .update();

    jdbcClient
        .sql(
            """
            INSERT INTO location_place_aliases (
                place_geoname_id,
                alias_name,
                normalized_alias_name
            ) VALUES
                (3115177, 'Santa Maria de Oleiros', 'santa maria'),
                (3115177, 'Santa María de Oleiros', 'santa maria'),
                (3108165, 'Santa Maria de Teo', 'santa maria'),
                (3108165, 'Santa María de Teo', 'santa maria'),
                (3108000, 'Santa Maria de Tomiño', 'santa maria'),
                (2511138, 'Santa Maria del Camí', 'santa maria'),
                (2739118, 'Santa Maria de Galegos', 'santa maria'),
                (3116729, 'Santa Maria de Miño', 'santa maria'),
                (2976139, 'Santa Maria', 'santa maria')
            """)
        .update();

    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Santa Maria").param("limit", "8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(8)))
        .andExpect(jsonPath("$.items[0].id").value("3167551"))
        .andExpect(jsonPath("$.items[1].id").value("3115177"))
        .andExpect(jsonPath("$.items[2].id").value("3108165"))
        .andExpect(jsonPath("$.items[3].id").value("3108000"))
        .andExpect(jsonPath("$.items[4].id").value("2511138"))
        .andExpect(jsonPath("$.items[5].id").value("2739118"))
        .andExpect(jsonPath("$.items[6].id").value("3116729"))
        .andExpect(jsonPath("$.items[7].id").value("2976139"));
  }
}
