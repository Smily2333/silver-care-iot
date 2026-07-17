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

Admin APIs under `/api/admin/**` use HTTP Basic authentication. The Web client asks for the credentials
at login time and keeps them only in the current browser session. Configure credentials before starting:

```bash
SILVER_CARE_ADMIN_USERNAME=...
SILVER_CARE_ADMIN_PASSWORD=...
SILVER_CARE_DB_PASSWORD=...
WECHAT_MINIAPP_APP_SECRET=...
```

See `docs/deployment.md` and `.env.example` for the complete deployment checklist. The mini program uses
WeChat `wx.login`, server-side sessions, and per-user device bindings; its AppSecret belongs only on the
backend server.

Default ports:

- HTTP API: `8080`
- Device TCP gateway: `9001`
