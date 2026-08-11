# Admin API

Base URL: `http://localhost:8080`

## 认证

所有 `/api/admin/**` 接口使用 **HTTP Basic Authentication**。

服务启动前必须通过环境变量配置凭证：

```bash
SILVER_CARE_PROD_ADMIN_USERNAME=...
SILVER_CARE_PROD_ADMIN_PASSWORD=...
```

---

## 设备

### GET /api/admin/devices

分页查询所有设备。

参数（Pageable）：`?page=0&size=20&sort=id,desc`

返回 `Page<Device>`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 设备ID |
| deviceNo | String | 设备编号 |
| model | String | 型号 |
| status | Enum | ONLINE / OFFLINE |
| batteryLevel | Integer | 电量 |
| stepCount | Integer | 设备通过 LK 心跳或位置报文上报的每日计步值 |
| lastOnlineAt | Instant | 最后在线时间 |
| lastHeartbeatAt | Instant | 最后心跳时间 |

---

### GET /api/admin/devices/{id}

查询单个设备详情，返回 `Device`。

---

### GET /api/admin/devices/{id}/latest-health

查询该设备最新一条健康数据，返回 `HealthRecord`。

| 字段 | 类型 | 说明 |
|------|------|------|
| heartRate | Integer | 心率 |
| systolicPressure | Integer | 收缩压 |
| diastolicPressure | Integer | 舒张压 |
| bodyTemperature | BigDecimal | 体温 |
| heightCm | Integer | 身高(cm) |
| weightKg | Integer | 体重(kg) |
| age | Integer | 年龄 |
| genderCode | Integer | 性别代码 |
| sourceCommand | String | 来源协议命令 |
| measuredAt | Instant | 测量时间 |

---

### GET /api/admin/devices/{id}/health-records

查询该设备最新 100 条健康数据，返回 `List<HealthRecord>`。

健康记录增加 `heartRateStatus`、`bloodPressureStatus`、`temperatureStatus` 和 `invalidReason`。
设备失败状态值和数值 `0` 不再作为正常指标返回。

### GET /api/admin/devices/{id}/health-summary

分别返回最新有效心率、血压和体温，每项包含独立测量时间、测量状态和 `FRESH/STALE/UNKNOWN` 新鲜度。

### 设备快捷动作

```text
POST /api/admin/devices/{id}/actions
GET  /api/admin/devices/{id}/actions/capabilities
GET  /api/admin/devices/{id}/actions/{actionId}
GET  /api/admin/devices/{id}/actions
```

快捷动作只接受 `DeviceActionType` 枚举。通信未人工确认时 capability 返回禁用原因，后端不会发送报文。

---

### GET /api/admin/devices/{id}/latest-location

查询该设备最新一条位置数据，返回 `LocationRecord`。

| 字段 | 类型 | 说明 |
|------|------|------|
| latitude | BigDecimal | 纬度 |
| longitude | BigDecimal | 经度 |
| speed | BigDecimal | 速度 |
| direction | BigDecimal | 方向 |
| altitude | BigDecimal | 海拔 |
| satelliteCount | Integer | 卫星数 |
| gsmSignal | Integer | GSM信号强度 |
| batteryLevel | Integer | 电量 |
| gpsValid | Boolean | GPS是否有效 |
| locatedAt | Instant | 定位时间 |
| approximateAddress | String | 大概位置名称，可能为空 |
| addressStatus | String | PENDING / RESOLVED / NOT_FOUND / FAILED / SKIPPED |

---

### GET /api/admin/devices/{id}/location-records

查询该设备最新 100 条位置数据，返回 `List<LocationRecord>`。

---

## 原始报文

### GET /api/admin/raw-packets

分页查询所有原始报文日志。

参数（Pageable）：`?page=0&size=20&sort=id,desc`

返回 `Page<RawPacketLog>`

| 字段 | 类型 | 说明 |
|------|------|------|
| deviceNo | String | 设备编号 |
| direction | Enum | UPLINK / DOWNLINK |
| command | String | 协议命令 (LK/UD/UD2/AL 等) |
| lenHex | String | 长度校验位(hex) |
| content | String | 报文内容 |
| rawPacket | String | 原始完整报文 |
| parseStatus | Enum | SUCCESS / FAILED |
| errorMessage | String | 解析错误信息 |
| receivedAt | Instant | 接收时间 |
