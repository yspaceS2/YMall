# Flyway migration history integrity

## Incident summary

The local PostgreSQL database contained successful Flyway records for V20, V24,
V25, and V27, but the repository contained different files for three of those
versions and no V25 file. Flyway correctly rejected the application startup
before running later migrations.

The mismatch was caused by feature branches independently assigning migration
versions that had already been applied from commit `b97fa57`. Later merges added
new V20, V24, and V27 scripts while the applied V25 script was not carried into
`develop`.

## Canonical applied migrations

The following files are immutable because their checksums are already recorded
in an existing database:

| Version | File | Flyway checksum |
| --- | --- | ---: |
| V20 | `V20__add_order_delivery_operations.sql` | `-85917804` |
| V24 | `V24__create_product_return_requests.sql` | `-2073083079` |
| V25 | `V25__expand_refund_status_constraints.sql` | `-1795236840` |
| V27 | `V27__expand_notification_type_constraint.sql` | `-2076353839` |

`FlywayMigrationHistoryIntegrityTest` locks these values so a future edit fails
the backend test suite before it reaches an existing environment.

## Forward-only corrections

Changes that were previously placed in an applied version were moved to new
versioned migrations:

- V30 adds order-item fulfillment fields, widens `tracking_number`, and copies
  legacy `shipping_carrier` values into `carrier`.
- V31 adds the payment-refund foreign key and return-request status constraint.
- V32 aligns the notification type constraint with the current enum.

Do not use `flyway repair` to make a modified migration look valid. Restore the
exact applied file and express every new schema change in a higher version.

## New environment baseline

`B32__current_schema.sql` is a Flyway baseline migration containing the complete
PostgreSQL schema after V32. Flyway uses it only for an empty environment. An
environment with existing versioned history ignores B32 and continues from its
latest applied V migration.

The baseline contains schema objects only. It must not contain application data,
credentials, personal information, or `flyway_schema_history`.

## Verification

### Automated

The GitHub Actions backend job starts a dedicated PostgreSQL 16 Testcontainer.
`FlywayPostgresMigrationIntegrationTest` verifies that:

1. the database starts empty;
2. Flyway applies B32 as one `SQL_BASELINE` migration;
3. Flyway validation succeeds;
4. the expected application tables and fulfillment columns exist.

Run the same migration verification locally without external database settings:

```shell
cd backend
./gradlew postgresTest --tests "com.ymall.backend.migration.FlywayPostgresMigrationIntegrationTest"
```

### Existing database

Start the normal Docker PostgreSQL service and the backend with the local profile.
The application must validate the canonical checksums, apply V29 through V32,
pass Hibernate schema validation, and start without editing
`flyway_schema_history`.

Confirm the applied range without exposing credentials:

```sql
SELECT version, description, checksum, success
FROM flyway_schema_history
WHERE version IN ('20', '24', '25', '27', '29', '30', '31', '32')
ORDER BY installed_rank;
```

## Rules for future migrations

- Check the highest migration version on the latest `develop` before assigning a
  new number.
- Never edit, rename, or delete a migration that may have been applied.
- Use a new version for constraint, column, index, or data backfill changes.
- Keep migrations safe for both an upgraded database and the latest baseline.
- Update the baseline only after the versioned migration has been verified.
- Run the PostgreSQL migration integration test before merging migration changes.
