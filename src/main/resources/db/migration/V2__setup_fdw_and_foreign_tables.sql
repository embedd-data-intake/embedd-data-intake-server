CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SERVER IF NOT EXISTS timescale_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (
        host '${timescaleHost}',
        port '${timescalePort}',
        dbname '${timescaleDb}'
    );

CREATE USER MAPPING IF NOT EXISTS FOR postgres
    SERVER timescale_server
    OPTIONS (
        user '${timescaleUser}',
        password '${timescalePassword}'
    );

CREATE SCHEMA IF NOT EXISTS foreign_schema;

IMPORT FOREIGN SCHEMA public
    FROM SERVER timescale_server
    INTO foreign_schema;
