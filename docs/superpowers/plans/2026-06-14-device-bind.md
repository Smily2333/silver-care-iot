# 设备绑定与改名实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Device 实体加 ownerName 字段，新增小程序绑定接口和管理后台改名接口。

**Architecture:** Device 实体加字段，JPA 自动建列；新增两个 Controller 方法，复用现有 DeviceRepository；无新 Service 层（逻辑简单，直接在 Controller 操作）。

**Tech Stack:** Spring Boot 3.3.5, Spring Data JPA, MySQL 8.0, JUnit 5, Mockito

---

## 文件变更清单

- Modify: `backend/src/main/java/com/silvercare/iot/domain/entity/Device.java`
- Modify: `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java`
- Modify: `backend/src/main/java/com/silvercare/iot/api/AdminDeviceController.java`
- Create: `backend/src/test/java/com/silvercare/iot/api/MiniappBindTest.java`
- Create: `backend/src/test/java/com/silvercare/iot/api/AdminDevicePatchTest.java`

---

### Task 1: Device 实体加 ownerName 字段

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/domain/entity/Device.java`

- [ ] **Step 1: 在 Device 类的字段区加入 ownerName**

在 `private Instant createdAt;` 之前插入：

```java
@Column(length = 64)
private String ownerName;
```

- [ ] **Step 2: 加 getter 和 setter**

在现有 getter/setter 区域末尾添加：

```java
public String getOwnerName() {
    return ownerName;
}

public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
}
```

- [ ] **Step 3: 运行现有测试，确认没有破坏**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test 2>&1 | tail -10
```

预期：`BUILD SUCCESS`，4 tests passed。

- [ ] **Step 4: 提交**

```bash
cd /home/ubuntu/www/silver-care-iot/backend
git add src/main/java/com/silvercare/iot/domain/entity/Device.java
git commit -m "feat: add ownerName field to Device entity"
```

---

### Task 2: 小程序绑定接口

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java`
- Create: `backend/src/test/java/com/silvercare/iot/api/MiniappBindTest.java`

- [ ] **Step 1: 写失败测试**

新建 `backend/src/test/java/com/silvercare/iot/api/MiniappBindTest.java`：

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

class MiniappBindTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private MiniappDeviceController controller() {
        return new MiniappDeviceController(deviceRepository, null, null);
    }

    @Test
    void bind_setsOwnerNameAndReturnsDevice() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().bind(new MiniappDeviceController.BindRequest("DEV001", "张奶奶"));

        assertThat(result.getOwnerName()).isEqualTo("张奶奶");
        verify(deviceRepository).save(device);
    }

    @Test
    void bind_deviceNotFound_throws404() {
        when(deviceRepository.findByDeviceNo("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                controller().bind(new MiniappDeviceController.BindRequest("NOTEXIST", "张奶奶")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void bind_overwritesExistingOwnerName() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        device.setOwnerName("旧姓名");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().bind(new MiniappDeviceController.BindRequest("DEV001", "新姓名"));

        assertThat(result.getOwnerName()).isEqualTo("新姓名");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败（编译失败即可）**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test -Dtest=MiniappBindTest 2>&1 | tail -15
```

预期：编译错误，`bind` 方法和 `BindRequest` 不存在。

- [ ] **Step 3: 在 MiniappDeviceController 加 BindRequest record 和 bind 方法**

在类顶部 import 区补充：

```java
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
```

在类末尾（最后一个方法之后，类的 `}` 之前）添加 record 和方法：

```java
public record BindRequest(
        @NotBlank String deviceNo,
        @NotBlank String ownerName
) {}

@PostMapping("/bind")
public Device bind(@Valid @RequestBody BindRequest req) {
    Device device = deviceRepository.findByDeviceNo(req.deviceNo())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
    device.setOwnerName(req.ownerName());
    return deviceRepository.save(device);
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test -Dtest=MiniappBindTest 2>&1 | tail -10
```

