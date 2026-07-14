package io.github.jantrw.carfuellive.locations.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import io.github.jantrw.carfuellive.locations.support.LocationSearchTestData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@AutoConfigureMockMvc
class LocationSearchRepositoryTests {

  @Autowired private LocationSearchRepository locationSearchRepository;

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void setUpLocationRows() {
    LocationSearchTestData.resetDefaultFixture(jdbcClient);
  }

  @Test
  void should_returnCountryExactLookup_when_normalizedNameMatches() {
    final List<LocationSearchResult> results =
        locationSearchRepository.searchCountriesExact("belgium", 8);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().id()).isEqualTo("BE");
    assertThat(results.getFirst().type()).isEqualTo("country");
  }

  @Test
  void should_returnPlacePrefixLookups_when_queryMatchesShortText() {
    final List<LocationSearchResult> results =
        locationSearchRepository.searchPlaces("be", "be%", 8);

    assertThat(results).extracting(LocationSearchResult::id).containsExactly("2950159", "3169070");
    assertThat(results).extracting(LocationSearchResult::matchRank).containsOnly(2);
  }

  @Test
  void should_returnAliasExactLookup_when_normalizedAliasMatches() {
    final List<LocationSearchResult> results =
        locationSearchRepository.searchPlaceAliasesExact("berlino", 8);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().id()).isEqualTo("2950159");
    assertThat(results.getFirst().label()).isEqualTo("Berlin, Germany");
  }

  @Test
  void should_returnSyntheticFoldedAliasLookup_when_umlautVariantMatches() {
    final List<LocationSearchResult> results =
        locationSearchRepository.searchPlaceAliasesExact("koln", 8);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().id()).isEqualTo("2886242");
    assertThat(results.getFirst().label()).isEqualTo("Köln, Germany");
  }

  @Test
  void should_returnPostalCodeExactLookup_when_postalCodeMatches() {
    final List<LocationSearchResult> results =
        locationSearchRepository.searchGermanPostalCodesExact("10115", 8);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().postalCode()).isEqualTo("10115");
    assertThat(results.getFirst().label()).isEqualTo("10115 Berlin, Germany");
  }
}
