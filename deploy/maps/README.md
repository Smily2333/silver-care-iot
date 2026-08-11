# Web map assets

Production map assets live outside the Git repository at `/var/lib/silver-care-maps`.

Required files:

```text
tianjin.pmtiles
assets/fonts/{fontstack}/{range}.pbf
assets/sprites/v4/light.json
assets/sprites/v4/light.png
assets/sprites/v4/light@2x.json
assets/sprites/v4/light@2x.png
```

Use an OpenStreetMap-derived PMTiles archive obtained or generated in accordance with its data licence.
Do not crawl `tile.openstreetmap.org`. Keep the source version and licence information alongside each
deployed archive, and retain visible `© OpenStreetMap contributors` attribution in the Web map.

After copying the files, verify byte-range support:

```text
curl -I -H "Range: bytes=0-127" https://nkucare.cloud/maps/tianjin.pmtiles
```

The response must be `206 Partial Content` before enabling the map in production.
