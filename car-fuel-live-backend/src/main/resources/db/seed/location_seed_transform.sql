TRUNCATE TABLE location_place_aliases, location_places, german_postal_codes, location_countries RESTART IDENTITY CASCADE;

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
)
SELECT
    stage.country_code::CHAR(2),
    stage.geoname_id::BIGINT,
    stage.name,
    stage.normalized_name,
    NULLIF(stage.iso3_code, '')::CHAR(3),
    NULLIF(stage.numeric_code, '')::INTEGER,
    NULLIF(stage.capital_name, ''),
    stage.continent_code::CHAR(2),
    place.latitude::DOUBLE PRECISION,
    place.longitude::DOUBLE PRECISION,
    NULLIF(stage.population, '')::BIGINT
FROM location_country_stage stage
JOIN location_place_stage place
    ON place.geoname_id = stage.geoname_id
   AND place.feature_code = 'PCLI'
WHERE stage.country_code <> 'CS';

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
    population,
    timezone,
    source_modified_on,
    alternate_names
)
SELECT
    stage.geoname_id::BIGINT,
    stage.country_code::CHAR(2),
    stage.name,
    stage.ascii_name,
    stage.normalized_name,
    stage.normalized_ascii_name,
    stage.latitude::DOUBLE PRECISION,
    stage.longitude::DOUBLE PRECISION,
    stage.feature_class::CHAR(1),
    stage.feature_code,
    NULLIF(stage.admin1_code, ''),
    NULLIF(stage.admin2_code, ''),
    NULLIF(stage.admin3_code, ''),
    NULLIF(stage.admin4_code, ''),
    COALESCE(NULLIF(stage.population, ''), '0')::BIGINT,
    NULLIF(stage.timezone, ''),
    NULLIF(stage.source_modified_on, '')::DATE,
    NULLIF(stage.alternate_names, '')
FROM location_place_stage stage
WHERE stage.feature_code <> 'PCLI'
  AND stage.country_code <> 'CS';

INSERT INTO location_place_aliases (
    place_geoname_id,
    alias_name,
    normalized_alias_name
)
SELECT DISTINCT
    stage.place_geoname_id::BIGINT,
    stage.alias_name,
    stage.normalized_alias_name
FROM location_place_alias_stage stage
JOIN location_places place
    ON place.geoname_id = stage.place_geoname_id::BIGINT
WHERE stage.alias_name <> ''
  AND stage.normalized_alias_name <> '';

WITH capital_candidates AS (
    SELECT
        country.country_code,
        place.geoname_id,
        CASE
            WHEN place.normalized_name = stage.normalized_capital_name THEN 0
            WHEN place.normalized_ascii_name = stage.normalized_capital_name THEN 1
            WHEN EXISTS (
                SELECT 1
                FROM location_place_aliases alias
                WHERE alias.place_geoname_id = place.geoname_id
                  AND alias.normalized_alias_name = stage.normalized_capital_name
            ) THEN 2
        END AS candidate_rank
    FROM location_countries country
    JOIN location_country_stage stage
        ON stage.country_code = country.country_code
    JOIN location_places place
        ON place.country_code = country.country_code
       AND place.feature_code = 'PPLC'
    WHERE stage.normalized_capital_name <> ''
      AND (
          place.normalized_name = stage.normalized_capital_name
          OR place.normalized_ascii_name = stage.normalized_capital_name
          OR EXISTS (
              SELECT 1
              FROM location_place_aliases alias
              WHERE alias.place_geoname_id = place.geoname_id
                AND alias.normalized_alias_name = stage.normalized_capital_name
          )
      )
),
best_capital_candidate_ranks AS (
    SELECT
        country_code,
        MIN(candidate_rank) AS best_rank
    FROM capital_candidates
    GROUP BY country_code
),
best_capital_candidates AS (
    SELECT
        candidate.country_code,
        candidate.geoname_id
    FROM capital_candidates candidate
    JOIN best_capital_candidate_ranks best_rank
        ON best_rank.country_code = candidate.country_code
       AND best_rank.best_rank = candidate.candidate_rank
    GROUP BY candidate.country_code, candidate.geoname_id
),
best_capital_candidate_counts AS (
    SELECT
        country_code,
        COUNT(*) AS candidate_count
    FROM best_capital_candidates
    GROUP BY country_code
)
UPDATE location_countries country
SET capital_place_geoname_id = candidate.geoname_id
FROM best_capital_candidates candidate
JOIN best_capital_candidate_counts candidate_count
    ON candidate_count.country_code = candidate.country_code
   AND candidate_count.candidate_count = 1
WHERE country.country_code = candidate.country_code;

DO $$
DECLARE unresolved_countries TEXT;
BEGIN
    SELECT string_agg(country.country_code || ' (' || country.name || ')', ', ' ORDER BY country.country_code)
    INTO unresolved_countries
    FROM location_countries country
    JOIN location_country_stage stage
        ON stage.country_code = country.country_code
    WHERE stage.capital_name <> ''
      AND country.capital_place_geoname_id IS NULL;

    IF unresolved_countries IS NOT NULL THEN
        RAISE EXCEPTION 'Capital place mapping unresolved for: %', unresolved_countries;
    END IF;
END $$;

INSERT INTO german_postal_codes (
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
)
SELECT
    'DE',
    stage.postal_code,
    stage.place_name,
    stage.normalized_place_name,
    NULLIF(stage.admin1_name, ''),
    NULLIF(stage.admin2_name, ''),
    NULLIF(stage.admin3_name, ''),
    stage.latitude::DOUBLE PRECISION,
    stage.longitude::DOUBLE PRECISION,
    NULLIF(stage.accuracy, '')::SMALLINT
FROM german_postal_code_stage stage
WHERE stage.country_code = 'DE';
