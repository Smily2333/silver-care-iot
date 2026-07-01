# Owner Name Edit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让监护人在小程序中可以修改老人姓名，并在 Web 管理端展示该姓名。

**Architecture:** 后端新增 `PATCH /api/miniapp/devices/{deviceNo}/owner-name` 接口；小程序 overview 页顶部显示老人姓名并支持就地编辑；Web 端设备列表和详情页只读展示姓名。

**Tech Stack:** Spring Boot 3 / Jakarta Validation（后端）、微信小程序原生框架（小程序）、Vue 3 + Element Plus（Web）

---

## 文件清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java` | 修改 | 新增 `UpdateOwnerNameRequest` record 和 `updateOwnerName` 方法 |
| `backend/src/test/java/com/silvercare/iot/api/MiniappUpdateOwnerNameTest.java` | 新建 | 新接口的单元测试 |
| `miniapp/utils/api.js` | 修改 | 新增 `requestPatch` 和 `updateOwnerName` |
| `miniapp/pages/overview/overview.js` | 修改 | 新增编辑态数据和处理逻辑 |
| `miniapp/pages/overview/overview.wxml` | 修改 | 顶部改为显示 ownerName，加编辑 UI |
| `miniapp/pages/overview/overview.wxss` | 修改 | 编辑态样式 |
| `web/src/views/DeviceList.vue` | 修改 | 插入老人姓名列 |
| `web/src/views/DeviceDetail.vue` | 修改 | 插入老人姓名行 |

---

## Task 1：后端接口 — 测试

**Files:**
- Create: `backend/src/test/java/com/silvercare/iot/api/MiniappUpdateOwnerNameTest.java`

- [ ] **Step 1: 新建测试文件**

```java
package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MiniappUpdateOwnerNameTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private MiniappDeviceController controller() {
        return new MiniappDeviceController(deviceRepository, null, null);
    }

    @Test
    void updateOwnerName_savesNewName() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setOwnerName("旧名字");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().updateOwnerName(
                "DEV001",
                new MiniappDeviceController.UpdateOwnerNameRequest("新名字"));

        assertThat(result.getOwnerName()).isEqualTo("新名字");
        verify(deviceRepository).save(device);
    }

    @Test
    void updateOwnerName_deviceNotFound_throws404() {
        when(deviceRepository.findByDeviceNo("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                controller().updateOwnerName(
                        "NOTEXIST",
                        new MiniappDeviceController.UpdateOwnerNameRequest("张奶奶")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败（方法未定义）**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test -pl . -Dtest=MiniappUpdateOwnerNameTest -q 2>&1 | tail -20
```

预期：编译错误，`cannot find symbol: updateOwnerName`

---

## Task 2：后端接口 — 实现

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java`

- [ ] **Step 1: 在 `MiniappDeviceController` 末尾添加 record 和方法**

在文件末尾 `}` 之前，紧接 `bind` 方法后插入：

```java
    public record UpdateOwnerNameRequest(
            @NotBlank @jakarta.validation.constraints.Size(max = 64) String ownerName
    ) {}

    @org.springframework.web.bind.annotation.PatchMapping("/{deviceNo}/owner-name")
    public Device updateOwnerName(@PathVariable String deviceNo,
                                  @Valid @RequestBody UpdateOwnerNameRequest req) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        device.setOwnerName(req.ownerName());
        return deviceRepository.save(device);
    }
```

需要在文件顶部 import 列表中补充（检查是否已存在，没有则加）：
```java
import org.springframework.web.bind.annotation.PatchMapping;
import jakarta.validation.constraints.Size;
```

- [ ] **Step 2: 运行测试，确认通过**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test -pl . -Dtest=MiniappUpdateOwnerNameTest -q 2>&1 | tail -10
```

预期：`BUILD SUCCESS`

- [ ] **Step 3: 运行全量测试，确认无回归**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test -q 2>&1 | tail -10
```

预期：`BUILD SUCCESS`

- [ ] **Step 4: 重新打包并重启服务**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn package -DskipTests -q && sudo systemctl restart silver-care-backend
sleep 10 && sudo systemctl status silver-care-backend | grep Active
```

预期：`Active: active (running)`

- [ ] **Step 5: 冒烟测试接口**

```bash
curl -s -X PATCH http://localhost:8080/api/miniapp/devices/DEV001/owner-name \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"张奶奶"}' | python3 -m json.tool
```

