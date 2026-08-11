# 老人健康监测与 Web 快捷控制实施计划

**日期：** 2026-08-11
**依据：** `docs/superpowers/specs/2026-08-11-elder-health-monitoring-web-controls-design.md`
**状态：** 待实施

---

## 1. 实施原则

- 先建立可追踪的设备动作，再增加 Web 按钮；
- 快捷按钮调用类型化后端 API，不在前端拼接报文；
- 先验证佩戴状态 Bit，再实现健康测量门禁；
- 立即动作必须区分 `SENT`、`ACKNOWLEDGED` 和 `COMPLETED`；
- 健康测量和位置结果以新入库记录作为完成依据；
- 手写命令继续保留，但移动到高级工具；
- 跌倒功能保留，并修正所有 `AL` 被当作跌倒的问题；
- 每一阶段都先补测试，再修改实现。

---

## 2. 里程碑

| 里程碑 | 目标 | 可交付结果 |
|---|---|---|
| M0 协议真机确认 | 确认关键状态位和单次测量行为 | 真实报文样本和确认表 |
| M1 健康数据正确性 | 修复最新指标和异常值展示 | 可信健康概览 |
| M2 设备动作基础 | 建立动作任务、ACK 和结果关联 | 后端类型化动作 API |
| M3 Web 快捷控制 | 提供友好按钮和执行进度 | 可用的立即定位/健康检测 |
| M4 佩戴状态与定时监测 | 仅在在线佩戴时测量 | 监测策略和调度任务 |
| M5 跌倒闭环 | 正确分类、去重和处置 | 可确认和关闭的跌倒事件 |
| M6 联调上线 | 真机、后端、Web、小程序整体回归 | 上线检查清单 |

---

## 3. M0：协议真机确认

### Task 0.1：建立测试样本

**不修改业务逻辑前完成。**

采集以下原始报文并保存到脱敏测试夹具：

- 已佩戴且静止的 `UD`；
- 已佩戴且走动的 `UD`；
- 取下前后的 `UD`；
- 取下产生的 `AL`；
- 真正跌倒测试产生的 `AL`；
- `CR` 命令 ACK 及其后的新位置；
- `hrtstart,1` ACK 及其后的 `bphrt`；
- `bodytemp2` ACK 及其后的 `btemp2`；
- 测量失败或未佩戴时的健康上报。

输出：

- `docs/protocol-verification.md`；
- 测试资源中的脱敏报文夹具；
- 已确认的 Bit 掩码、命令顺序和典型耗时。

### Task 0.2：确认产品参数

确认：

- 默认位置间隔；
- 默认心率间隔；
- 默认体温计划；
- 佩戴状态过期时间；
- 动作超时时间；
- 跌倒通知渠道；
- 是否开放组合“立即健康检测”。

---

## 4. M1：健康数据正确性

### Task 1.1：健康测量状态

**Files:**

- Modify: `backend/src/main/java/com/silvercare/iot/domain/entity/HealthRecord.java`
- Modify: `backend/src/main/java/com/silvercare/iot/service/HealthDataService.java`
- Create: `backend/src/main/java/com/silvercare/iot/domain/HealthMeasurementStatus.java`
- Create: `backend/src/test/java/com/silvercare/iot/service/HealthDataServiceTest.java`
- Create: `deploy/mysql/2026-08-11-health-monitoring.sql`

实现：

- 体温 `0/1` 转换为异常状态；
- 血压、心率 `0` 转换为无效状态或空有效值；
- 非数字、缺字段、超合理显示范围的数据保留原始报文引用并标记无效；
- 不静默将异常数据展示为正常测量值。

### Task 1.2：健康概览聚合

**Files:**

- Modify: `backend/src/main/java/com/silvercare/iot/repository/HealthRecordRepository.java`
- Create: `backend/src/main/java/com/silvercare/iot/api/dto/AdminHealthSummaryResponse.java`
- Create: `backend/src/main/java/com/silvercare/iot/service/HealthSummaryService.java`
- Modify: `backend/src/main/java/com/silvercare/iot/api/AdminDeviceController.java`
- Modify: `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java`
- Create: relevant backend tests

实现：

- 分别查询最新有效心率、血压和体温；
- 每项返回独立测量时间和新鲜度；
- 不再使用同一条 `HealthRecord` 代表全部指标。

### Task 1.3：前端展示适配

**Files:**

- Modify: `web/src/api/devices.js`
- Modify: `web/src/views/DeviceDetail.vue`
- Modify: `miniapp/pages/overview/overview.js`
- Modify: `miniapp/pages/overview/overview.wxml`
- Modify: `miniapp/pages/health/health.js`
- Modify: `miniapp/pages/health/health.wxml`

实现：

- 展示每项指标的独立更新时间；
- 显示有效、异常、无效、过期状态；
- 保留日常照护免责声明。

---

## 5. M2：设备动作基础

### Task 2.1：动作数据模型

**Files:**

