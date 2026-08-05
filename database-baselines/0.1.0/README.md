# Database Baseline 0.1.0

## Artifact

- File: `car-fuel-live-database-baseline-0.1.0-pg17.dump`
- Format: PostgreSQL custom archive
- Size: 78,038,972 bytes
- SHA-256: `1316cd0a74145069e51a1ddcfa2bbbbddd1d17e86eb797b861595241a6de10fb`
- Created: 2026-08-05 21:20:26 UTC
- PostgreSQL and `pg_dump`: 17.9
- Source commit: `d645b3594f81fa594e1420a3d804a211b018ca7e`
- Source database: `car-fuel-live` from Docker volume `car-fuel-live-backend_postgres_data`

The dump is stored locally at `backups/database-baseline-0.1.0/` until it is uploaded as a release asset.

## Reproducibility

Released installations reproduce this database by restoring the dump and verifying its SHA-256 checksum. `scripts/import-location-data.ps1` deliberately downloads the current rolling GeoNames files and must not be used to recreate baseline 0.1.0. A future data refresh requires a new reviewed baseline dump, version, and checksum.

## Data Source And Attribution

This baseline contains data derived from [GeoNames](https://www.geonames.org/), licensed under the [Creative Commons Attribution 4.0 International License](https://creativecommons.org/licenses/by/4.0/).

Source files:

- [`allCountries.zip`](https://download.geonames.org/export/dump/allCountries.zip)
- [`countryInfo.txt`](https://download.geonames.org/export/dump/countryInfo.txt)
- [`DE.zip`](https://download.geonames.org/export/zip/DE.zip)

The files were imported on 2026-05-14 between 15:42:53 and 15:45:31 UTC. The latest `source_modified_on` value retained from the GeoNames place data is 2026-05-13. The database baseline was created on 2026-08-05.

Car Fuel Live transforms the source data. It retains European countries, administrative and populated-place records, and German postal codes; excludes country rows from place results and the obsolete `CS` country data; creates normalized search fields and transliteration variants; deduplicates aliases; and maps the result into the application schema. The resulting database is therefore a filtered and modified derivative of the GeoNames datasets, not an unchanged GeoNames dump.

## Export

The source was exported without ownership or ACL commands:

```sh
pg_dump -Fc --no-owner --no-acl \
  --file=car-fuel-live-database-baseline-0.1.0-pg17.dump \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

The archive was restored once into an isolated PostgreSQL 17 instance with `--no-owner --no-acl` and exported again under the generic `postgres` owner. The final archive contains no ACL entries, ownership commands, role commands, or references to the local source owner.

## Verified State

| Table | Rows |
| --- | ---: |
| `flyway_schema_history` | 5 |
| `german_postal_codes` | 23,297 |
| `location_countries` | 53 |
| `location_place_aliases` | 1,965,379 |
| `location_places` | 1,089,946 |

- Flyway versions 1 through 5 are successful.
- No application sequences, views, materialized views, or foreign tables exist.
- Country code `CS`, GeoNames places `8505031` and `8505033`, and their aliases are absent.
- Berlin `2950159`, Paris `2988507`, Prague `3067696`, Belgrade `792680`, and Podgorica `3193044` exist as capitals (`PPLC`).
- German postal codes `01067`, `10115`, and `80331` return the expected places.
- All 53 `capital_place_geoname_id` values reference the unique `PPLC` row in the same country.
- A fresh PostgreSQL 17.9 restore reproduced every row count and Flyway checksum above. The backend validated all five migrations and returned Berlin, Prague, Belgrade, and postal code `10115`; `Tschechoslowakei` returned no results.

## Content Review

The archive contains only the Flyway history and the four location data tables. No authentication data, API keys, passwords, connection configuration, application-user data, roles, or ACLs are present. The GeoNames attribution and modification notice above must remain with every redistributed copy of the baseline.
