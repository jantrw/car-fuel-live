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
  void should_returnValidationError_when_limitIsTooLarge() throws Exception {
    mockMvc
        .perform(get("/api/v1/locations/search").param("query", "Berlin").param("limit", "9"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
