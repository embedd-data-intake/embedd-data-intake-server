# Embedd Data Intake - Server

## About

This repository is a part of a bigger project about a platform for ingesting and serving IoT data. The server's (this 
repository) purpose is to expose REST API for the user to allow them to create accounts and register, manage share IoT
nodes as well as access the data gathered (using a MQTT ingest) by the nodes.

## Stack

### Server

- Maven
- Java 25
- Spring Boot 4.1.0

### Database

- PostgreSQL (for registering user and device registration)
- PostgreSQL TimescaleDB Hypertable (for storing the time series data gathered by the IoT nodes)

## Endpoints

### AuthController:

- `POST /api/v1/auth/register` - User registration

- `POST /api/v1/auth/login` - User login

- `POST /api/v1/auth/refresh` - Get a new session token using the refresh token

- `POST /api/v1/auth/logout` - Invalidates a refresh token and the session tokes created from it

- `POST /api/v1/auth/logout-all` - Invalidates all refresh tokens and the session tokens create from them

### UserController

All the mentioned endpoints require a session token under the `Authorization` header.

- `GET /api/v1/user` - Retrieve a user's id and email

- `GET /api/v1/user/attributes` - Retrieve the attributes used by the user

- `GET /api/v1/user/device` - Retrieve the devices which the user has access to

- `GET /api/v1/user/data` - Access the data under the devices for which the user has at least READ access to (supports 
date range filtering, attribute filtering, pagination)

### DeviceController

All the mentioned endpoints require a session token under the `Authorization` header.
Operations executed on a specific device (with `deviceId` specification) require the user to have a minimal access role 
for the device.

- `POST /api/v1/device` - Add a new device

- `GET /api/v1/device/{deviceId}` - (for >=READ permission) Retrieve a device's information

- `GET /api/v1/device/{deviceId}/attributes` - (for >=READ permission) Retrieve the attributes gathered by the device

- `GET /api/v1/device/{deviceId}/access` - (for >=ADMIN permission) Retrieve the users that have some level of access
along with their level of access

- `GET /api/v1/device/{deviceId}/data` - (for >=READ permission) Retrieve the data gathered by the device (supports date
range filtering, attribute filtering, pagination)

- `POST /api/v1/device/{deviceId}/share` - (for >=ADMIN permission) set another user's level of access to a device by
the target user's email

### Misc

- `GET /swagger-ui/index.html` - Swagger docs (only available under the `dev` profile)

- `GET /actuator**` - Health probe

## Database

### App DB

- Uses flyway for migrations

- Uses soft deletes (`deleted_at` column) which are automatically handled by JPA (with`@SQLRestriction("deleted_at is 
NULL")` and `@SQLDelete(sql = "UPDATE user_device SET deleted_at = NOW() WHERE id =?")`)

- Uses Foreign Data Wrapper (FDW) to access the time series data from the TimescaleDB without requiring Hibernate to
create a second connection

### Telemetry DB

- Uses narrow table pattern giving the users the flexibility of introducing custom attributes without modifying the
table structure

- Uses Timescale DB for the `sensor_data` table which stores the data gathered by the IoT modules 

- The `sensor_data` table is identified by an embedded id (`timestamp`, `device_id`, `attribute_id`) which allows for
automatic data partioning
