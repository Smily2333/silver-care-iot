# MVP Scope

## In Scope

- Device TCP connection and heartbeat
- Raw packet logging
- Device online/offline status
- Location upload parsing
- Temperature upload parsing
- Blood pressure and heart rate upload parsing
- Admin API skeleton
- Mini-program API skeleton

## Supported Protocol Commands

- `LK`: heartbeat
- `UD`, `UD_LTE`, `UD_WCDMA`, `UD_TDSCDMA`, `UD_CDMA`: location
- `UD2`: blind-zone location supplement
- `AL`, `AL_LTE`, `AL_WCDMA`, `AL_TDSCDMA`, `AL_CDMA`: alarm upload, stored only in MVP
- `btemp2`: body temperature upload
- `bphrt`: blood pressure and heart rate upload

## Out of Scope For MVP

- SpO2, because protocol V3.0 has no blood oxygen upload command
- SOS business workflow
- Low battery, wrist-off, fall detection workflow
- Phonebook, find watch, remote shutdown/reboot
- Geo-fence
- Chat
- Big-screen statistics
