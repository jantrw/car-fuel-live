package io.github.jantrw.carfuellive;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LocationSeedSchemaMigrationTests {

  @Autowired private DataSource dataSource;

  @Test
  void should_createLocationSeedTables_when_contextLoads() throws Exception {
    final List<String> tableNames;
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement();
        var resultSet =
            statement.executeQuery(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'PUBLIC'
                """)) {
      final java.util.ArrayList<String> discoveredTableNames = new java.util.ArrayList<>();
      while (resultSet.next()) {
        discoveredTableNames.add(resultSet.getString("table_name"));
      }
      tableNames = discoveredTableNames;
    }

    assertThat(tableNames)
        .contains("LOCATION_COUNTRIES")
        .contains("LOCATION_PLACES")
        .contains("LOCATION_PLACE_ALIASES")
        .contains("GERMAN_POSTAL_CODES");
  }
}
