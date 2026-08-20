# Local demo data

The demo initializer is isolated from production migrations. It is only created
when the `dev` Spring profile is active and only runs when
`DEMO_DATA_ENABLED=true`.

From `backend/`, after configuring the normal database and JWT environment
variables, start it with:

```bash
SPRING_PROFILES_ACTIVE=dev DEMO_DATA_ENABLED=true mvn spring-boot:run
```

Demo login:

- Email: `admin@wafabureau.ma`
- Password: `Admin123!`

The admin email is the seed marker. Restarting the application does not insert
the dataset again.

To reset a disposable local PostgreSQL database, stop the application and run:

```bash
psql -h localhost -U wafa_gestion -d wafa_gestion \
  -f src/main/resources/db/demo/reset-demo-data.sql
```

Then restart with the two demo settings enabled. The reset script deletes all
application data in that local database, preserves the schema and Flyway
history, and is not part of Flyway's configured migration location.
