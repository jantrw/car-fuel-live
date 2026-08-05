UPDATE location_countries
SET capital_place_geoname_id = NULL
WHERE capital_place_geoname_id IN (
    SELECT geoname_id
    FROM location_places
    WHERE country_code = 'CS'
);

DELETE FROM location_places
WHERE country_code = 'CS';

DELETE FROM location_countries
WHERE country_code = 'CS';
