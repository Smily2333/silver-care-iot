# Private Nominatim deployment notes

Silver Care must use a private Nominatim instance for automatic reverse geocoding. The public OSM
Nominatim endpoint is not configured anywhere in the application.

Deployment requirements:

- import only the operational region needed by the project when practical;
- bind the HTTP service to `127.0.0.1:7070` or a private network, not the public Internet;
- update the OSM extract on a controlled schedule;
- protect the database and import volume with normal server backups;
- verify `GET /reverse?lat=39.032137&lon=117.7007781&format=jsonv2&addressdetails=1&accept-language=zh-CN`;
- enable the application only after the private endpoint responds successfully.

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
