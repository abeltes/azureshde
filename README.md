# hrmws

Minimal Spring Boot REST service.

## Endpoint

GET `/api/test?dateFrom=yyyy-mm-dd&dateTo=yyyy-mm-dd`

Example response:

```json
{
  "retVal": "OK",
  "dateFrom": "2026-09-01",
  "dateTo": "2026-09-09"
}
```

## Run

Install Java 17+ and Maven, then run:

```bash
mvn spring-boot:run
```

Before starting the service, set `ORACLE_DB_PASSWORD`, `FOTA_USERNAME`,
`FOTA_PASSWORD`, `SHDE_CLIENT_ID`, and `SHDE_CLIENT_SECRET` in the environment.
`ORACLE_DB_URL` and `ORACLE_DB_USERNAME` can also be set to override the
development defaults.
