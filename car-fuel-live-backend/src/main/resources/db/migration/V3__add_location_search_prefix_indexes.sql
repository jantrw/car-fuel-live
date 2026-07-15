CREATE INDEX IF NOT EXISTS idx_location_places_country_normalized_name_prefix
    ON location_places (country_code, normalized_name);

CREATE INDEX IF NOT EXISTS idx_location_places_country_normalized_ascii_name_prefix
    ON location_places (country_code, normalized_ascii_name);

CREATE INDEX IF NOT EXISTS idx_location_place_aliases_normalized_alias_name_prefix
    ON location_place_aliases (normalized_alias_name, place_geoname_id);
