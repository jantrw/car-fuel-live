# Database Baseline 0.1.0

## Artifact

- File: `car-fuel-live-database-baseline-0.1.0-pg17.dump`
- Format: PostgreSQL custom archive
- Size: 78,038,699 bytes
- SHA-256: `2f74b8d4f728fc3209cf7b1eccb0ff81fa3d9c8737b5dbceef65f4f8a9c9d06b`
- Created: 2026-08-05 20:19:52 UTC
- PostgreSQL and `pg_dump`: 17.9
- Source commit: `80299d03f238e26e03063b64d8d85e1ca7697dc2`
- Source database: `car-fuel-live` from Docker volume `car-fuel-live-backend_postgres_data`

The dump is stored locally at `backups/database-baseline-0.1.0/` until the third-party data review in issue #71 permits publication.

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
| `flyway_schema_history` | 4 |
| `german_postal_codes` | 23,297 |
| `location_countries` | 53 |
| `location_place_aliases` | 1,965,379 |
| `location_places` | 1,089,946 |

- Flyway versions 1 through 4 are successful.
- No application sequences, views, materialized views, or foreign tables exist.
- Country code `CS`, GeoNames places `8505031` and `8505033`, and their aliases are absent.
- Berlin `2950159`, Paris `2988507`, Prague `3067696`, Belgrade `792680`, and Podgorica `3193044` exist as capitals (`PPLC`).
- German postal codes `01067`, `10115`, and `80331` return the expected places.
- All 53 `capital_place_geoname_id` values are `NULL`. This existing source state is preserved exactly; the application does not currently read this column.

## Content Review

The archive contains only the Flyway history and the four location data tables. No authentication data, API keys, passwords, connection configuration, application-user data, roles, or ACLs are present. Publication remains blocked until issue #71 confirms that the GeoNames-derived country, place, alias, and postal-code data may be redistributed with the required attribution.
