UPDATE location_countries
SET capital_place_geoname_id = (
    SELECT MIN(capital.geoname_id)
    FROM location_places capital
    WHERE capital.country_code = location_countries.country_code
      AND capital.feature_code = 'PPLC'
    HAVING COUNT(*) = 1
)
WHERE capital_place_geoname_id IS NULL;

CREATE TABLE country_capital_mapping_validation (
    valid SMALLINT NOT NULL CHECK (valid = 1)
);

INSERT INTO country_capital_mapping_validation (valid)
SELECT CASE WHEN EXISTS (
    SELECT 1
    FROM location_countries country
    LEFT JOIN location_places capital
        ON capital.geoname_id = country.capital_place_geoname_id
       AND capital.country_code = country.country_code
       AND capital.feature_code = 'PPLC'
    WHERE capital.geoname_id IS NULL
) THEN 0 ELSE 1 END;

DROP TABLE country_capital_mapping_validation;
