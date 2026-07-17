# Deployment checklist

## Required server settings

Configure these environment variables before starting the backend. Never commit their real values.

```text
SILVER_CARE_DB_URL
SILVER_CARE_DB_USERNAME
SILVER_CARE_DB_PASSWORD
SILVER_CARE_ADMIN_USERNAME
SILVER_CARE_ADMIN_PASSWORD
WECHAT_MINIAPP_APPID
WECHAT_MINIAPP_APP_SECRET
```

`WECHAT_MINIAPP_APP_SECRET` is available in the WeChat mini program management console. It stays on the
server and must never be added to the mini program source code.

## HTTPS and routing

Use the filed `nkucare.cloud` domain with a valid TLS certificate. The public layout is:

```text
https://nkucare.cloud/api/*  -> backend http://127.0.0.1:8080/api/*
https://nkucare.cloud/*      -> /var/www/silver-care-iot with SPA history fallback
```

The HTTP bootstrap configuration is tracked at `deploy/nginx/silver-care-iot.conf`. Copy the built Web
files from `web/dist` to `/var/www/silver-care-iot` before reloading Nginx. Both `nkucare.cloud` and
`www.nkucare.cloud` must point to `120.53.225.169`; redirect `www` to the root domain after TLS is enabled.

The domain currently needs ICP filing before Tencent Cloud will pass public traffic to this mainland
server. Do not run certificate issuance or switch the mini program release URL until the filing is active.

The backend HTTP and device TCP ports should not share the same public access policy. Restrict MySQL and
the backend HTTP port to the host or private network. Expose the device TCP port only where the watch
protocol requires it.

## Mini program domain

1. Add `https://nkucare.cloud` to the mini program request legal-domain list.
2. Set the same HTTPS origin for `trial` and `release` in `miniapp/config.js`.
3. Keep the current HTTP IP only under `develop`; it is for the WeChat developer tool.
4. Upload an experience build and verify login, first binding, health data, location and fall alerts.

## Web map tiles

The Web client defaults to the public OpenStreetMap tile endpoint for development. Before commercial or
high-volume use, configure `VITE_MAP_TILE_URL` and `VITE_MAP_TILE_ATTRIBUTION` for a compliant tile
provider or a self-hosted tile service. The Web map uses the original WGS-84 coordinates; the mini program
map uses the API's GCJ-02 display coordinates.

## First deployment after this change

JPA creates `miniapp_users`, `miniapp_sessions`, and `device_bindings` on startup. Existing device and
measurement tables are unchanged. Existing users must enter the device number once and confirm the
wearer's name to establish the first binding.

One device currently has one primary mini program account. Family sharing should be implemented later
with owner-approved invitations rather than by allowing another account to enter the device number.
