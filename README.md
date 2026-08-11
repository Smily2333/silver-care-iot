# Silver Care IoT

MVP for an elderly-care smart watch IoT platform.

Current focus:

- Backend first
- TCP long-connection device gateway
- Protocol parsing for `LK`, `UD`, `UD2`, `AL`, `btemp2`, `bphrt`
- Device, health, location, alarm, and raw packet data model
- REST APIs for future Web and mini-program clients

## Structure

```text
silver-care-iot/
  backend/     Spring Boot backend and TCP gateway
  web/         Reserved for Web admin frontend
  miniapp/     Reserved for mini-program frontend
  docs/        Protocol and requirement notes
```

## Backend

Planned stack:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL

Run after installing JDK and Maven:

```bash
cd backend
mvn spring-boot:run
```

The default Spring profile is `dev`. It connects only to the `silver_care_dev` database and keeps the
device TCP gateway and typed device commands disabled. Create that database once before local startup:

```sql
CREATE DATABASE silver_care_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Production never activates implicitly. The server service must set `SPRING_PROFILES_ACTIVE=prod`; missing
production database, admin, or WeChat credentials then cause startup to fail instead of falling back to
development values.

On Windows, switch both the Web development proxy and mini program API target with one command:

```bat
scripts\use-production.cmd
scripts\use-development.cmd
scripts\use-auto.cmd
```

The repository is currently set to `production`. Restart Vite and recompile the mini program after a switch.

Admin APIs under `/api/admin/**` use HTTP Basic authentication. The Web client asks for the credentials
at login time and keeps them only in the current browser session. Configure credentials before starting:

Development settings are documented in `.env.example`. Production settings use the separately named
variables in `deploy/env/backend-prod.env.example`.

See `docs/deployment.md` and `.env.example` for the complete deployment checklist. The mini program uses
WeChat `wx.login`, server-side sessions, and per-user device bindings; its AppSecret belongs only on the
backend server.

Default ports:

- HTTP API: `8080`
- Device TCP gateway: `9001`
