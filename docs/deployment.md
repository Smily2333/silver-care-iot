# Development and production deployment

## Environment boundary

The backend defaults to the `dev` Spring profile. Development uses `silver_care_dev`, runs JPA with
`ddl-auto=update`, disables the device TCP gateway by default, and leaves every typed device command
disabled. Local startup must never be given production environment variables. A startup guard requires
development database names to end in `_dev`; the production profile rejects database names with that
suffix and also rejects activating `dev` and `prod` together.

Production must explicitly set:

```text
SPRING_PROFILES_ACTIVE=prod
```

The production profile uses the existing `silver_care` database, sets `ddl-auto=validate`, and has no
fallback values for database credentials, administrator credentials, or the WeChat secret. A missing value
therefore stops startup. Templates are tracked separately:

- Development: `.env.example`
- Production: `deploy/env/backend-prod.env.example`

Create the local database once; JPA will maintain its development-only schema:

```sql
CREATE DATABASE silver_care_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Required server settings

Configure these environment variables before starting the backend. Never commit their real values.

```text
SPRING_PROFILES_ACTIVE=prod
SILVER_CARE_PROD_DB_URL
SILVER_CARE_PROD_DB_USERNAME
SILVER_CARE_PROD_DB_PASSWORD
SILVER_CARE_PROD_ADMIN_USERNAME
SILVER_CARE_PROD_ADMIN_PASSWORD
WECHAT_MINIAPP_PROD_APPID
WECHAT_MINIAPP_PROD_APP_SECRET
SILVER_CARE_PROD_GEOCODING_ENABLED
SILVER_CARE_PROD_NOMINATIM_BASE_URL
SILVER_CARE_PROD_GEOCODING_BACKFILL_LIMIT
SILVER_CARE_PROD_CONFIRMED_DEVICE_ACTIONS
SILVER_CARE_PROD_ALLOW_HEALTH_WITHOUT_WEAR_STATUS
```

`WECHAT_MINIAPP_PROD_APP_SECRET` is available in the WeChat mini program management console. It stays on the
server and must never be added to the mini program source code.

Install `deploy/systemd/silver-care-iot.service` on the server and store the real production environment at
`/etc/silver-care-iot/backend-prod.env` with file mode `600`. This keeps production secrets outside the
repository and makes the active profile explicit on every restart.

## Production database migrations

Migration files no longer contain `USE silver_care`. Always select the target database explicitly. For the
current production database:

```bash
mysql --user=silvercare --password --database=silver_care < deploy/mysql/2026-08-11-location-address.sql
mysql --user=silvercare --password --database=silver_care < deploy/mysql/2026-08-11-health-monitoring.sql
mysql --user=silvercare --password --database=silver_care < deploy/mysql/2026-08-11-blood-oxygen.sql
mysql --user=silvercare --password --database=silver_care < deploy/mysql/2026-08-11-blood-oxygen-backfill.sql
```

For development, do not run those commands against `silver_care`; the `dev` profile updates
`silver_care_dev` automatically.

## Web and mini program quick switch

Run one of these files from Windows Explorer or a terminal:

```bat
scripts\use-production.cmd
scripts\use-development.cmd
scripts\use-auto.cmd
```

The underlying command is also available directly:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/switch-environment.ps1 production
```

The switch updates `web/.env.local` and `miniapp/environment.js` together:

- `production`: Web's Vite proxy and the mini program both use `https://api.nkucare.cloud`.
- `development`: Web and the mini program both use the local backend at `127.0.0.1:8080`.
- `auto`: Web uses the local backend; mini program `develop` uses local while `trial` and `release` use
  production.

Restart `npm run dev` and recompile the mini program after switching. `web/.env.local` is ignored by Git.
The selected mini program target remains visible in `miniapp/environment.js` so production access is never
implicit.

## HTTPS and routing

Use the filed `nkucare.cloud` domain with a valid TLS certificate. The public layout is:

```text
https://nkucare.cloud/api/*  -> backend http://127.0.0.1:8080/api/*
https://nkucare.cloud/*      -> /var/www/silver-care-iot with SPA history fallback
https://nkucare.cloud/privacy/ -> public privacy policy (`web/public/privacy/index.html`)
https://api.nkucare.cloud/api/* -> backend http://127.0.0.1:8080/api/*
```

The HTTP bootstrap configuration is tracked at `deploy/nginx/silver-care-iot.conf`. Copy the built Web
files from `web/dist` to `/var/www/silver-care-iot` before reloading Nginx. `nkucare.cloud`,
`www.nkucare.cloud`, and `api.nkucare.cloud` must point to `120.53.225.169`; redirect `www` to the root
domain after TLS is enabled.

