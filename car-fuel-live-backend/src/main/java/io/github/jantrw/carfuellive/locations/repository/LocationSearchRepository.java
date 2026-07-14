package io.github.jantrw.carfuellive.locations.repository;

import io.github.jantrw.carfuellive.locations.model.LocationSearchResult;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC-backed access layer for the seeded location search dataset.
 *
 * <p>Each query returns {@link LocationSearchResult} rows with enough metadata for the service to
 * merge exact, prefix, alias, country, and postal-code candidates into one ranked result list.
 */
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
    return searchPlaces(normalizedQuery, likePrefix, null, limit, false);
  }

  // Country-scoped prefix lookup ensures the active country can contribute strong local
  // candidates before broader cross-country popularity pushes them out of the bounded result
  // window.
  public List<LocationSearchResult> searchPlacesInCountry(
      String normalizedQuery, String likePrefix, String countryCode, int limit) {
    return searchPlaces(normalizedQuery, likePrefix, countryCode, limit, false);
  }

  // Full-name lookups should stay index-friendly and prefer populated places over same-label
  // administrative rows.
  public List<LocationSearchResult> searchPlacesExact(String normalizedQuery, int limit) {
    return searchPlaces(normalizedQuery, null, null, limit, true);
  }

  // Exact same-country lookup stays separate so local full-name matches are guaranteed to enter
  // the candidate set before broader global exact matches are merged in.
  public List<LocationSearchResult> searchPlacesExactInCountry(
      String normalizedQuery, String countryCode, int limit) {
    return searchPlaces(normalizedQuery, null, countryCode, limit, true);
  }

  private List<LocationSearchResult> searchPlaces(
      String normalizedQuery, String likePrefix, String countryCode, int limit, boolean exactOnly) {
    final String whereClause = placeWhereClause(countryCode != null, exactOnly);
    var query =
        jdbcClient
            .sql(placeSearchSql(whereClause, exactOnly))
            .param("query", normalizedQuery)
            .param("limit", limit);
    if (!exactOnly) {
      query = query.param("prefix", likePrefix);
    }
    if (countryCode != null) {
      query = query.param("countryCode", countryCode);
    }

    return query.query(LocationSearchRepository::mapLocationSearchResult).list();
  }

  private static String placeSearchSql(String whereClause, boolean exactOnly) {
    final String matchRank =
        exactOnly
            ? """
                CASE
                    WHEN p.feature_class = 'P' THEN 0
                    ELSE 1
                END AS match_rank,
              """
            : """
                CASE
                    WHEN (p.normalized_name = :query OR p.normalized_ascii_name = :query)
                        AND p.feature_class = 'P' THEN 0
                    WHEN p.normalized_name = :query OR p.normalized_ascii_name = :query THEN 1
                    WHEN p.feature_class = 'P' THEN 2
                    ELSE 3
                END AS match_rank,
              """;
    return """
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
        """
        + matchRank
        + """
            p.population AS popularity
        FROM location_places p
        JOIN location_countries c ON c.country_code = p.country_code
        WHERE
        """
        + whereClause
        + """
        ORDER BY match_rank, popularity DESC, p.name
        LIMIT :limit
        """;
  }

  private static String placeWhereClause(boolean countryScoped, boolean exactOnly) {
    final String matchClause =
        exactOnly
            ? """
                (
                    p.normalized_name = :query
                    OR p.normalized_ascii_name = :query
                )
              """
            : """
                (
                    p.normalized_name = :query
                    OR p.normalized_ascii_name = :query
                    OR p.normalized_name LIKE :prefix ESCAPE '\\'
                    OR p.normalized_ascii_name LIKE :prefix ESCAPE '\\'
                )
              """;
    return countryScoped ? "p.country_code = :countryCode AND " + matchClause : matchClause;
  }

  // Alias prefix search is the most expensive lookup path because aliases are the largest table.
  // Keep it as a fallback for partial alternate-name input.
  public List<LocationSearchResult> searchPlaceAliases(
      String normalizedQuery, String likePrefix, int limit) {
    return searchPlaceAliases(normalizedQuery, likePrefix, null, limit, false);
  }

  // Alias prefix search also needs a same-country path so common local alternate names do not get
  // displaced by more popular foreign rows before Java ranking can apply the context preference.
  public List<LocationSearchResult> searchPlaceAliasesInCountry(
      String normalizedQuery, String likePrefix, String countryCode, int limit) {
    return searchPlaceAliases(normalizedQuery, likePrefix, countryCode, limit, false);
  }

  // Exact alias lookup supports alternate names without paying the prefix-scan cost first.
  public List<LocationSearchResult> searchPlaceAliasesExact(String normalizedQuery, int limit) {
    return searchPlaceAliases(normalizedQuery, null, null, limit, true);
  }

  // Exact alias matches should also respect the active country context at candidate-collection
  // time so local alternate names remain visible even with a bounded overfetch window.
  public List<LocationSearchResult> searchPlaceAliasesExactInCountry(
      String normalizedQuery, String countryCode, int limit) {
    return searchPlaceAliases(normalizedQuery, null, countryCode, limit, true);
  }

  private List<LocationSearchResult> searchPlaceAliases(
      String normalizedQuery, String likePrefix, String countryCode, int limit, boolean exactOnly) {
    final String whereClause = aliasWhereClause(countryCode != null, exactOnly);
    var query =
        jdbcClient
            .sql(aliasSearchSql(whereClause, exactOnly))
            .param("query", normalizedQuery)
            .param("limit", limit);
    if (!exactOnly) {
      query = query.param("prefix", likePrefix);
    }
    if (countryCode != null) {
      query = query.param("countryCode", countryCode);
    }

    return query.query(LocationSearchRepository::mapLocationSearchResult).list();
  }

  private static String aliasSearchSql(String whereClause, boolean exactOnly) {
    final String matchRank =
        exactOnly
            ? """
                CASE
                    WHEN p.feature_class = 'P' THEN 2
                    ELSE 3
                END AS match_rank,
              """
            : """
                CASE
                    WHEN a.normalized_alias_name = :query AND p.feature_class = 'P' THEN 2
                    WHEN a.normalized_alias_name = :query THEN 3
                    WHEN p.feature_class = 'P' THEN 4
                    ELSE 5
                END AS match_rank,
              """;
    return """
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
        """
        + matchRank
        + """
            p.population AS popularity
        FROM location_place_aliases a
        JOIN location_places p ON p.geoname_id = a.place_geoname_id
        JOIN location_countries c ON c.country_code = p.country_code
        WHERE
        """
        + whereClause
        + """
        ORDER BY match_rank, popularity DESC, p.name
        LIMIT :limit
        """;
  }

  private static String aliasWhereClause(boolean countryScoped, boolean exactOnly) {
    final String matchClause =
        exactOnly
            ? """
                a.normalized_alias_name = :query
              """
            : """
                (
                    a.normalized_alias_name = :query
                    OR a.normalized_alias_name LIKE :prefix ESCAPE '\\'
                )
              """;
    return countryScoped ? "p.country_code = :countryCode AND " + matchClause : matchClause;
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
}