预期：返回 device 对象，`ownerName` 字段为 `"张奶奶"`（若 DEV001 不存在则返回 404，属正常）

- [ ] **Step 6: Commit**

```bash
cd /home/ubuntu/www/silver-care-iot/backend
git add src/main/java/com/silvercare/iot/api/MiniappDeviceController.java \
        src/test/java/com/silvercare/iot/api/MiniappUpdateOwnerNameTest.java
git commit -m "feat: add PATCH owner-name endpoint for miniapp"
```

---

## Task 3：小程序 api.js — 新增 requestPatch 和 updateOwnerName

**Files:**
- Modify: `miniapp/utils/api.js`

- [ ] **Step 1: 在 `api.js` 中添加 `requestPatch` 函数和 `updateOwnerName` 导出**

在现有 `request` 函数之后、第一个 `export function` 之前插入：

```js
function requestPatch(path, data) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + path,
      data: data || {},
      method: 'PATCH',
      header: { 'Content-Type': 'application/json' },
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 404) {
          reject(new Error('设备不存在'))
        } else {
          reject(new Error('请求失败：' + res.statusCode))
        }
      },
      fail: err => reject(new Error(err.errMsg || '网络错误'))
    })
  })
}
```

在文件末尾追加：

```js
export function updateOwnerName(deviceNo, ownerName) {
  return requestPatch(`/api/miniapp/devices/${deviceNo}/owner-name`, { ownerName })
}
```

- [ ] **Step 2: Commit**

```bash
cd /home/ubuntu/www/silver-care-iot
git add miniapp/utils/api.js
git commit -m "feat(miniapp): add updateOwnerName api helper"
```

---

## Task 4：小程序 overview — 逻辑层

**Files:**
- Modify: `miniapp/pages/overview/overview.js`

- [ ] **Step 1: 更新 `overview.js`**

将整个文件替换为以下内容：

```js
const { getOverview, updateOwnerName } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    data: { device: {}, latestHealth: null, latestLocation: null },
    lastHeartbeat: '-',
    measuredAt: '-',
    locatedAt: '-',
    displayName: '',
    editing: false,
    editValue: '',
    saving: false
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
    wx.setNavigationBarTitle({ title: deviceNo })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true })
    return getOverview(deviceNo)
      .then(res => {
        const displayName = res.device.ownerName || deviceNo
        this.setData({
          data: res,
          displayName,
          lastHeartbeat: res.device.lastHeartbeatAt ? this.formatTime(res.device.lastHeartbeatAt) : '-',
          measuredAt: res.latestHealth?.measuredAt ? this.formatTime(res.latestHealth.measuredAt) : '-',
          locatedAt: res.latestLocation?.locatedAt ? this.formatTime(res.latestLocation.locatedAt) : '-'
        })
      })
      .catch(err => {
        wx.showToast({ title: err.message, icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  startEdit() {
    this.setData({ editing: true, editValue: this.data.data.device.ownerName || '' })
  },

  onEditInput(e) {
    this.setData({ editValue: e.detail.value })
  },

  saveOwnerName() {
    const name = this.data.editValue.trim()
    if (!name || this.data.saving) return
    this.setData({ saving: true })
    updateOwnerName(this.data.deviceNo, name)
      .then(updatedDevice => {
        const newData = { ...this.data.data, device: updatedDevice }
        this.setData({
          data: newData,
          displayName: updatedDevice.ownerName || this.data.deviceNo,
          editing: false,
          saving: false
        })
        wx.showToast({ title: '保存成功', icon: 'success' })
      })
      .catch(err => {
        this.setData({ saving: false })
        wx.showToast({ title: err.message, icon: 'none' })
      })
  },

  goHealth() {
    wx.navigateTo({ url: `/pages/health/health?deviceNo=${this.data.deviceNo}` })
  },

  goLocation() {
    wx.navigateTo({ url: `/pages/location/location?deviceNo=${this.data.deviceNo}` })
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
```

- [ ] **Step 2: Commit**

```bash
cd /home/ubuntu/www/silver-care-iot
git add miniapp/pages/overview/overview.js
git commit -m "feat(miniapp): add owner name edit logic to overview page"
```

---

## Task 5：小程序 overview — 模板层

**Files:**
- Modify: `miniapp/pages/overview/overview.wxml`
- Modify: `miniapp/pages/overview/overview.wxss`