- Create: `backend/src/main/java/com/silvercare/iot/domain/DeviceActionType.java`
- Create: `backend/src/main/java/com/silvercare/iot/domain/DeviceActionStatus.java`
- Create: `backend/src/main/java/com/silvercare/iot/domain/entity/DeviceAction.java`
- Create: `backend/src/main/java/com/silvercare/iot/repository/DeviceActionRepository.java`
- Modify: `deploy/mysql/2026-08-11-health-monitoring.sql`

要求：

- 记录动作请求、发送、ACK、完成和失败时间；
- 保存协议命令名和实际发送内容；
- 支持关联位置或健康结果记录；
- 同设备同命令限制一个进行中动作。

### Task 2.2：动作服务与命令白名单

**Files:**

- Create: `backend/src/main/java/com/silvercare/iot/service/DeviceActionService.java`
- Create: `backend/src/main/java/com/silvercare/iot/service/DeviceCommandCatalog.java`
- Create: `backend/src/test/java/com/silvercare/iot/service/DeviceActionServiceTest.java`

首批动作：

- `LOCATE_NOW -> CR`
- `MEASURE_HEART_RATE -> hrtstart,1`
- `MEASURE_TEMPERATURE -> bodytemp2`

要求：

- 使用 `ProtocolParser.build()` 生成完整报文；
- 设备无活动连接时返回明确状态；
- 心率、体温动作执行佩戴门禁；
- TCP 写入成功只标记 `SENT`。

### Task 2.3：类型化 Admin API

**Files:**

- Create: `backend/src/main/java/com/silvercare/iot/api/AdminDeviceActionController.java`
- Create: `backend/src/main/java/com/silvercare/iot/api/dto/DeviceActionResponse.java`
- Create: `backend/src/test/java/com/silvercare/iot/api/AdminDeviceActionControllerTest.java`
- Modify: `docs/api/admin-api.md`

接口：

- `POST /api/admin/devices/{id}/actions`
- `GET /api/admin/devices/{id}/actions/{actionId}`
- `GET /api/admin/devices/{id}/actions?size=20`

### Task 2.4：ACK 和结果关联

**Files:**

- Modify: `backend/src/main/java/com/silvercare/iot/service/DevicePacketDispatcher.java`
- Modify: `backend/src/main/java/com/silvercare/iot/service/HealthDataService.java`
- Modify: `backend/src/main/java/com/silvercare/iot/service/LocationDataService.java`
- Create/Modify: dispatcher and service tests

实现：

- `CR`、`hrtstart`、`bodytemp2` ACK 更新动作状态；
- 新位置完成 `LOCATE_NOW`；
- 新有效心率完成 `MEASURE_HEART_RATE`；
- 新体温结果完成 `MEASURE_TEMPERATURE`；
- ACK 丢失但结果到达时允许动作完成并记录该事实；
- 到期任务进入对应超时状态。

---

## 6. M3：Web 快捷控制

### Task 3.1：Web API

**Files:**

- Modify: `web/src/api/devices.js`
- Create: `web/src/api/deviceActions.js`（如保持 API 按领域拆分）

增加：

- `createDeviceAction(id, type)`；
- `getDeviceAction(id, actionId)`；
- `listDeviceActions(id)`；
- `getHealthSummary(id)`。

### Task 3.2：快捷操作组件

**Files:**

- Create: `web/src/components/device/DeviceQuickActions.vue`
- Create: `web/src/components/device/DeviceActionProgress.vue`
- Modify: `web/src/views/DeviceDetail.vue`
- Create: relevant Vitest component tests

按钮：

- 立即定位；
- 测量心率；
- 测量体温；
- 立即健康检测（完成真机串行验证后启用）；
- 刷新数据。

交互：

- 离线时禁用设备动作；
- 未佩戴或佩戴未知时禁用健康动作；
- 防止重复点击；
- 轮询动作状态直到完成或超时；
- 完成后刷新相应 Tab；
- 展示明确的失败原因。

### Task 3.3：高级手写命令

**Files:**

- Create: `web/src/components/device/AdvancedCommandPanel.vue`
- Modify: `web/src/views/DeviceDetail.vue`

实现：

- 将现有手写输入框移动到折叠区域；
- 增加风险提示和二次确认；
- 保留完整报文回显；
- 明确标识“只确认发送，不跟踪执行结果”。

---

## 7. M4：佩戴状态与定时监测

### Task 4.1：佩戴状态解析

**Files:**

- Create: `backend/src/main/java/com/silvercare/iot/domain/WearStatus.java`
- Create: `backend/src/main/java/com/silvercare/iot/protocol/TerminalStatusDecoder.java`
- Modify: `backend/src/main/java/com/silvercare/iot/domain/entity/Device.java`
- Modify: `backend/src/main/java/com/silvercare/iot/service/LocationDataService.java`
- Modify: `deploy/mysql/2026-08-11-health-monitoring.sql`
- Create: `backend/src/test/java/com/silvercare/iot/protocol/TerminalStatusDecoderTest.java`

要求：

- 只使用 M0 真机确认后的掩码；
- 保存原始状态值；
- 实现状态去抖和过期转 `UNKNOWN`；
- 不用缓存佩戴状态覆盖报文自身报警信息。

### Task 4.2：监测策略

**Files:**

