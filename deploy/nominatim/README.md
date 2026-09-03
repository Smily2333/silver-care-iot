# Private Nominatim deployment

Silver Care must use a private Nominatim instance for automatic reverse geocoding. The public OSM
Nominatim endpoint is not configured anywhere in the application.

Deployment requirements:

- import only the operational region needed by the project when practical;
- bind the HTTP service to `127.0.0.1:7070` or a private network, not the public Internet;
- update the OSM extract on a controlled schedule;
- protect the database and import volume with normal server backups;
- verify `GET /reverse?lat=39.032137&lon=117.7007781&format=jsonv2&addressdetails=1&accept-language=zh-CN`;
- enable the application only after the private endpoint responds successfully.

## Supported production topology

Run Nominatim on a **dedicated private VM**. Bind the API to `127.0.0.1:7070`
when the backend is on that VM, or bind it to the VM's private address and allow
only the backend VM's private IP in the security group/firewall. Do not expose port
7070 publicly and do not route automatic requests to the public OSM Nominatim service.

The current application host must not be used for the import: it has 2 vCPU, 3.6 GiB
RAM and only 27 GiB free disk, while it is already serving the application and database.
Provision a separate SSD-backed VM with at least 8 vCPU, 32 GiB RAM and 150 GiB free
disk for the Beijing--Tianjin--Hebei extract; use 16 vCPU, 64 GiB RAM and 300 GiB free
disk for a less constrained import/update window. These are operational safety floors,
not a guarantee of a particular import duration.

## Data preparation and import

1. Obtain a current, legally usable Beijing--Tianjin--Hebei `.osm.pbf` extract from an
   approved OSM extract provider. Record its source URL, publication date, SHA-256 and
   the ODbL attribution in the deployment change record.
2. Store the file as `/var/lib/silver-care-nominatim/import/beijing-tianjin-hebei-latest.osm.pbf`.
   Do not overwrite an already-working PostgreSQL data directory.
3. Copy `.env.example` to `/etc/silver-care-nominatim/nominatim.env`, set a generated
   local database password, then run from this directory:

   ```bash
   docker compose --env-file /etc/silver-care-nominatim/nominatim.env up -d
   ```

4. Watch `docker compose logs -f nominatim`, `free -h`, `df -h`, and the backend health
   during import. Stop and move to a larger VM if memory, swap, disk, or application
   health becomes unsafe. Never delete the current data volume as an automatic recovery step.
5. After the health check passes, verify locally:

   ```bash
   curl --fail --get 'http://127.0.0.1:7070/reverse' \
     --data-urlencode 'lat=39.032137' \
     --data-urlencode 'lon=117.7007781' \
     --data-urlencode 'format=jsonv2' \
     --data-urlencode 'addressdetails=1' \
     --data-urlencode 'accept-language=zh-CN'
   ```

The compose file pins the Nominatim image build, persists PostgreSQL and flatnode data,
uses bounded local logs, disables search-only extras, and listens only on loopback.
For a split private-network topology, change only the host-side bind address after adding
a firewall/security-group allow-list for the backend host.

## Application enablement and rollback

Application settings:

```text
SILVER_CARE_PROD_GEOCODING_ENABLED=true
SILVER_CARE_PROD_NOMINATIM_BASE_URL=http://127.0.0.1:7070
SILVER_CARE_PROD_GEOCODING_USER_AGENT=SilverCare/0.1
SILVER_CARE_PROD_GEOCODING_BACKFILL_LIMIT=100
```

On startup the backend queues up to `SILVER_CARE_PROD_GEOCODING_BACKFILL_LIMIT` recent valid records whose
address is missing or previously failed. New records are resolved asynchronously and never block device
packet ingestion.

Add the four variables to `/etc/silver-care-iot/backend-prod.env` without replacing the
file or printing its contents; keep mode `600`. Restart `silver-care-backend.service` only
after the private API is healthy. To roll back, set only
`SILVER_CARE_PROD_GEOCODING_ENABLED=false` and restart the backend. Do not delete resolved
addresses, `location_address_cache`, or the Nominatim volumes.
