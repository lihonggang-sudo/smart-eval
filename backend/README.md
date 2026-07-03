# Smart Eval Backend

## Requirements

- Java 17
- Maven 3.9+
- PostgreSQL 16+

## Local Database

Create a database named `smart_eval` and a user named `smart_eval` with password `smart_eval`.

## Run Tests

```bash
mvn test
```

## Run API

```bash
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

Administrator bootstrap is disabled by default. For a clean local database, enable it explicitly on first startup:

```bash
APP_BOOTSTRAP_ADMIN_ENABLED=true APP_BOOTSTRAP_ADMIN_USERNAME=admin APP_BOOTSTRAP_ADMIN_PASSWORD=<local-password> mvn spring-boot:run
```

If bootstrap is enabled and the `admin_user` table is empty, `app.bootstrap.admin.username`, `app.bootstrap.admin.password`, and `app.bootstrap.admin.display-name` must all be configured. Disable bootstrap again after creating the first administrator.