- Create: `backend/src/main/java/com/silvercare/iot/domain/entity/DeviceMonitoringPolicy.java`
- Create: `backend/src/main/java/com/silvercare/iot/repository/DeviceMonitoringPolicyRepository.java`
- Create: `backend/src/main/java/com/silvercare/iot/service/DeviceMonitoringPolicyService.java`
- Create: `backend/src/main/java/com/silvercare/iot/api/AdminMonitoringPolicyController.java`
- Create: relevant tests
- Modify: `deploy/mysql/2026-08-11-health-monitoring.sql`

接口：

- `GET /api/admin/devices/{id}/monitoring-policy`
- `PUT /api/admin/devices/{id}/monitoring-policy`

### Task 4.3：调度器

**Files:**

- Create: `backend/src/main/java/com/silvercare/iot/service/HealthMonitoringScheduler.java`
- Create: `backend/src/test/java/com/silvercare/iot/service/HealthMonitoringSchedulerTest.java`
- Modify: `backend/src/main/resources/application.yml`

要求：

- 定期扫描到期策略；
- 在线且佩戴时创建单次健康动作；
- 离线、未佩戴、状态未知时记录跳过原因；
- 同类动作未完成时不重复创建；
- 位置间隔通过 `UPLOAD` 应用到设备；
- 设备重连后重试未应用策略。

### Task 4.4：Web 策略配置

**Files:**

- Create: `web/src/components/device/MonitoringPolicyForm.vue`
- Modify: `web/src/views/DeviceDetail.vue`
- Modify: Web API files
- Create: component tests

页面提供：

- 定时监测总开关；
- 位置间隔；
- 心率间隔；
- 体温间隔或固定时间；
- 跌倒开关和灵敏度；
- 期望配置、已应用配置和最近错误。

---

## 8. M5：跌倒闭环

### Task 5.1：报警位分类

**Files:**

- Modify: `backend/src/main/java/com/silvercare/iot/service/DevicePacketDispatcher.java`
- Modify: `backend/src/main/java/com/silvercare/iot/service/FallAlertService.java`
- Create: alarm classification tests

实现：

- 解析 `terminalStatus`；
- 只有跌倒位有效时创建疑似跌倒事件；
- 其他报警分类记录；
- 未知状态保留原始报文并标记待核验。

### Task 5.2：去重与处理状态

**Files:**

- Modify: `backend/src/main/java/com/silvercare/iot/domain/entity/FallAlert.java`
- Modify: `backend/src/main/java/com/silvercare/iot/repository/FallAlertRepository.java`
- Modify: `backend/src/main/java/com/silvercare/iot/api/MiniappAlertController.java`
- Create: Admin alert API if required
- Modify: database migration and tests

实现：

- 时间窗口和设备状态组成去重键；
- 支持 `PENDING`、`CONFIRMED`、`RESOLVED`、`FALSE_ALARM`；
- 保存处理人、时间和备注；
- 小程序本地 `lastSeen` 只作为 UI 辅助，服务端状态为准。

### Task 5.3：通知

接入微信订阅消息或短信服务：

- 按绑定关系查找监护人；
- 记录每次发送和失败；
- 推送失败不影响设备 ACK 和告警入库；
- 定义重试上限，避免重复轰炸。

---

## 9. M6：联调、测试与上线

### 后端测试

- 协议长度和命令构建；
- 状态位解析；
- 在线/佩戴门禁；
- 动作并发限制；
- ACK 和结果关联；
- 无 ACK 有结果；
- 有 ACK 无结果；
- 健康异常值；
- 定时任务跳过和恢复；
- 跌倒分类、去重和状态流转；
- 设备绑定和管理权限。

### Web 测试

- 按钮启用/禁用条件；
- 动作状态轮询；
- 超时和错误提示；
- 完成后刷新；
- 高级命令二次确认；
- 监测策略表单校验。

### 真机验收

1. 在线佩戴时立即定位成功并刷新地图；
2. 在线佩戴时心率和体温产生新记录；
3. 取下后健康按钮禁用，定时任务跳过；
4. 重新佩戴后恢复监测；
5. 离线时不发送命令并提示原因；
6. ACK 丢失、结果超时能够正确显示；
7. 跌倒与取下、低电等其他 `AL` 不混淆；
8. 重复跌倒报文不会产生无限重复事件；
9. Web 和小程序显示的最新指标一致；
10. 页面明确展示数据时间和日常照护免责声明。

### 构建验证

```powershell
Set-Location backend
mvn test

Set-Location ..\web
npm test
npm run build
```

---

## 10. 推荐实施顺序

建议按以下顺序执行，避免先做按钮、后补底层闭环：

1. M0 真机协议验证；
2. M1 健康数据正确性；
3. M2 设备动作基础；
4. M3 Web 快捷控制；
5. M4 佩戴状态与定时监测；
6. M5 跌倒闭环；
7. M6 联调上线。

如果需要先快速交付一个可演示版本，可以在 M2 完成后先开放“立即定位、测量心率、测量体温”三个按钮，但必须保留动作状态，并将佩戴未知时的健康测量设置为管理员确认后执行，不能直接显示为成功。
