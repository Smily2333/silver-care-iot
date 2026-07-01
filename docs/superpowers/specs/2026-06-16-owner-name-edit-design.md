# 老人姓名修改功能设计

日期：2026-06-16

## 背景

`Device` 实体上已有 `ownerName` 字段，绑定设备时可以设置，但绑定后无法单独修改。监护人在小程序中没有办法更正或更新老人的姓名。Web 管理端目前也没有展示该字段。

## 范围

- 后端：新增修改 ownerName 的小程序 API 接口
- 小程序：overview 页顶部展示老人姓名，支持就地编辑
- Web 端：设备列表和设备详情展示老人姓名（只读）

## 后端

### 新接口

```
PATCH /api/miniapp/devices/{deviceNo}/owner-name
```

请求体：
```json
{ "ownerName": "张奶奶" }
```

- `ownerName` 非空，长度 1–64 字符
- 设备不存在返回 404
- 成功返回更新后的 Device 对象（200）

实现位置：`MiniappDeviceController`，新增内部 record `UpdateOwnerNameRequest`，复用已有的 `DeviceRepository`。无需新建 Service 方法，逻辑简单，直接在 controller 内完成。

## 小程序

### overview 页顶部

当前显示 `deviceNo`，改为优先显示 `ownerName`，无名字时 fallback 到 `deviceNo`。

```
张奶奶  ✏️        [在线]
智能健康手表
最后同步：2026-06-16 10:23
```

### 编辑交互

点击 ✏️ 后，名字文本切换为输入框，右侧显示确认按钮（✓）：

```
[张奶奶        ] ✓
```

- 输入框预填当前名字
- 点击确认：调用 API，成功后切回展示态并更新显示
- 输入为空时禁用确认按钮
- 保存中显示 loading 状态，防止重复提交
- 失败时 `wx.showToast` 提示错误，不清空输入内容

### api.js 新增方法

```js
export function updateOwnerName(deviceNo, ownerName) {
  return requestPatch(`/api/miniapp/devices/${deviceNo}/owner-name`, { ownerName })
}
```

`api.js` 中补充 `requestPatch` 方法（与现有 `request` GET 方法对称）。

## Web 端（只读展示）

### DeviceList

在"设备编号"列后插入"老人姓名"列，无值显示 `-`。

### DeviceDetail

基本信息 `el-descriptions` 中，"设备编号"行后插入"老人姓名"行，无值显示 `-`。

## 不在范围内

- Web 端编辑老人姓名（管理员无需此操作，由监护人在小程序修改）
- 姓名历史记录
- 多设备绑定同一老人
