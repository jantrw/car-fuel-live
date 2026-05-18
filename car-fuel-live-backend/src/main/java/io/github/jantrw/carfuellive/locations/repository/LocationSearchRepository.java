package io.github.jantrw.carfuellive.locations.repository;

import io.github.jantrw.carfuellive.locations.model.CountryCapitalPlace;
import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LocationSearchRepository {

  private final JdbcClient jdbcClient;

  public LocationSearchRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  // Prefix search supports partial country input after exact lookup misses.
  public List<LocationSearchResult> searchCountries(
      String normalizedQuery, String likePrefix, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'country' AS type,
                country_code AS id,
                name AS label,
                country_code,
                latitude,
                longitude,
                NULL AS postal_code,
                NULL AS feature_class,
                NULL AS admin1_code,
                NULL AS admin2_code,
                NULL AS admin3_code,
                NULL AS admin4_code,
                CASE
                    WHEN normalized_name = :query THEN 0
                    ELSE 1
                END AS match_rank,
                COALESCE(population, 0) AS popularity
            FROM location_countries
            WHERE normalized_name = :query
                OR normalized_name LIKE :prefix ESCAPE '\\'
            ORDER BY match_rank, popularity DESC, name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("prefix", likePrefix)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Exact query stays separate so PostgreSQL can use the normalized-name index.
  public List<LocationSearchResult> searchCountriesExact(String normalizedQuery, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'country' AS type,
                country_code AS id,
                name AS label,
                country_code,
                latitude,
                longitude,
                NULL AS postal_code,
                NULL AS feature_class,
                NULL AS admin1_code,
                NULL AS admin2_code,
                NULL AS admin3_code,
                NULL AS admin4_code,
                0 AS match_rank,
                COALESCE(population, 0) AS popularity
            FROM location_countries
            WHERE normalized_name = :query
            ORDER BY popularity DESC, name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Prefix query supports partial city/place input such as "Ber"; use only after exact lookup
  // misses because it can scan the large GeoNames place table without pattern indexes.
  public List<LocationSearchResult> searchPlaces(
      String normalizedQuery, String likePrefix, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN (p.normalized_name = :query OR p.normalized_ascii_name = :query)
                        AND p.feature_class = 'P' THEN 0
                    WHEN p.normalized_name = :query OR p.normalized_ascii_name = :query THEN 1
                    WHEN p.feature_class = 'P' THEN 2
                    ELSE 3
                END AS match_rank,
                p.population AS popularity
            FROM location_places p
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE p.normalized_name = :query
                OR p.normalized_ascii_name = :query
                OR p.normalized_name LIKE :prefix ESCAPE '\\'
                OR p.normalized_ascii_name LIKE :prefix ESCAPE '\\'
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("prefix", likePrefix)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Country-scoped prefix lookup ensures the active country can contribute strong local
  // candidates before broader cross-country popularity pushes them out of the bounded result
  // window.
  public List<LocationSearchResult> searchPlacesInCountry(
      String normalizedQuery, String likePrefix, String countryCode, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN (p.normalized_name = :query OR p.normalized_ascii_name = :query)
                        AND p.feature_class = 'P' THEN 0
                    WHEN p.normalized_name = :query OR p.normalized_ascii_name = :query THEN 1
                    WHEN p.feature_class = 'P' THEN 2
                    ELSE 3
                END AS match_rank,
                p.population AS popularity
            FROM location_places p
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE p.country_code = :countryCode
                AND (
                    p.normalized_name = :query
                    OR p.normalized_ascii_name = :query
                    OR p.normalized_name LIKE :prefix ESCAPE '\\'
                    OR p.normalized_ascii_name LIKE :prefix ESCAPE '\\'
                )
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("prefix", likePrefix)
        .param("countryCode", countryCode)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Full-name lookups should stay index-friendly and prefer populated places over same-label
  // administrative rows.
  public List<LocationSearchResult> searchPlacesExact(String normalizedQuery, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN p.feature_class = 'P' THEN 0
                    ELSE 1
                END AS match_rank,
                p.population AS popularity
            FROM location_places p
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE p.normalized_name = :query
                OR p.normalized_ascii_name = :query
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Exact same-country lookup stays separate so local full-name matches are guaranteed to enter
  // the candidate set before broader global exact matches are merged in.
  public List<LocationSearchResult> searchPlacesExactInCountry(
      String normalizedQuery, String countryCode, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN p.feature_class = 'P' THEN 0
                    ELSE 1
                END AS match_rank,
                p.population AS popularity
            FROM location_places p
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE p.country_code = :countryCode
                AND (
                    p.normalized_name = :query
                    OR p.normalized_ascii_name = :query
                )
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("countryCode", countryCode)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Alias prefix search is the most expensive lookup path because aliases are the largest table.
  // Keep it as a fallback for partial alternate-name input.
  public List<LocationSearchResult> searchPlaceAliases(
      String normalizedQuery, String likePrefix, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN a.normalized_alias_name = :query AND p.feature_class = 'P' THEN 2
                    WHEN a.normalized_alias_name = :query THEN 3
                    WHEN p.feature_class = 'P' THEN 4
                    ELSE 5
                END AS match_rank,
                p.population AS popularity
            FROM location_place_aliases a
            JOIN location_places p ON p.geoname_id = a.place_geoname_id
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE a.normalized_alias_name = :query
                OR a.normalized_alias_name LIKE :prefix ESCAPE '\\'
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("prefix", likePrefix)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Alias prefix search also needs a same-country path so common local alternate names do not get
  // displaced by more popular foreign rows before Java ranking can apply the context preference.
  public List<LocationSearchResult> searchPlaceAliasesInCountry(
      String normalizedQuery, String likePrefix, String countryCode, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN a.normalized_alias_name = :query AND p.feature_class = 'P' THEN 2
                    WHEN a.normalized_alias_name = :query THEN 3
                    WHEN p.feature_class = 'P' THEN 4
                    ELSE 5
                END AS match_rank,
                p.population AS popularity
            FROM location_place_aliases a
            JOIN location_places p ON p.geoname_id = a.place_geoname_id
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE p.country_code = :countryCode
                AND (
                    a.normalized_alias_name = :query
                    OR a.normalized_alias_name LIKE :prefix ESCAPE '\\'
                )
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("prefix", likePrefix)
        .param("countryCode", countryCode)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Exact alias lookup supports alternate names without paying the prefix-scan cost first.
  public List<LocationSearchResult> searchPlaceAliasesExact(String normalizedQuery, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN p.feature_class = 'P' THEN 2
                    ELSE 3
                END AS match_rank,
                p.population AS popularity
            FROM location_place_aliases a
            JOIN location_places p ON p.geoname_id = a.place_geoname_id
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE a.normalized_alias_name = :query
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Exact alias matches should also respect the active country context at candidate-collection
  // time so local alternate names remain visible even with a bounded overfetch window.
  public List<LocationSearchResult> searchPlaceAliasesExactInCountry(
      String normalizedQuery, String countryCode, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'place' AS type,
                CAST(p.geoname_id AS VARCHAR) AS id,
                p.name || ', ' || c.name AS label,
                p.country_code,
                p.latitude,
                p.longitude,
                NULL AS postal_code,
                p.feature_class,
                p.admin1_code,
                p.admin2_code,
                p.admin3_code,
                p.admin4_code,
                CASE
                    WHEN p.feature_class = 'P' THEN 2
                    ELSE 3
                END AS match_rank,
                p.population AS popularity
            FROM location_place_aliases a
            JOIN location_places p ON p.geoname_id = a.place_geoname_id
            JOIN location_countries c ON c.country_code = p.country_code
            WHERE p.country_code = :countryCode
                AND a.normalized_alias_name = :query
            ORDER BY match_rank, popularity DESC, p.name
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("countryCode", countryCode)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Broad postal-code prefix search remains available for future partial postal-code search.
  // Full text place lookup should not use this path.
  public List<LocationSearchResult> searchGermanPostalCodes(
      String normalizedQuery, String likePrefix, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'postalCode' AS type,
                country_code || '-' || postal_code || '-' || place_name AS id,
                postal_code || ' ' || place_name || ', Germany' AS label,
                country_code,
                latitude,
                longitude,
                postal_code,
                NULL AS feature_class,
                NULL AS admin1_code,
                NULL AS admin2_code,
                NULL AS admin3_code,
                NULL AS admin4_code,
                CASE
                    WHEN postal_code = :query THEN 0
                    WHEN postal_code LIKE :prefix ESCAPE '\\' THEN 1
                    WHEN normalized_place_name = :query THEN 2
                    ELSE 3
                END AS match_rank,
                COALESCE(accuracy, 0) AS popularity
            FROM german_postal_codes
            WHERE postal_code = :query
                OR postal_code LIKE :prefix ESCAPE '\\'
                OR normalized_place_name = :query
                OR normalized_place_name LIKE :prefix ESCAPE '\\'
            ORDER BY match_rank, place_name, postal_code
            LIMIT :limit
            """)
        .param("query", normalizedQuery)
        .param("prefix", likePrefix)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Concrete PLZ lookup maps directly to stored coordinates; do not broaden it to place-name
  // search.
  public List<LocationSearchResult> searchGermanPostalCodesExact(String postalCode, int limit) {
    return jdbcClient
        .sql(
            """
            SELECT
                'postalCode' AS type,
                country_code || '-' || postal_code || '-' || place_name AS id,
                postal_code || ' ' || place_name || ', Germany' AS label,
                country_code,
                latitude,
                longitude,
                postal_code,
                NULL AS feature_class,
                NULL AS admin1_code,
                NULL AS admin2_code,
                NULL AS admin3_code,
                NULL AS admin4_code,
                0 AS match_rank,
                COALESCE(accuracy, 0) AS popularity
            FROM german_postal_codes
            WHERE postal_code = :query
            ORDER BY place_name, postal_code
            LIMIT :limit
            """)
        .param("query", postalCode)
        .param("limit", limit)
        .query(LocationSearchRepository::mapLocationSearchResult)
        .list();
  }

  // Future country-driven flows must not re-run a fuzzy capital-name search. Resolve the seeded
  // stable place reference with one indexed country lookup and one direct place join.
  public Optional<CountryCapitalPlace> findCapitalPlaceByCountryCode(String countryCode) {
    return jdbcClient
        .sql(
            """
            SELECT
                country.country_code,
                country.name AS country_name,
                place.geoname_id AS place_geoname_id,
                place.name AS place_name,
                place.latitude,
                place.longitude
            FROM location_countries country
            JOIN location_places place
                ON place.geoname_id = country.capital_place_geoname_id
            WHERE TRIM(CAST(country.country_code AS VARCHAR)) = :countryCode
            """)
        .param("countryCode", countryCode)
        .query(LocationSearchRepository::mapCountryCapitalPlace)
        .list()
        .stream()
        .findFirst();
  }

  private static LocationSearchResult mapLocationSearchResult(
      java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
    return new LocationSearchResult(
        resultSet.getString("type"),
        resultSet.getString("id"),
        resultSet.getString("label"),
        resultSet.getString("country_code"),
        resultSet.getObject("latitude", Double.class),
        resultSet.getObject("longitude", Double.class),
        resultSet.getString("postal_code"),
        resultSet.getString("feature_class"),
        resultSet.getString("admin1_code"),
        resultSet.getString("admin2_code"),
        resultSet.getString("admin3_code"),
        resultSet.getString("admin4_code"),
        resultSet.getInt("match_rank"),
        resultSet.getLong("popularity"));
  }

  private static CountryCapitalPlace mapCountryCapitalPlace(
      java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
    return new CountryCapitalPlace(
        resultSet.getString("country_code"),
        resultSet.getString("country_name"),
        resultSet.getLong("place_geoname_id"),
        resultSet.getString("place_name"),
        resultSet.getDouble("latitude"),
        resultSet.getDouble("longitude"));
  }
}
