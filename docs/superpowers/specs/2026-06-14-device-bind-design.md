# 设备绑定与改名设计

> **Superseded:** 当前实现已使用微信登录用户和 `device_bindings` 绑定表。现行行为见
> `docs/api/miniapp-auth.md`，请勿恢复下文“无用户体系”的旧方案。

**日期：** 2026-06-14
**项目：** silver-care-iot / backend
**阶段：** C（无用户体系，ownerName 直接存设备上）

---

## 背景

目前设备由 IoT 硬件自动注册，没有"主人姓名"概念。家属需要通过小程序扫码或输入编号完成绑定，同时设置老人姓名，方便在 overview 页展示。管理后台也需要能改名。

后续扩展路径：阶段 B 引入用户表和绑定关系表时，`ownerName` 可迁移到绑定关系上，`Device` 实体本身不需要大改。

---

## 数据库变更

`devices` 表新增一列：

```sql
ALTER TABLE devices ADD COLUMN owner_name VARCHAR(64) NULL;
```

JPA `ddl-auto: update` 会自动执行，无需手动迁移。

---

## 代码变更

### 1. `Device` 实体

新增字段：

```java
@Column(length = 64)
private String ownerName;
// getter + setter
```

### 2. 小程序绑定接口

`POST /api/miniapp/devices/bind`

Request body：
```json
{ "deviceNo": "2016001000", "ownerName": "张奶奶" }
```

- `deviceNo` 不存在 → 404
- 成功 → 200，返回更新后的 `Device`
- 重复绑定 → 覆盖 `ownerName`（C 阶段不做冲突检测）

### 3. 管理后台改名接口

`PATCH /api/admin/devices/{id}`

Request body：
```json
{ "ownerName": "张奶奶" }
```

- `id` 不存在 → 404
- `ownerName` 为空字符串 → 400
- 成功 → 200，返回更新后的 `Device`

### 4. overview 响应

`MiniappOverviewResponse` 已直接返回 `Device` 对象，`ownerName` 字段加到实体后自动透出，无需改 DTO。

---

## 接口汇总

| 方法 | 路径 | 调用方 | 说明 |
|------|------|--------|------|
| POST | `/api/miniapp/devices/bind` | 小程序 | 扫码/输入编号绑定，设置姓名 |
| PATCH | `/api/admin/devices/{id}` | 管理后台 | 改名 |

---

## 约束

- `ownerName` 允许为 null（未绑定状态）
- 最大长度 64 字符
- C 阶段不验证设备是否已被绑定，直接覆盖