预期：`Tests run: 3, Failures: 0, Errors: 0`。

- [ ] **Step 5: 提交**

```bash
cd /home/ubuntu/www/silver-care-iot/backend
git add src/main/java/com/silvercare/iot/api/MiniappDeviceController.java \
        src/test/java/com/silvercare/iot/api/MiniappBindTest.java
git commit -m "feat: add miniapp bind endpoint (POST /api/miniapp/devices/bind)"
```

---

### Task 3: 管理后台改名接口

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/api/AdminDeviceController.java`
- Create: `backend/src/test/java/com/silvercare/iot/api/AdminDevicePatchTest.java`

- [ ] **Step 1: 写失败测试**

新建 `backend/src/test/java/com/silvercare/iot/api/AdminDevicePatchTest.java`：

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

class AdminDevicePatchTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);

    private AdminDeviceController controller() {
        return new AdminDeviceController(deviceRepository, null, null, null);
    }

    @Test
    void patch_setsOwnerName() {
        Device device = new Device();
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        Device result = controller().patch(1L, new AdminDeviceController.PatchRequest("李爷爷"));

        assertThat(result.getOwnerName()).isEqualTo("李爷爷");
        verify(deviceRepository).save(device);
    }

    @Test
    void patch_deviceNotFound_throws404() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                controller().patch(99L, new AdminDeviceController.PatchRequest("李爷爷")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void patch_emptyOwnerName_throws400() {
        assertThatThrownBy(() ->
                controller().patch(1L, new AdminDeviceController.PatchRequest("  ")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test -Dtest=AdminDevicePatchTest 2>&1 | tail -15
```

预期：编译错误，`patch` 方法和 `PatchRequest` 不存在。

- [ ] **Step 3: 在 AdminDeviceController 加 PatchRequest record 和 patch 方法**

在类顶部 import 区补充：

```java
import org.springframework.web.bind.annotation.PatchMapping;
```

在类末尾（最后一个方法之后，类的 `}` 之前）添加：

```java
public record PatchRequest(String ownerName) {}

@PatchMapping("/{id}")
public Device patch(@PathVariable Long id, @RequestBody PatchRequest req) {
    if (req.ownerName() == null || req.ownerName().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerName must not be blank");
    }
    Device device = deviceRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
    device.setOwnerName(req.ownerName());
    return deviceRepository.save(device);
}
```

- [ ] **Step 4: 运行全部测试，确认通过**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test 2>&1 | tail -10
```

预期：`Tests run: 10, Failures: 0, Errors: 0`（原有 4 个 + 新增 6 个）。

- [ ] **Step 5: 提交**

```bash
cd /home/ubuntu/www/silver-care-iot/backend
git add src/main/java/com/silvercare/iot/api/AdminDeviceController.java \
        src/test/java/com/silvercare/iot/api/AdminDevicePatchTest.java
git commit -m "feat: add admin PATCH /devices/{id} for ownerName update"
```

---

### Task 4: 验证数据库列自动创建

**Files:**
- 无代码改动，仅验证

- [ ] **Step 1: 后台启动应用**

```bash
cd /home/ubuntu/www/silver-care-iot/backend
mvn spring-boot:run > /tmp/app-startup.log 2>&1 &
sleep 20 && grep -E "(Started|ERROR|owner_name|alter)" /tmp/app-startup.log | head -10
```

预期：`Started SilverCareIotApplication`，无 ERROR。

- [ ] **Step 2: 验证 MySQL 中 owner_name 列已创建**

```bash
docker exec kangyang-mysql mysql -uroot -e "USE silver_care; DESCRIBE devices;"
```

预期：列表中包含 `owner_name | varchar(64) | YES | | NULL |`。

- [ ] **Step 3: 停止应用，推送**

```bash
pkill -f "spring-boot:run"; sleep 2
cd /home/ubuntu/www/silver-care-iot
git push origin main
```
