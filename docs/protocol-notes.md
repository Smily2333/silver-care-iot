# Protocol Notes

## Frame

```text
[vendor*deviceNo*LEN*content]
```

- `vendor`: fixed vendor mark, for example `3G`
- `deviceNo`: watch device ID / IMEI
- `LEN`: 4-character uppercase hexadecimal ASCII length
- `content`: command and parameters

`LEN` counts only the byte length of `content`.

Example:

```text
[3G*8800000015*000D*LK,50,100,100]
```

`LK,50,100,100` is 13 ASCII bytes, so `LEN=000D`.

## Transport

- Device creates a TCP long connection to the server.
- Uplink and downlink packets share the same TCP connection.
- The backend keeps a `deviceNo -> connection` registry for future downlink commands.

## MVP Commands

### LK

Heartbeat:

```text
LK,steps,rolloverCount,batteryPercent
```

The server replies:

```text
LK
```

### btemp2

Temperature upload:

```text
btemp2,type,temp
```

- `type`: `0` forehead, `1` wrist
- `temp`: Celsius decimal string. `0` and `1` are measurement abnormal special values.

### bphrt

Blood pressure and heart rate upload:

```text
bphrt,systolic,diastolic,heartRate,heightCm,gender,age,weightKg
```

If only heart rate is measured, systolic and diastolic may be `0`.

### UD / UD2 / AL / 4G variants

Location-like packets:

```text
command,date,time,gpsValid,lat,latHemisphere,lng,lngHemisphere,speed,direction,altitude,satelliteCount,gsmSignal,battery,steps,rolloverCount,terminalStatus,...
```

`UD2` is blind-zone supplement data. `AL` is alarm upload. MVP stores them and treats location fields the same where present.
