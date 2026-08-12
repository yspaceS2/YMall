\set ON_ERROR_STOP on
\getenv migration_user DB_MIGRATION_USERNAME
\getenv app_user DB_APP_USERNAME
\getenv backup_user DB_BACKUP_USERNAME

SELECT count(*) = 3 AS roles_are_restricted
FROM pg_roles
WHERE rolname IN (:'migration_user', :'app_user', :'backup_user')
  AND NOT rolsuper
  AND NOT rolcreatedb
  AND NOT rolcreaterole
  AND NOT rolreplication
  AND NOT rolbypassrls
\gset
\if :roles_are_restricted
\else
    \echo 'Database roles are missing or have elevated attributes.'
    \quit 1
\endif

SELECT owner_role.rolname = :'migration_user' AS migration_owns_database
FROM pg_database database
JOIN pg_roles owner_role ON owner_role.oid = database.datdba
WHERE database.datname = current_database()
\gset
\if :migration_owns_database
\else
    \echo 'Migration role does not own the database.'
    \quit 1
\endif

SELECT NOT has_schema_privilege(:'app_user', 'public', 'CREATE') AS app_cannot_create_schema_objects
\gset
\if :app_cannot_create_schema_objects
\else
    \echo 'Application role unexpectedly has schema CREATE privilege.'
    \quit 1
\endif

SELECT NOT EXISTS (
    SELECT 1
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relkind IN ('r', 'p')
      AND has_table_privilege(:'backup_user', c.oid, 'INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
) AS backup_is_read_only
\gset
\if :backup_is_read_only
\else
    \echo 'Backup role unexpectedly has write privilege.'
    \quit 1
\endif

SELECT current_setting('log_connections') = 'on'
   AND current_setting('log_disconnections') = 'on'
   AND current_setting('log_statement') = 'ddl' AS audit_settings_enabled
\gset
\if :audit_settings_enabled
\else
    \echo 'PostgreSQL connection or DDL audit settings are not enabled.'
    \quit 1
\endif

\echo 'PostgreSQL least-privilege roles and audit settings are valid.'
