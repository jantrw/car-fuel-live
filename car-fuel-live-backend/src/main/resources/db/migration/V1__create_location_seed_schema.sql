CREATE TABLE IF NOT EXISTS location_countries (
    country_code CHAR(2) PRIMARY KEY,
    geoname_id BIGINT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    iso3_code CHAR(3),
    numeric_code INTEGER,
    capital_name TEXT,
    continent_code CHAR(2) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    population BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS location_places (
    geoname_id BIGINT PRIMARY KEY,
    country_code CHAR(2) NOT NULL REFERENCES location_countries(country_code),
    name TEXT NOT NULL,
    ascii_name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    normalized_ascii_name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    feature_class CHAR(1) NOT NULL,
    feature_code TEXT NOT NULL,
    admin1_code TEXT,
    admin2_code TEXT,
    admin3_code TEXT,
    admin4_code TEXT,
    population BIGINT NOT NULL,
    timezone TEXT,
    source_modified_on DATE,
    alternate_names TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS location_place_aliases (
    place_geoname_id BIGINT NOT NULL REFERENCES location_places(geoname_id) ON DELETE CASCADE,
    alias_name TEXT NOT NULL,
    normalized_alias_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (place_geoname_id, alias_name)
);

CREATE TABLE IF NOT EXISTS german_postal_codes (
    country_code CHAR(2) NOT NULL DEFAULT 'DE',
    postal_code VARCHAR(20) NOT NULL,
    place_name TEXT NOT NULL,
    normalized_place_name TEXT NOT NULL,
    admin1_name TEXT,
    admin2_name TEXT,
    admin3_name TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy SMALLINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT german_postal_codes_country_code_check CHECK (country_code = 'DE'),
    CONSTRAINT german_postal_codes_primary_key PRIMARY KEY (postal_code, place_name)
);

CREATE INDEX IF NOT EXISTS idx_location_countries_normalized_name
    ON location_countries (normalized_name);

CREATE INDEX IF NOT EXISTS idx_location_places_country_code
    ON location_places (country_code);

CREATE INDEX IF NOT EXISTS idx_location_places_normalized_name
    ON location_places (normalized_name);

CREATE INDEX IF NOT EXISTS idx_location_places_normalized_ascii_name
    ON location_places (normalized_ascii_name);

CREATE INDEX IF NOT EXISTS idx_location_places_feature_code
    ON location_places (feature_code);

CREATE INDEX IF NOT EXISTS idx_location_place_aliases_normalized_alias_name
    ON location_place_aliases (normalized_alias_name);

CREATE INDEX IF NOT EXISTS idx_german_postal_codes_normalized_place_name
    ON german_postal_codes (normalized_place_name);