Vite copies `web/public/privacy/index.html` to `web/dist/privacy/index.html`. Verify the public policy at
`https://nkucare.cloud/privacy/` after every Web deployment.

The domain must have an active ICP filing before Tencent Cloud will pass public traffic to this mainland
server. Keep the filing active when changing DNS or public routing.

The backend HTTP and device TCP ports should not share the same public access policy. Restrict MySQL and
the backend HTTP port to the host or private network. Expose the device TCP port only where the watch
protocol requires it.

## Mini program domain

1. Add `https://api.nkucare.cloud` to the mini program request legal-domain list.
2. Use the environment switcher above. For local mode, disable domain validation only inside WeChat
   Developer Tools.
3. Production mode uses `https://api.nkucare.cloud`. Do not replace it with a backend IP.
4. Upload an experience build and verify login, first binding, health data, location and fall alerts.

## Web map files and address lookup

The current low-volume internal production build uses the standard OpenStreetMap raster endpoint for
interactive viewing only:

```text
VITE_MAP_MODE=raster
VITE_MAP_RASTER_TILE_URL=https://tile.openstreetmap.org/{z}/{x}/{y}.png
```

Keep visible attribution, browser caching and the normal referrer. Do not add offline download, prefetch or
bulk tile requests. The endpoint is best-effort and has no SLA, so self-hosted PMTiles remains the intended
long-term production mode.

Set `VITE_MAP_MODE=pmtiles` when the self-hosted archive is ready. The Web client then reads the basemap
from `/maps/tianjin.pmtiles`. Keep the large map file,
font glyphs, and sprites outside the Web build under `/var/lib/silver-care-maps`:

```text
/var/lib/silver-care-maps/
  tianjin.pmtiles
  assets/fonts/{fontstack}/{range}.pbf
  assets/sprites/v4/light.json
  assets/sprites/v4/light.png
  assets/sprites/v4/light@2x.json
  assets/sprites/v4/light@2x.png
```

Build the Web client with these settings when the deployed filenames differ:

```text
VITE_MAP_PMTILES_URL=/maps/tianjin-2026-08.pmtiles
VITE_MAP_GLYPHS_URL=/maps/assets/fonts/{fontstack}/{range}.pbf
VITE_MAP_SPRITE_URL=/maps/assets/sprites/v4/light
```

Verify that a request containing a `Range` header receives HTTP 206. Never scrape or bulk-download the
public `tile.openstreetmap.org` endpoint to create this archive. Keep the visible OpenStreetMap attribution.

The Web map uses original WGS-84 coordinates; the mini program map uses the API's GCJ-02 display
coordinates.

Run a private Nominatim instance for approximate address lookup, then configure:

```text
SILVER_CARE_PROD_GEOCODING_ENABLED=true
SILVER_CARE_PROD_NOMINATIM_BASE_URL=http://127.0.0.1:7070
SILVER_CARE_PROD_GEOCODING_USER_AGENT=SilverCare/0.1
SILVER_CARE_PROD_GEOCODING_BACKFILL_LIMIT=100
```

Leave geocoding disabled until the private instance is ready. Device packet ingestion does not wait for
address lookup; lookup failures only leave the address unavailable.

## Device quick actions

All typed device commands are disabled until their real-device exchange is confirmed. See
`docs/protocol-verification.md`. Enable only verified actions, for example:

```text
SILVER_CARE_PROD_CONFIRMED_DEVICE_ACTIONS=LOCATE_NOW,MEASURE_HEART_RATE,MEASURE_TEMPERATURE,CONFIGURE_LOCATION_INTERVAL
SILVER_CARE_PROD_ALLOW_HEALTH_WITHOUT_WEAR_STATUS=true
SILVER_CARE_PROD_AUTOMATIC_MONITORING_ENABLED=true
SILVER_CARE_PROD_HEART_RATE_INTERVAL_MINUTES=60
SILVER_CARE_PROD_TEMPERATURE_INTERVAL_MINUTES=240
SILVER_CARE_PROD_LOCATION_UPLOAD_SECONDS=600
```

Do not enable heart-rate or temperature actions until the wear-status protocol has been confirmed, unless
an authorised operator explicitly accepts the temporary override.

## First deployment after this change

JPA creates `miniapp_users`, `miniapp_sessions`, and `device_bindings` on startup. Existing device and
measurement tables are unchanged. Existing users must enter the device number once and confirm the
wearer's name to establish the first binding.

One device currently has one primary mini program account. Family sharing should be implemented later
with owner-approved invitations rather than by allowing another account to enter the device number.
