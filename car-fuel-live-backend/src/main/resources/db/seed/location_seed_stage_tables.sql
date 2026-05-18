DROP TABLE IF EXISTS location_country_stage;
DROP TABLE IF EXISTS location_place_stage;
DROP TABLE IF EXISTS location_place_alias_stage;
DROP TABLE IF EXISTS german_postal_code_stage;

CREATE TEMP TABLE location_country_stage (
    country_code TEXT,
    iso3_code TEXT,
    numeric_code TEXT,
    name TEXT,
    normalized_name TEXT,
    capital_name TEXT,
    normalized_capital_name TEXT,
    population TEXT,
    continent_code TEXT,
    geoname_id TEXT
);

CREATE TEMP TABLE location_place_stage (
    geoname_id TEXT,
    country_code TEXT,
    name TEXT,
    ascii_name TEXT,
    normalized_name TEXT,
    normalized_ascii_name TEXT,
    latitude TEXT,
    longitude TEXT,
    feature_class TEXT,
    feature_code TEXT,
    admin1_code TEXT,
    admin2_code TEXT,
    admin3_code TEXT,
    admin4_code TEXT,
    population TEXT,
    timezone TEXT,
    source_modified_on TEXT,
    alternate_names TEXT
);

CREATE TEMP TABLE location_place_alias_stage (
    place_geoname_id TEXT,
    alias_name TEXT,
    normalized_alias_name TEXT
);

CREATE TEMP TABLE german_postal_code_stage (
    country_code TEXT,
    postal_code TEXT,
    place_name TEXT,
    normalized_place_name TEXT,
    admin1_name TEXT,
    admin2_name TEXT,
    admin3_name TEXT,
    latitude TEXT,
    longitude TEXT,
    accuracy TEXT
);

