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

CREATE FOREIGN TABLE IF NOT EXISTS foreign_sensor_data (
    timestamp TIMESTAMPTZ NOT NULL,
    device_id UUID NOT NULL,
    payload JSONB NOT NULL
)
SERVER timescale_server
OPTIONS (schema_name 'public', table_name 'sensor_data');
