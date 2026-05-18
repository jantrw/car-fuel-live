ALTER TABLE location_countries
    ADD COLUMN IF NOT EXISTS capital_place_geoname_id BIGINT;

ALTER TABLE location_countries
    ADD CONSTRAINT fk_location_countries_capital_place
        FOREIGN KEY (capital_place_geoname_id) REFERENCES location_places (geoname_id);

CREATE INDEX IF NOT EXISTS idx_location_countries_capital_place_geoname_id
    ON location_countries (capital_place_geoname_id);
