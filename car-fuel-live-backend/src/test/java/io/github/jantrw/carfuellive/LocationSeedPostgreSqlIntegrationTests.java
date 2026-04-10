package io.github.jantrw.carfuellive;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Testcontainers
class LocationSeedPostgreSqlIntegrationTests {

  @Container
  static final PostgreSQLContainer POSTGRE_SQL_CONTAINER =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
          .withDatabaseName("car-fuel-live-test")
          .withUsername("postgres")
          .withPassword("postgres");

  @Autowired private DataSource dataSource;

  @DynamicPropertySource
  static void configureDatabase(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRE_SQL_CONTAINER::getUsername);
    registry.add("spring.datasource.password", POSTGRE_SQL_CONTAINER::getPassword);
    registry.add("spring.flyway.url", POSTGRE_SQL_CONTAINER::getJdbcUrl);
    registry.add("spring.flyway.user", POSTGRE_SQL_CONTAINER::getUsername);
    registry.add("spring.flyway.password", POSTGRE_SQL_CONTAINER::getPassword);
  }

  @Test
  void should_loadSeedRowsIntoTargetTables_when_transformRunsOnPostgreSql() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      executeSqlScript(connection, "db/seed/location_seed_stage_tables.sql");

      insertCountryStageRows(connection);
      insertPlaceStageRows(connection);
      insertAliasStageRows(connection);
      insertGermanPostalCodeStageRows(connection);

      executeSqlScript(connection, "db/seed/location_seed_transform.sql");
    }

    try (Connection connection = dataSource.getConnection()) {
      assertThat(selectCount(connection, "SELECT COUNT(*) FROM location_countries")).isEqualTo(2);
      assertThat(selectCount(connection, "SELECT COUNT(*) FROM location_places")).isEqualTo(2);
      assertThat(selectCount(connection, "SELECT COUNT(*) FROM location_place_aliases"))
          .isEqualTo(2);
      assertThat(selectCount(connection, "SELECT COUNT(*) FROM german_postal_codes"))
          .isEqualTo(1);

      try (PreparedStatement statement =
          connection.prepareStatement(
              """
              SELECT name, latitude, longitude
              FROM location_countries
              WHERE country_code = ?
              """)) {
        statement.setString(1, "DE");
        try (var resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("name")).isEqualTo("Germany");
          assertThat(resultSet.getDouble("latitude")).isEqualTo(51.1657d);
          assertThat(resultSet.getDouble("longitude")).isEqualTo(10.4515d);
        }
      }

      try (PreparedStatement statement =
          connection.prepareStatement(
              """
              SELECT normalized_name, population
              FROM location_places
              WHERE geoname_id = ?
              """)) {
        statement.setLong(1, 2950159L);
        try (var resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("normalized_name")).isEqualTo("berlin");
          assertThat(resultSet.getLong("population")).isEqualTo(3426354L);
        }
      }

      try (PreparedStatement statement =
          connection.prepareStatement(
              """
              SELECT alias_name, normalized_alias_name
              FROM location_place_aliases
              WHERE place_geoname_id = ?
              ORDER BY alias_name
              """)) {
        statement.setLong(1, 2950159L);
        try (var resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("alias_name")).isEqualTo("Berlino");
          assertThat(resultSet.getString("normalized_alias_name")).isEqualTo("berlino");
          assertThat(resultSet.next()).isFalse();
        }
      }

      try (PreparedStatement statement =
          connection.prepareStatement(
              """
              SELECT alias_name, normalized_alias_name
              FROM location_place_aliases
              WHERE place_geoname_id = ?
              """)) {
        statement.setLong(1, 2988507L);
        try (var resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("alias_name")).isEqualTo("Parigi");
          assertThat(resultSet.getString("normalized_alias_name")).isEqualTo("parigi");
          assertThat(resultSet.next()).isFalse();
        }
      }

      try (PreparedStatement statement =
          connection.prepareStatement(
              """
              SELECT place_name, admin1_name, accuracy
              FROM german_postal_codes
              WHERE postal_code = ?
              """)) {
        statement.setString(1, "10115");
        try (var resultSet = statement.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("place_name")).isEqualTo("Berlin");
          assertThat(resultSet.getString("admin1_name")).isEqualTo("Berlin");
          assertThat(resultSet.getShort("accuracy")).isEqualTo((short) 6);
        }
      }
    }
  }

  private void insertCountryStageRows(Connection connection) throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO location_country_stage (
                country_code,
                iso3_code,
                numeric_code,
                name,
                normalized_name,
                capital_name,
                population,
                continent_code,
                geoname_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
      insertCountryStageRow(
          statement, "DE", "DEU", "276", "Germany", "germany", "Berlin", "84000000", "EU",
          "2921044");
      insertCountryStageRow(
          statement, "FR", "FRA", "250", "France", "france", "Paris", "68000000", "EU",
          "3017382");
    }
  }

  private void insertCountryStageRow(
      PreparedStatement statement,
      String countryCode,
      String iso3Code,
      String numericCode,
      String name,
      String normalizedName,
      String capitalName,
      String population,
      String continentCode,
      String geonameId)
      throws Exception {
    statement.setString(1, countryCode);
    statement.setString(2, iso3Code);
    statement.setString(3, numericCode);
    statement.setString(4, name);
    statement.setString(5, normalizedName);
    statement.setString(6, capitalName);
    statement.setString(7, population);
    statement.setString(8, continentCode);
    statement.setString(9, geonameId);
    statement.executeUpdate();
  }

  private void insertPlaceStageRows(Connection connection) throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO location_place_stage (
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
                population,
                timezone,
                source_modified_on,
                alternate_names
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
      insertPlaceStageRow(
          statement,
          "2921044",
          "DE",
          "Germany",
          "Germany",
          "germany",
          "germany",
          "51.1657",
          "10.4515",
          "A",
          "PCLI",
          "",
          "",
          "",
          "",
          "84000000",
          "Europe/Berlin",
          "2026-04-10",
          "Deutschland,Germany");
      insertPlaceStageRow(
          statement,
          "2950159",
          "DE",
          "Berlin",
          "Berlin",
          "berlin",
          "berlin",
          "52.52437",
          "13.41053",
          "P",
          "PPLC",
          "16",
          "11000",
          "",
          "",
          "3426354",
          "Europe/Berlin",
          "2026-04-10",
          "Berlin,Berlino");
      insertPlaceStageRow(
          statement,
          "2988507",
          "FR",
          "Paris",
          "Paris",
          "paris",
          "paris",
          "48.85341",
          "2.3488",
          "P",
          "PPLC",
          "11",
          "75C",
          "",
          "",
          "2138551",
          "Europe/Paris",
          "2026-04-10",
          "Paris,Parigi");
    }
  }

  private void insertPlaceStageRow(
      PreparedStatement statement,
      String geonameId,
      String countryCode,
      String name,
      String asciiName,
      String normalizedName,
      String normalizedAsciiName,
      String latitude,
      String longitude,
      String featureClass,
      String featureCode,
      String admin1Code,
      String admin2Code,
      String admin3Code,
      String admin4Code,
      String population,
      String timezone,
      String sourceModifiedOn,
      String alternateNames)
      throws Exception {
    statement.setString(1, geonameId);
    statement.setString(2, countryCode);
    statement.setString(3, name);
    statement.setString(4, asciiName);
    statement.setString(5, normalizedName);
    statement.setString(6, normalizedAsciiName);
    statement.setString(7, latitude);
    statement.setString(8, longitude);
    statement.setString(9, featureClass);
    statement.setString(10, featureCode);
    statement.setString(11, admin1Code);
    statement.setString(12, admin2Code);
    statement.setString(13, admin3Code);
    statement.setString(14, admin4Code);
    statement.setString(15, population);
    statement.setString(16, timezone);
    statement.setString(17, sourceModifiedOn);
    statement.setString(18, alternateNames);
    statement.executeUpdate();
  }

  private void insertAliasStageRows(Connection connection) throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO location_place_alias_stage (
                place_geoname_id,
                alias_name,
                normalized_alias_name
            ) VALUES (?, ?, ?)
            """)) {
      insertAliasStageRow(statement, "2950159", "Berlino", "berlino");
      insertAliasStageRow(statement, "2988507", "Parigi", "parigi");
      insertAliasStageRow(statement, "9999999", "Ghost Place", "ghost place");
    }
  }

  private void insertAliasStageRow(
      PreparedStatement statement, String placeGeonameId, String aliasName, String normalizedAlias)
      throws Exception {
    statement.setString(1, placeGeonameId);
    statement.setString(2, aliasName);
    statement.setString(3, normalizedAlias);
    statement.executeUpdate();
  }

  private void insertGermanPostalCodeStageRows(Connection connection) throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            INSERT INTO german_postal_code_stage (
                country_code,
                postal_code,
                place_name,
                normalized_place_name,
                admin1_name,
                admin2_name,
                admin3_name,
                latitude,
                longitude,
                accuracy
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
      statement.setString(1, "DE");
      statement.setString(2, "10115");
      statement.setString(3, "Berlin");
      statement.setString(4, "berlin");
      statement.setString(5, "Berlin");
      statement.setString(6, "");
      statement.setString(7, "");
      statement.setString(8, "52.532");
      statement.setString(9, "13.3849");
      statement.setString(10, "6");
      statement.executeUpdate();
    }
  }

  private long selectCount(Connection connection, String sql) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(sql);
        var resultSet = statement.executeQuery()) {
      assertThat(resultSet.next()).isTrue();
      return resultSet.getLong(1);
    }
  }

  private void executeSqlScript(Connection connection, String classpathLocation) {
    ResourceDatabasePopulator databasePopulator =
        new ResourceDatabasePopulator(new ClassPathResource(classpathLocation));
    databasePopulator.populate(connection);
  }
}