- [ ] **Step 1: 替换 wxml 中的 `.device-name-row` 块**

将 overview.wxml 中的这段：

```xml
      <view class="device-name-row">
        <text class="device-name">{{deviceNo}}</text>
        <text class="{{data.device.status === 'ONLINE' ? 'tag-online' : 'tag-offline'}}">
          {{data.device.status === 'ONLINE' ? '在线' : '离线'}}
        </text>
      </view>
```

替换为：

```xml
      <view class="device-name-row">
        <block wx:if="{{!editing}}">
          <text class="device-name">{{displayName}}</text>
          <text class="edit-icon" bindtap="startEdit">✏️</text>
        </block>
        <block wx:else>
          <input class="name-input" value="{{editValue}}" bindinput="onEditInput"
                 placeholder="输入老人姓名" maxlength="64" focus />
          <text class="{{editValue.trim() && !saving ? 'confirm-btn' : 'confirm-btn confirm-btn-disabled'}}"
                bindtap="saveOwnerName">✓</text>
        </block>
        <text class="{{data.device.status === 'ONLINE' ? 'tag-online' : 'tag-offline'}}">
          {{data.device.status === 'ONLINE' ? '在线' : '离线'}}
        </text>
      </view>
```

- [ ] **Step 2: 在 overview.wxss 末尾追加编辑态样式**

```css
/* 名字编辑态 */
.edit-icon {
  font-size: 28rpx;
  opacity: 0.7;
  padding: 4rpx 8rpx;
}

.name-input {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
  border-bottom: 2rpx solid rgba(255,255,255,0.6);
  padding-bottom: 4rpx;
  min-width: 200rpx;
  max-width: 340rpx;
}

.confirm-btn {
  font-size: 36rpx;
  color: #fff;
  padding: 4rpx 12rpx;
  background: rgba(52,199,89,0.5);
  border-radius: 12rpx;
  margin-left: 12rpx;
}

.confirm-btn-disabled {
  opacity: 0.35;
}
```

- [ ] **Step 3: Commit**

```bash
cd /home/ubuntu/www/silver-care-iot
git add miniapp/pages/overview/overview.wxml miniapp/pages/overview/overview.wxss
git commit -m "feat(miniapp): show owner name with inline edit on overview page"
```

---

## Task 6：Web 端展示老人姓名

**Files:**
- Modify: `web/src/views/DeviceList.vue`
- Modify: `web/src/views/DeviceDetail.vue`

- [ ] **Step 1: DeviceList.vue — 在"设备编号"列后插入"老人姓名"列**

在 `DeviceList.vue` 中，`<el-table-column prop="deviceNo" label="设备编号" width="160" />` 之后插入：

```html
      <el-table-column label="老人姓名" width="120">
        <template #default="{ row }">
          {{ row.ownerName || '-' }}
        </template>
      </el-table-column>
```

- [ ] **Step 2: DeviceDetail.vue — 在"设备编号"行后插入"老人姓名"行**

在 `DeviceDetail.vue` 中，`<el-descriptions-item label="设备编号">{{ device?.deviceNo }}</el-descriptions-item>` 之后插入：

```html
          <el-descriptions-item label="老人姓名">{{ device?.ownerName || '-' }}</el-descriptions-item>
```

- [ ] **Step 3: 构建 Web 并确认无错误**

```bash
cd /home/ubuntu/www/silver-care-iot/web && npm run build 2>&1 | tail -15
```

预期：`✓ built in` 无 error

- [ ] **Step 4: Commit**

```bash
cd /home/ubuntu/www/silver-care-iot
git add web/src/views/DeviceList.vue web/src/views/DeviceDetail.vue
git commit -m "feat(web): display ownerName in device list and detail"
```

---

## 自检清单

- [x] 后端：`PATCH /{deviceNo}/owner-name` 接口，非空 + 长度校验，404 处理
- [x] 后端：单元测试覆盖正常路径和 404
- [x] 小程序：overview 顶部显示 `ownerName`，fallback 到 `deviceNo`
- [x] 小程序：点击 ✏️ 切换编辑态，空值禁用确认，保存中防重复提交，失败不清空输入
- [x] 小程序：`api.js` 新增 `requestPatch` 和 `updateOwnerName`
- [x] Web：`DeviceList` 插入老人姓名列
- [x] Web：`DeviceDetail` 插入老人姓名行
