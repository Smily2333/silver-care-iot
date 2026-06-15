# Silver Care IoT 前端设计文档

**日期：** 2026-06-01  
**状态：** 已确认

---

## 一、背景

后端已完成 TCP 设备网关（端口 9001）和 Admin REST API（端口 8080）。  
本文档描述需要新增的两个前端客户端及配套后端 API。

---

## 二、整体架构

```
设备(手表) ──TCP:9001──> Spring Boot 后端
                              │
                    ┌─────────┴─────────┐
                    │                   │
              HTTP:8080           HTTP:8080
                    │                   │
            Vue3 管理后台        微信小程序
           (管理员使用)          (家属使用)
           Basic Auth           无需认证
```

---

## 三、后端新增 API（MiniApp API）

### 路径前缀

`/api/miniapp/**` — 无需认证，Spring Security 放行此路径。

### 接口列表

#### GET /api/miniapp/devices/{deviceNo}/overview

一次请求返回首页所需全部数据，减少小程序网络请求次数。

响应结构：
```json
{
  "device": {
    "id": 1,
    "deviceNo": "8800000015",
    "model": "SilverWatch-X1",
    "status": "ONLINE",
    "batteryLevel": 85,
    "stepCount": 3200,
    "lastHeartbeatAt": "2026-06-01T08:00:00Z"
  },
  "latestHealth": {
    "heartRate": 72,
    "systolicPressure": 120,
    "diastolicPressure": 80,
    "bodyTemperature": 36.5,
    "measuredAt": "2026-06-01T07:55:00Z"
  },
  "latestLocation": {
    "latitude": 22.570720,
    "longitude": 113.862017,
    "gpsValid": true,
    "locatedAt": "2026-06-01T07:58:00Z"
  }
}
```

若设备不存在返回 404。`latestHealth` 和 `latestLocation` 若无数据返回 null。

#### GET /api/miniapp/devices/{deviceNo}/health-records?size=20

返回最近 N 条健康记录，默认 20 条，最大 100 条。  
用于健康趋势图。

#### GET /api/miniapp/devices/{deviceNo}/location-records?size=20

返回最近 N 条位置记录，默认 20 条，最大 100 条。  
用于地图轨迹回放。只返回 `gpsValid=true` 的记录。

---

## 四、微信小程序（家属端）

### 技术选型

- 原生微信小程序（WXML + WXSS + JS）
- 腾讯地图 SDK（`@map-component/miniprogram-map`）
- wx.request 调用后端 API

### 目录结构

```
miniapp/
├── app.js
├── app.json
├── app.wxss
├── pages/
│   ├── index/          # 首页：输入设备编号
│   ├── overview/       # 设备概览
│   ├── health/         # 健康数据 + 趋势图
│   └── location/       # 地图 + 轨迹
└── utils/
    └── api.js          # 封装所有 HTTP 请求
```

### 页面设计

#### pages/index — 首页

- 输入框：输入设备编号（deviceNo）
- 「查看」按钮：跳转到 overview 页，携带 deviceNo 参数
- 本地缓存最近查看的设备编号（wx.setStorageSync）

#### pages/overview — 设备概览

调用 `/overview` 接口，展示：
- 设备状态（在线/离线，绿/灰色标签）
- 电量（进度条）
- 今日步数
- 最新心率、血压、体温（卡片形式）
- 最后心跳时间
- 底部 Tab：「健康」「位置」

#### pages/health — 健康数据

- 顶部：最新一条健康数据的数值卡片
- 中部：折线图（使用 wx-charts 或 echarts-for-weixin）
  - 心率趋势（最近 20 条）
  - 血压趋势（收缩压 + 舒张压双线）
  - 体温趋势
- 底部：历史记录列表（时间 + 数值）

#### pages/location — 位置与轨迹

- 腾讯地图组件，居中显示最新位置（marker）
- 轨迹线：将最近 20 条有效位置连成 polyline
- 底部信息栏：最后定位时间、GPS 是否有效、卫星数、信号强度

### API 配置

`utils/api.js` 中统一配置 BASE_URL，方便切换开发/生产环境：
```js
const BASE_URL = 'http://your-server:8080'
```

---

## 五、Vue3 管理后台

### 技术选型

- Vue 3 + Vite
- Element Plus（UI 组件库）
- Vue Router 4
- Axios（HTTP 客户端，统一配置 Basic Auth）
- 腾讯地图 JavaScript API（轨迹地图）

### 目录结构

```
web/
├── index.html
├── vite.config.js
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── router/index.js
│   ├── api/
│   │   ├── axios.js        # Axios 实例，配置 Basic Auth
│   │   ├── devices.js
│   │   └── packets.js
│   ├── views/
│   │   ├── DeviceList.vue      # 设备列表
│   │   ├── DeviceDetail.vue    # 设备详情
│   │   └── RawPackets.vue      # 原始报文
│   └── components/
│       ├── LocationMap.vue     # 腾讯地图轨迹组件（复用）
│       └── HealthChart.vue     # 健康趋势图（可选）
```

### 页面设计

#### DeviceList — 设备列表

- Element Plus Table，分页
- 列：设备编号、型号、状态（Tag）、电量、步数、最后心跳时间、操作
- 操作列：「详情」按钮，跳转 DeviceDetail
- 状态 Tag：ONLINE=绿色，OFFLINE=灰色

#### DeviceDetail — 设备详情

路由：`/devices/:id`

分 Tab 展示：
1. **基本信息** — 设备字段
2. **健康记录** — 表格，最近 100 条，列：时间、心率、血压、体温
3. **位置记录** — 上方腾讯地图（polyline 轨迹 + 最新位置 marker），下方表格（最近 100 条）
4. **发送指令** — 输入框 + 发送按钮，调用 `POST /api/admin/devices/{id}/send-command`

#### RawPackets — 原始报文

- 分页表格
- 列：时间、设备编号、方向、命令、解析状态、内容
- 解析状态 Tag：OK=绿色，ERROR=红色

### 认证

Axios 实例统一设置 Authorization header（Basic Auth），凭证从环境变量或本地配置读取。

---

## 六、开发顺序

1. 后端：新增 MiniApp API + 放行 `/api/miniapp/**` 安全配置
2. Web 管理后台：设备列表 → 设备详情（含地图轨迹）→ 原始报文
3. 微信小程序：首页 → 概览 → 健康 → 位置地图

---

## 七、范围外（本次不做）

- 用户注册/登录（小程序账号系统）
- 实时推送（WebSocket / SSE）
- 报警通知
- 地理围栏
