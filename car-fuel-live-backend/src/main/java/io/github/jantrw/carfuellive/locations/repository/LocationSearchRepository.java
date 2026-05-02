package io.github.jantrw.carfuellive.locations.repository;

import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LocationSearchRepository {

  private final JdbcClient jdbcClient;

  public LocationSearchRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<LocationSearchResult> searchCountries(
      String normalizedQuery, String likePrefix, int limit) {
    // Prefix search supports partial country input after exact lookup misses.
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

  public List<LocationSearchResult> searchCountriesExact(String normalizedQuery, int limit) {
    // Exact query stays separate so PostgreSQL can use the normalized-name index.
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

  public List<LocationSearchResult> searchPlaces(
      String normalizedQuery, String likePrefix, int limit) {
    // Prefix query supports partial city/place input such as "Ber"; use only after exact lookup
    // misses because it can scan the large GeoNames place table without pattern indexes.
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

  public List<LocationSearchResult> searchPlacesExact(String normalizedQuery, int limit) {
    // Full-name lookups should stay index-friendly and prefer populated places over same-label
    // administrative rows.
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

  public List<LocationSearchResult> searchPlaceAliases(
      String normalizedQuery, String likePrefix, int limit) {
    // Alias prefix search is the most expensive lookup path because aliases are the largest table.
    // Keep it as a fallback for partial alternate-name input.
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

  public List<LocationSearchResult> searchPlaceAliasesExact(String normalizedQuery, int limit) {
    // Exact alias lookup supports alternate names without paying the prefix-scan cost first.
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

  public List<LocationSearchResult> searchGermanPostalCodes(
      String normalizedQuery, String likePrefix, int limit) {
    // Broad postal-code prefix search remains available for future partial postal-code
    // autocomplete.
    // Full text place lookup should not use this path.
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

  public List<LocationSearchResult> searchGermanPostalCodesExact(String postalCode, int limit) {
    // Concrete PLZ lookup maps directly to stored coordinates; do not broaden it to place-name
    // search.
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
        resultSet.getInt("match_rank"),
        resultSet.getLong("popularity"));
  }
}
