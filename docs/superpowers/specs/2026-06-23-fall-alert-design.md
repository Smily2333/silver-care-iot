# 跌倒警报功能设计文档

**日期：** 2026-06-23  
**项目：** silver-care-iot  
**状态：** 待实现

---

## 1. 背景与目标

当老人疑似发生跌倒等安全事件时，手表设备会主动向后端上报 `AL` 命令（Alarm Upload）。当前代码已能接收 `AL` 包，但仅将其作为普通位置数据存入 `location_records`，没有任何告警语义和通知机制。

**目标：** 在小程序端让监护人能及时感知跌倒事件，并可查看告警历史和联动位置。

**不在范围内：** 处理状态流转（已处理/未处理）、微信订阅消息推送（后续扩展）。

---

## 2. 协议说明

设备检测到跌倒时上报 `AL` 命令，格式与 `UD` 完全相同：

```
AL,date,time,gpsValid,lat,latHemisphere,lng,lngHemisphere,speed,direction,
altitude,satelliteCount,gsmSignal,battery,steps,rolloverCount,terminalStatus,...
```

服务器回复 `AL` 确认收包。

`AL` 命令名本身即表示告警类型（跌倒/安全事件），无需解析 `terminalStatus` 字段识别类型。

下发配置命令（暂不实现，预留）：
- `FALLDOWN,X,Y` — 开关跌倒报警及拨号功能
- `LSSET,ls` — 设置跌倒灵敏度

---

## 3. 架构设计

### 3.1 数据层 — 新增 `fall_alerts` 表

新建独立实体 `FallAlert`，与 `location_records` 平行存在。`AL` 包到来时**同时写两张表**：`location_records` 保留现有位置逻辑不变，`fall_alerts` 承载告警语义。

```
fall_alerts
├── id              BIGINT PK AUTO_INCREMENT
├── deviceId        BIGINT NOT NULL   -- 关联 devices.id
├── latitude        DECIMAL(11,7)
├── longitude       DECIMAL(12,7)
├── gpsValid        BOOLEAN
├── locationRecordId BIGINT           -- 关联 location_records.id，便于联动跳转
├── rawPacketId     BIGINT            -- 关联 raw_packet_logs.id
└── alertedAt       TIMESTAMP NOT NULL  -- 告警发生时间（解析自包内 date+time 字段）
```

索引：`(deviceId, alertedAt DESC)`

**不加 `status` 字段**，后续需要处理流程时再通过 migration 添加，不影响现有结构。

### 3.2 后端层

**新增文件：**

| 文件 | 说明 |
|------|------|
| `domain/entity/FallAlert.java` | JPA 实体 |
| `repository/FallAlertRepository.java` | `findTop20ByDeviceIdOrderByAlertedAtDesc` |
| `service/FallAlertService.java` | `saveAlert(device, frame, locationRecordId, rawPacketId)` |
| `api/MiniappAlertController.java` | REST 接口 |

**修改文件：**

| 文件 | 改动 |
|------|------|
| `service/DevicePacketDispatcher.java` | `AL` 分支：先调 `locationDataService.saveLocation()` 取返回的 `LocationRecord`，再调 `fallAlertService.saveAlert()`，最后 `sendAck` |
| `service/LocationDataService.java` | `saveLocation()` 改为返回 `LocationRecord`（现在返回 void） |

### 3.3 API 接口

```
GET /api/miniapp/devices/{deviceNo}/fall-alerts?size=20
```

返回：

```json
[
  {
    "id": 1,
    "deviceId": 42,
    "latitude": 30.1234567,
    "longitude": 120.1234567,
    "gpsValid": true,
    "locationRecordId": 88,
    "alertedAt": "2026-06-23T10:30:00Z"
  }
]
```

```
GET /api/miniapp/devices/{deviceNo}/fall-alerts/latest
```

返回最新一条告警，用于概览页轮询判断是否有新告警。若无告警返回 `204 No Content`。

### 3.4 小程序端

**新增页面：** `pages/alerts/alerts`（告警列表页）

**修改文件：**

| 文件 | 改动 |
|------|------|
| `utils/api.js` | 新增 `getFallAlerts(deviceNo)`、`getLatestFallAlert(deviceNo)` |
| `pages/overview/overview.js` | `onShow` 时轮询 `/fall-alerts/latest`，有新告警弹 modal |
| `pages/overview/overview.wxml` | 有未确认告警时在顶部显示红色警告横幅，点击跳转告警列表 |
| `app.json` | 注册 `pages/alerts/alerts` 路由 |

**概览页告警横幅逻辑：**

`onShow` 触发时请求 `latest`，将返回的 `alertedAt` 与本地 `lastSeenAlertAt`（存入 `wx.storage`）对比，若更新则显示横幅并更新本地时间戳。用户点击横幅或导航至告警页后，视为「已确认」，横幅消失。

**告警列表页布局：**
- 顶部渐变头部，标题「跌倒警报」
- 每条记录卡片：告警时间、位置坐标（GPS 有效时显示）、「查看位置」按钮（跳转 location 页并传入经纬度）
- 空状态：「暂无告警记录」

---

## 4. 数据流

```
设备发送 AL 包
    │
    ▼
DevicePacketDispatcher.dispatch()
    │
    ├─► locationDataService.saveLocation()  ──► location_records 表
    │       └─ 返回 LocationRecord
    │
    ├─► fallAlertService.saveAlert()  ──► fall_alerts 表
    │
    └─► sendAck(AL)  ──► 设备收到确认

小程序 onShow
    │
    ▼
GET /fall-alerts/latest
    │
    ├─ 有新告警 → 显示红色横幅 + modal 提醒
    └─ 无新告警 → 不操作
```

---

## 5. 后续扩展预留

- **订阅消息推送**：`FallAlertService.saveAlert()` 内预留注释 `// TODO: trigger wx subscribe message push`，后续在此处调用微信推送接口，不需要改其他代码
- **处理状态**：`fall_alerts` 表后续可加 `status VARCHAR(16) DEFAULT 'PENDING'` 列，通过 Flyway migration 添加，不影响现有字段

---

## 6. 测试要点

- 后端单元测试：`FallAlertService` 解析 `AL` 包的 `alertedAt` 字段正确
- 后端单元测试：`DevicePacketDispatcher` 收到 `AL` 包后同时写两张表并回复 ACK
- 集成测试：`GET /fall-alerts` 接口返回正确分页数据
- 小程序手动测试：概览页 `onShow` 时有新告警弹出 modal；横幅在点击后消失
