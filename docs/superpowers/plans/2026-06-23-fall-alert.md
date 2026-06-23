# 跌倒警报功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增跌倒警报功能——后端识别 `AL` 包写入独立告警表，小程序在概览页即时提醒监护人并提供告警历史列表页。

**Architecture:** 新增 `FallAlert` 实体和对应 Repository/Service；`LocationDataService.saveLocation()` 改为返回 `LocationRecord`；`DevicePacketDispatcher` 在处理 `AL` 包时同时写位置记录和告警记录；小程序新增告警列表页，概览页 `onShow` 轮询最新告警并弹窗提醒。

**Tech Stack:** Spring Boot 3.3 / JPA (Hibernate) / MySQL / JUnit 5 + Mockito + AssertJ / 微信小程序原生框架

---

## 文件清单

### 新增
| 文件 | 职责 |
|------|------|
| `backend/src/main/java/com/silvercare/iot/domain/entity/FallAlert.java` | JPA 实体，映射 `fall_alerts` 表 |
| `backend/src/main/java/com/silvercare/iot/repository/FallAlertRepository.java` | 数据访问，查最新/分页 |
| `backend/src/main/java/com/silvercare/iot/service/FallAlertService.java` | 解析 AL 包并保存告警 |
| `backend/src/main/java/com/silvercare/iot/api/MiniappAlertController.java` | REST 接口 `/fall-alerts` 和 `/fall-alerts/latest` |
| `backend/src/test/java/com/silvercare/iot/service/FallAlertServiceTest.java` | FallAlertService 单元测试 |
| `backend/src/test/java/com/silvercare/iot/service/DevicePacketDispatcherAlertTest.java` | Dispatcher 处理 AL 包集成测试 |
| `backend/src/test/java/com/silvercare/iot/api/MiniappAlertControllerTest.java` | Controller 单元测试 |
| `miniapp/pages/alerts/alerts.js` | 告警列表页逻辑 |
| `miniapp/pages/alerts/alerts.wxml` | 告警列表页模板 |
| `miniapp/pages/alerts/alerts.wxss` | 告警列表页样式 |
| `miniapp/pages/alerts/alerts.json` | 告警列表页配置 |

### 修改
| 文件 | 改动 |
|------|------|
| `backend/src/main/java/com/silvercare/iot/service/LocationDataService.java` | `saveLocation()` 返回值 `void` → `LocationRecord` |
| `backend/src/main/java/com/silvercare/iot/service/DevicePacketDispatcher.java` | 注入 `FallAlertService`；AL 分支调用 saveAlert |
| `backend/src/test/java/com/silvercare/iot/service/LocationDataServiceTest.java` | 断言返回值非 null |
| `miniapp/utils/api.js` | 新增 `getFallAlerts`、`getLatestFallAlert` |
| `miniapp/app.json` | 注册 `pages/alerts/alerts` |
| `miniapp/pages/overview/overview.js` | `onShow` 轮询最新告警，有新告警弹 modal |
| `miniapp/pages/overview/overview.wxml` | 有未确认告警时显示红色横幅 |
| `miniapp/pages/overview/overview.wxss` | 横幅样式 |

---

## Task 1: FallAlert 实体

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/domain/entity/FallAlert.java`

- [ ] **Step 1: 创建 FallAlert 实体**

```java
package com.silvercare.iot.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fall_alerts", indexes = {
        @Index(name = "idx_fall_alert_device_time", columnList = "deviceId,alertedAt")
})
public class FallAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deviceId;

    @Column(precision = 11, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 7)
    private BigDecimal longitude;

    private Boolean gpsValid;
    private Long locationRecordId;
    private Long rawPacketId;

    @Column(nullable = false)
    private Instant alertedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getDeviceId() { return deviceId; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public Boolean getGpsValid() { return gpsValid; }
    public Long getLocationRecordId() { return locationRecordId; }
    public Long getRawPacketId() { return rawPacketId; }
    public Instant getAlertedAt() { return alertedAt; }

    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setGpsValid(Boolean gpsValid) { this.gpsValid = gpsValid; }
    public void setLocationRecordId(Long locationRecordId) { this.locationRecordId = locationRecordId; }
    public void setRawPacketId(Long rawPacketId) { this.rawPacketId = rawPacketId; }
    public void setAlertedAt(Instant alertedAt) { this.alertedAt = alertedAt; }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

期望：无编译错误。

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/silvercare/iot/domain/entity/FallAlert.java
git commit -m "feat: add FallAlert entity"
```

---

## Task 2: FallAlertRepository

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/repository/FallAlertRepository.java`

- [ ] **Step 1: 创建 Repository**

```java
package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.FallAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FallAlertRepository extends JpaRepository<FallAlert, Long> {

    Optional<FallAlert> findFirstByDeviceIdOrderByAlertedAtDesc(Long deviceId);

    List<FallAlert> findTop20ByDeviceIdOrderByAlertedAtDesc(Long deviceId);
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

期望：无编译错误。

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/silvercare/iot/repository/FallAlertRepository.java
git commit -m "feat: add FallAlertRepository"
```

---

## Task 3: LocationDataService 改为返回 LocationRecord

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/service/LocationDataService.java`
- Modify: `backend/src/test/java/com/silvercare/iot/service/LocationDataServiceTest.java`

- [ ] **Step 1: 先更新测试，断言返回值**

在 `LocationDataServiceTest.java` 中，将测试方法改为：

```java
@Test
void savesKnownLocationFields() {
    LocationRecordRepository repository = mock(LocationRecordRepository.class);
    LocationDataService service = new LocationDataService(repository);
    ProtocolParser parser = new ProtocolParser();
    Device device = new Device();
    device.setDeviceNo("2016001000");

    // mock repository.save() to return the record it receives
    when(repository.save(any(LocationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

    LocationRecord result = service.saveLocation(
            device,
            parser.parse("[3G*2016001000*00E0*UD,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010,6,255,460,0,9360,5081,156,9360,4081,129,9360,4151,128,9360,5082,127,9360,4723,122,9360,4082,120,5,buyaoxialian,a0:c5:f2:b0:7.4:d0,-34,22.4]"),
            1L
    );

    assertThat(result).isNotNull();
    assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("22.570720"));
    assertThat(result.getLongitude()).isEqualByComparingTo(new BigDecimal("113.8620167"));
    assertThat(result.getLocatedAt()).isEqualTo(LocalDate.of(2018, 1, 12)
            .atTime(LocalTime.of(7, 6, 25))
            .atZone(ZoneOffset.UTC)
            .toInstant());
}
```

同时在文件顶部补充 import：
```java
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
```

- [ ] **Step 2: 运行测试，确认失败（返回类型不匹配）**

```bash
cd backend && mvn test -pl . -Dtest=LocationDataServiceTest -q 2>&1 | tail -20
```

期望：编译错误 `incompatible types: void cannot be converted to LocationRecord`。

- [ ] **Step 3: 修改 LocationDataService.saveLocation() 返回 LocationRecord**

将 `saveLocation` 方法签名和返回值改为：

```java
public LocationRecord saveLocation(Device device, ProtocolFrame frame, Long rawPacketId) {
    String[] args = frame.content().split(",");
    LocationRecord record = new LocationRecord();
    record.setDeviceId(device.getId());
    record.setSourceCommand(frame.command());
    record.setRawPacketId(rawPacketId);
    Instant locatedAt = parseLocatedAt(args);
    if (locatedAt != null) {
        record.setLocatedAt(locatedAt);
    }
    record.setGpsValid("A".equalsIgnoreCase(value(args, 3)));
    record.setLatitude(parseDecimal(args, 4));
    record.setLatitudeHemisphere(value(args, 5));
    record.setLongitude(parseDecimal(args, 6));
    record.setLongitudeHemisphere(value(args, 7));
    record.setSpeed(parseDecimal(args, 8));
    record.setDirection(parseDecimal(args, 9));
    record.setAltitude(parseDecimal(args, 10));
    record.setSatelliteCount(parseInt(args, 11));
    record.setGsmSignal(parseInt(args, 12));
    record.setBatteryLevel(parseInt(args, 13));
    record.setStepCount(parseInt(args, 14));
    record.setRolloverCount(parseInt(args, 15));
    record.setTerminalStatus(value(args, 16));
    record.setAccuracy(parseLastDecimal(args));
    return repository.save(record);
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd backend && mvn test -pl . -Dtest=LocationDataServiceTest -q 2>&1 | tail -5
```

期望：`BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/silvercare/iot/service/LocationDataService.java \
        backend/src/test/java/com/silvercare/iot/service/LocationDataServiceTest.java
git commit -m "refactor: LocationDataService.saveLocation returns LocationRecord"
```

---

## Task 4: FallAlertService

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/service/FallAlertService.java`
- Create: `backend/src/test/java/com/silvercare/iot/service/FallAlertServiceTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.protocol.ProtocolParser;
import com.silvercare.iot.repository.FallAlertRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FallAlertServiceTest {

    private final FallAlertRepository repository = mock(FallAlertRepository.class);
    private final FallAlertService service = new FallAlertService(repository);
    private final ProtocolParser parser = new ProtocolParser();

    @Test
    void saveAlert_parsesLocationAndTime() {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        LocationRecord locationRecord = new LocationRecord();
        locationRecord.setDeviceId(1L);

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*00B4*AL,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                locationRecord,
                99L
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        FallAlert alert = captor.getValue();

        assertThat(alert.getLatitude()).isEqualByComparingTo(new BigDecimal("22.570720"));
        assertThat(alert.getLongitude()).isEqualByComparingTo(new BigDecimal("113.8620167"));
        assertThat(alert.getGpsValid()).isTrue();
        assertThat(alert.getRawPacketId()).isEqualTo(99L);
        assertThat(alert.getAlertedAt()).isEqualTo(
                LocalDate.of(2018, 1, 12).atTime(LocalTime.of(7, 6, 25))
                        .atZone(ZoneOffset.UTC).toInstant());
    }

    @Test
    void saveAlert_gpsInvalid_setsGpsValidFalse() {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        service.saveAlert(
                device,
                parser.parse("[3G*2016001000*00B4*AL,120118,070625,V,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]"),
                null,
                1L
        );

        ArgumentCaptor<FallAlert> captor = ArgumentCaptor.forClass(FallAlert.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGpsValid()).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd backend && mvn test -pl . -Dtest=FallAlertServiceTest -q 2>&1 | tail -10
```

期望：编译错误 `cannot find symbol: class FallAlertService`。

- [ ] **Step 3: 实现 FallAlertService**

```java
package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.protocol.ProtocolFrame;
import com.silvercare.iot.repository.FallAlertRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

@Service
public class FallAlertService {

    private static final DateTimeFormatter DATE_FMT = new DateTimeFormatterBuilder()
            .appendPattern("ddMM")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private final FallAlertRepository repository;

    public FallAlertService(FallAlertRepository repository) {
        this.repository = repository;
    }

    public void saveAlert(Device device, ProtocolFrame frame, LocationRecord locationRecord, Long rawPacketId) {
        String[] args = frame.content().split(",");
        FallAlert alert = new FallAlert();
        alert.setDeviceId(device.getId());
        alert.setRawPacketId(rawPacketId);
        alert.setGpsValid("A".equalsIgnoreCase(value(args, 3)));
        alert.setLatitude(parseDecimal(args, 4));
        alert.setLongitude(parseDecimal(args, 6));
        if (locationRecord != null) {
            alert.setLocationRecordId(locationRecord.getId());
        }
        Instant alertedAt = parseAlertedAt(args);
        alert.setAlertedAt(alertedAt != null ? alertedAt : Instant.now());
        // TODO: trigger wx subscribe message push
        repository.save(alert);
    }

    private Instant parseAlertedAt(String[] args) {
        try {
            String date = value(args, 1);
            String time = value(args, 2);
            if (date == null || time == null || date.isBlank() || time.isBlank()) return null;
            return LocalDate.parse(date, DATE_FMT)
                    .atTime(LocalTime.parse(time, TIME_FMT))
                    .atZone(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String value(String[] args, int index) {
        return index < args.length ? args[index].trim() : null;
    }

    private BigDecimal parseDecimal(String[] args, int index) {
        try {
            String v = value(args, index);
            return v == null || v.isBlank() ? null : new BigDecimal(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd backend && mvn test -pl . -Dtest=FallAlertServiceTest -q 2>&1 | tail -5
```

期望：`BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/silvercare/iot/service/FallAlertService.java \
        backend/src/test/java/com/silvercare/iot/service/FallAlertServiceTest.java
git commit -m "feat: add FallAlertService"
```

---

## Task 5: DevicePacketDispatcher 接入 FallAlertService

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/service/DevicePacketDispatcher.java`
- Create: `backend/src/test/java/com/silvercare/iot/service/DevicePacketDispatcherAlertTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.silvercare.iot.service;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.domain.entity.RawPacketLog;
import com.silvercare.iot.repository.FallAlertRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import com.silvercare.iot.tcp.DeviceConnection;
import com.silvercare.iot.tcp.DeviceConnectionRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DevicePacketDispatcherAlertTest {

    private final DeviceConnectionRegistry registry = mock(DeviceConnectionRegistry.class);
    private final DeviceService deviceService = mock(DeviceService.class);
    private final RawPacketLogService rawPacketLogService = mock(RawPacketLogService.class);
    private final HealthDataService healthDataService = mock(HealthDataService.class);
    private final LocationDataService locationDataService = mock(LocationDataService.class);
    private final FallAlertService fallAlertService = mock(FallAlertService.class);
    private final DeviceConnection connection = mock(DeviceConnection.class);

    private DevicePacketDispatcher dispatcher() {
        return new DevicePacketDispatcher(registry, deviceService, rawPacketLogService,
                healthDataService, locationDataService, fallAlertService);
    }

    @Test
    void alPacket_savesLocationAndAlert_andSendsAck() throws Exception {
        Device device = new Device();
        device.setDeviceNo("2016001000");

        RawPacketLog packetLog = new RawPacketLog();
        // set id via reflection since there's no setter
        var idField = RawPacketLog.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(packetLog, 7L);

        LocationRecord locationRecord = new LocationRecord();

        when(deviceService.ensureOnline(any())).thenReturn(device);
        when(rawPacketLogService.saveSuccess(any())).thenReturn(packetLog);
        when(locationDataService.saveLocation(any(), any(), any())).thenReturn(locationRecord);

        dispatcher().dispatch(
                "[3G*2016001000*00B4*AL,120118,070625,A,22.570720,N,113.8620167,E,0.00,188.6,0.0,9,100,51,14188,0,00000010]",
                connection
        );

        verify(locationDataService).saveLocation(eq(device), any(), eq(7L));
        verify(fallAlertService).saveAlert(eq(device), any(), eq(locationRecord), eq(7L));
        verify(connection).send(contains("AL"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd backend && mvn test -pl . -Dtest=DevicePacketDispatcherAlertTest -q 2>&1 | tail -15
```

期望：编译错误——`DevicePacketDispatcher` 构造器参数数量不匹配。

- [ ] **Step 3: 修改 DevicePacketDispatcher**

注入 `FallAlertService`，修改构造器和 `AL` 分支：

```java
@Service
public class DevicePacketDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DevicePacketDispatcher.class);

    private final ProtocolParser parser = new ProtocolParser();
    private final DeviceConnectionRegistry registry;
    private final DeviceService deviceService;
    private final RawPacketLogService rawPacketLogService;
    private final HealthDataService healthDataService;
    private final LocationDataService locationDataService;
    private final FallAlertService fallAlertService;

    public DevicePacketDispatcher(DeviceConnectionRegistry registry,
                                  DeviceService deviceService,
                                  RawPacketLogService rawPacketLogService,
                                  HealthDataService healthDataService,
                                  LocationDataService locationDataService,
                                  FallAlertService fallAlertService) {
        this.registry = registry;
        this.deviceService = deviceService;
        this.rawPacketLogService = rawPacketLogService;
        this.healthDataService = healthDataService;
        this.locationDataService = locationDataService;
        this.fallAlertService = fallAlertService;
    }

    public void dispatch(String rawPacket, DeviceConnection connection) {
        try {
            ProtocolFrame frame = parser.parse(rawPacket);
            connection.setDeviceNo(frame.deviceNo());
            registry.register(frame.deviceNo(), connection);
            Device device = deviceService.ensureOnline(frame.deviceNo());
            RawPacketLog packetLog = rawPacketLogService.saveSuccess(frame);

            switch (frame.command()) {
                case "LK" -> handleHeartbeat(frame, connection);
                case "btemp2" -> {
                    healthDataService.saveTemperature(device, frame, packetLog.getId());
                    sendAck(frame, connection);
                }
                case "bphrt" -> {
                    healthDataService.saveBloodPressureHeartRate(device, frame, packetLog.getId());
                    sendAck(frame, connection);
                }
                case "UD", "UD2", "UD_LTE", "UD_WCDMA", "UD_TDSCDMA", "UD_CDMA" ->
                        locationDataService.saveLocation(device, frame, packetLog.getId());
                case "AL", "AL_LTE", "AL_WCDMA", "AL_TDSCDMA", "AL_CDMA" -> {
                    LocationRecord locationRecord = locationDataService.saveLocation(device, frame, packetLog.getId());
                    fallAlertService.saveAlert(device, frame, locationRecord, packetLog.getId());
                    sendAck(frame, connection);
                }
                case "TKQ" -> sendAck(frame, connection);
                case "CR", "UPLOAD" ->
                        log.debug("Device {} acknowledged {}", frame.deviceNo(), frame.command());
                default -> log.info("Packet command ignored for MVP: {}", frame.command());
            }
        } catch (ProtocolParseException ex) {
            rawPacketLogService.saveFailure(rawPacket, ex.getMessage());
            log.warn("Failed to parse device packet: {}", ex.getMessage());
        }
    }

    public void onConnectionClosed(DeviceConnection connection) {
        if (connection.getDeviceNo() != null) {
            deviceService.markOffline(connection.getDeviceNo());
        }
    }

    private void sendAck(ProtocolFrame frame, DeviceConnection connection) {
        String reply = parser.build(frame.vendor(), frame.deviceNo(), frame.command());
        try {
            connection.send(reply);
        } catch (IOException e) {
            log.warn("Failed to send {} reply to device {}", frame.command(), frame.deviceNo(), e);
        }
    }

    private void handleHeartbeat(ProtocolFrame frame, DeviceConnection connection) {
        String[] args = frame.content().split(",");
        Integer steps = parseInt(args, 1);
        Integer battery = parseInt(args, 3);
        deviceService.updateHeartbeat(frame.deviceNo(), steps, battery);
        String reply = parser.build(frame.vendor(), frame.deviceNo(), "LK");
        try {
            connection.send(reply);
        } catch (IOException ex) {
            log.warn("Failed to reply heartbeat to device {}", frame.deviceNo(), ex);
        }
    }

    private Integer parseInt(String[] args, int index) {
        if (index >= args.length) return null;
        try {
            return Integer.valueOf(args[index].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
```

需要在文件顶部补充 import：
```java
import com.silvercare.iot.domain.entity.LocationRecord;
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd backend && mvn test -pl . -Dtest=DevicePacketDispatcherAlertTest -q 2>&1 | tail -5
```

期望：`BUILD SUCCESS`。

- [ ] **Step 5: 运行全部测试，确认无回归**

```bash
cd backend && mvn test -q 2>&1 | tail -10
```

期望：`BUILD SUCCESS`，所有测试通过。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/silvercare/iot/service/DevicePacketDispatcher.java \
        backend/src/test/java/com/silvercare/iot/service/DevicePacketDispatcherAlertTest.java
git commit -m "feat: dispatcher writes fall alert on AL packet"
```

---

## Task 6: MiniappAlertController（REST 接口）

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/api/MiniappAlertController.java`
- Create: `backend/src/test/java/com/silvercare/iot/api/MiniappAlertControllerTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.FallAlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MiniappAlertControllerTest {

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final FallAlertRepository fallAlertRepository = mock(FallAlertRepository.class);

    private MiniappAlertController controller() {
        return new MiniappAlertController(deviceRepository, fallAlertRepository);
    }

    @Test
    void list_returnsAlertsForDevice() {
        Device device = new Device();
        device.setDeviceNo("DEV001");

        FallAlert alert = new FallAlert();
        alert.setDeviceId(1L);
        alert.setAlertedAt(Instant.now());

        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(List.of(alert));

        List<FallAlert> result = controller().list("DEV001", 20);

        assertThat(result).hasSize(1);
    }

    @Test
    void list_deviceNotFound_throws404() {
        when(deviceRepository.findByDeviceNo("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller().list("NOTEXIST", 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void latest_noAlerts_returns204() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(Optional.empty());

        var response = controller().latest("DEV001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void latest_hasAlert_returns200() {
        Device device = new Device();
        device.setDeviceNo("DEV001");
        FallAlert alert = new FallAlert();
        alert.setAlertedAt(Instant.now());

        when(deviceRepository.findByDeviceNo("DEV001")).thenReturn(Optional.of(device));
        when(fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId()))
                .thenReturn(Optional.of(alert));

        var response = controller().latest("DEV001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(alert);
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd backend && mvn test -pl . -Dtest=MiniappAlertControllerTest -q 2>&1 | tail -10
```

期望：编译错误 `cannot find symbol: class MiniappAlertController`。

- [ ] **Step 3: 实现 MiniappAlertController**

```java
package com.silvercare.iot.api;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.FallAlert;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.FallAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/miniapp/devices")
public class MiniappAlertController {

    private final DeviceRepository deviceRepository;
    private final FallAlertRepository fallAlertRepository;

    public MiniappAlertController(DeviceRepository deviceRepository,
                                  FallAlertRepository fallAlertRepository) {
        this.deviceRepository = deviceRepository;
        this.fallAlertRepository = fallAlertRepository;
    }

    @GetMapping("/{deviceNo}/fall-alerts")
    public List<FallAlert> list(@PathVariable String deviceNo,
                                @RequestParam(defaultValue = "20") int size) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        int clamped = Math.min(size, 20);
        return fallAlertRepository.findTop20ByDeviceIdOrderByAlertedAtDesc(device.getId())
                .stream().limit(clamped).toList();
    }

    @GetMapping("/{deviceNo}/fall-alerts/latest")
    public ResponseEntity<FallAlert> latest(@PathVariable String deviceNo) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return fallAlertRepository.findFirstByDeviceIdOrderByAlertedAtDesc(device.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd backend && mvn test -pl . -Dtest=MiniappAlertControllerTest -q 2>&1 | tail -5
```

期望：`BUILD SUCCESS`。

- [ ] **Step 5: 运行全部测试，确认无回归**

```bash
cd backend && mvn test -q 2>&1 | tail -10
```

期望：`BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/silvercare/iot/api/MiniappAlertController.java \
        backend/src/test/java/com/silvercare/iot/api/MiniappAlertControllerTest.java
git commit -m "feat: add fall-alerts REST endpoints"
```

---

## Task 7: 小程序 API 层 + 路由注册

**Files:**
- Modify: `miniapp/utils/api.js`
- Modify: `miniapp/app.json`

- [ ] **Step 1: 在 api.js 末尾新增两个函数**

在 `miniapp/utils/api.js` 末尾追加：

```js
export function getFallAlerts(deviceNo, size = 20) {
  return request(`/api/miniapp/devices/${deviceNo}/fall-alerts`, { size })
}

export function getLatestFallAlert(deviceNo) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + `/api/miniapp/devices/${deviceNo}/fall-alerts/latest`,
      method: 'GET',
      header: { 'Content-Type': 'application/json' },
      success: res => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 204) {
          resolve(null)
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

- [ ] **Step 2: 注册告警页路由**

在 `miniapp/app.json` 的 `pages` 数组中追加新页面：

```json
{
  "pages": [
    "pages/index/index",
    "pages/overview/overview",
    "pages/health/health",
    "pages/location/location",
    "pages/alerts/alerts"
  ],
  "window": {
    "backgroundTextStyle": "light",
    "navigationBarBackgroundColor": "#304156",
    "navigationBarTitleText": "Silver Care",
    "navigationBarTextStyle": "white"
  },
  "sitemapLocation": "sitemap.json"
}
```

- [ ] **Step 3: 提交**

```bash
git add miniapp/utils/api.js miniapp/app.json
git commit -m "feat: add fall-alerts api helpers and register alerts route"
```

---

## Task 8: 告警列表页

**Files:**
- Create: `miniapp/pages/alerts/alerts.json`
- Create: `miniapp/pages/alerts/alerts.js`
- Create: `miniapp/pages/alerts/alerts.wxml`
- Create: `miniapp/pages/alerts/alerts.wxss`

- [ ] **Step 1: 创建 alerts.json**

```json
{
  "enablePullDownRefresh": true,
  "backgroundColor": "#f5f6fa"
}
```

- [ ] **Step 2: 创建 alerts.js**

```js
const { getFallAlerts } = require('../../utils/api')

Page({
  data: {
    deviceNo: '',
    loading: true,
    alerts: []
  },

  onLoad(options) {
    const deviceNo = options.deviceNo || ''
    this.setData({ deviceNo })
    wx.setNavigationBarTitle({ title: '跌倒警报' })
    this.load(deviceNo)
  },

  onPullDownRefresh() {
    this.load(this.data.deviceNo).finally(() => wx.stopPullDownRefresh())
  },

  load(deviceNo) {
    this.setData({ loading: true })
    return getFallAlerts(deviceNo, 20)
      .then(list => {
        const formatted = list.map(a => ({
          ...a,
          alertedAtStr: this.formatTime(a.alertedAt)
        }))
        this.setData({ alerts: formatted })
      })
      .catch(err => {
        wx.showToast({ title: err.message, icon: 'none' })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  goLocation(e) {
    const { lat, lng } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/location/location?deviceNo=${this.data.deviceNo}&lat=${lat}&lng=${lng}`
    })
  },

  formatTime(isoStr) {
    const d = new Date(isoStr)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
```

- [ ] **Step 3: 创建 alerts.wxml**

```xml
<!--跌倒警报列表页-->
<view class="page">
  <view wx:if="{{loading}}" class="loading-wrap">
    <text class="loading-icon">⏳</text>
    <text>加载中...</text>
  </view>

  <view wx:else>
    <view class="page-header">
      <text class="header-title">跌倒警报</text>
      <text class="header-sub">共 {{alerts.length}} 条记录</text>
    </view>

    <view class="content-area">
      <view wx:if="{{alerts.length === 0}}" class="card empty-card">
        <text class="empty-icon">✅</text>
        <text class="empty-text">暂无告警记录</text>
      </view>

      <view
        wx:for="{{alerts}}"
        wx:key="id"
        class="card alert-card"
      >
        <view class="alert-header-row">
          <view class="alert-badge">⚠️ 跌倒警报</view>
          <text class="alert-time">{{item.alertedAtStr}}</text>
        </view>
        <view wx:if="{{item.gpsValid && item.latitude}}" class="alert-location-row">
          <text class="location-label">📍 位置</text>
          <text class="location-coords">{{item.latitude}}, {{item.longitude}}</text>
          <text
            class="location-link"
            bindtap="goLocation"
            data-lat="{{item.latitude}}"
            data-lng="{{item.longitude}}"
          >查看地图</text>
        </view>
        <view wx:else class="alert-location-row">
          <text class="location-label no-gps">📍 位置信息不可用</text>
        </view>
      </view>
    </view>
  </view>
</view>
```

- [ ] **Step 4: 创建 alerts.wxss**

```css
.page { background: #f5f6fa; min-height: 100vh; }

.loading-wrap { display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: 120rpx 0; color: #999; }
.loading-icon { font-size: 60rpx; margin-bottom: 16rpx; }

.page-header { background: linear-gradient(135deg, #c0392b 0%, #e74c3c 100%);
  padding: 48rpx 40rpx 40rpx; }
.header-title { display: block; font-size: 44rpx; font-weight: 700;
  color: #fff; margin-bottom: 8rpx; }
.header-sub { display: block; font-size: 26rpx; color: rgba(255,255,255,0.8); }

.content-area { padding: 24rpx 28rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 32rpx;
  margin-bottom: 20rpx; box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06); }

.empty-card { display: flex; flex-direction: column; align-items: center;
  padding: 80rpx 0; }
.empty-icon { font-size: 72rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: #aaa; }

.alert-header-row { display: flex; align-items: center;
  justify-content: space-between; margin-bottom: 20rpx; }
.alert-badge { background: #fef0f0; color: #e74c3c; font-size: 26rpx;
  font-weight: 600; padding: 8rpx 20rpx; border-radius: 20rpx; }
.alert-time { font-size: 24rpx; color: #999; }

.alert-location-row { display: flex; align-items: center; gap: 12rpx; flex-wrap: wrap; }
.location-label { font-size: 26rpx; color: #666; }
.location-coords { font-size: 24rpx; color: #333; flex: 1; }
.location-link { font-size: 26rpx; color: #3498db; text-decoration: underline; }
.no-gps { color: #bbb; }
```

- [ ] **Step 5: 提交**

```bash
git add miniapp/pages/alerts/
git commit -m "feat: add fall alerts list page"
```

---

## Task 9: 概览页告警横幅与 onShow 轮询

**Files:**
- Modify: `miniapp/pages/overview/overview.js`
- Modify: `miniapp/pages/overview/overview.wxml`
- Modify: `miniapp/pages/overview/overview.wxss`

- [ ] **Step 1: 修改 overview.js**

在文件顶部修改 require 行，增加 `getLatestFallAlert`：

```js
const { getOverview, updateOwnerName, getLatestFallAlert } = require('../../utils/api')
```

在 `data` 对象中增加 `hasNewAlert: false`：

```js
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
  saving: false,
  hasNewAlert: false
},
```

在 `onLoad` 之后新增 `onShow` 方法：

```js
onShow() {
  const deviceNo = this.data.deviceNo
  if (!deviceNo) return
  getLatestFallAlert(deviceNo)
    .then(alert => {
      if (!alert) return
      const lastSeen = wx.getStorageSync('lastSeenAlertAt_' + deviceNo) || ''
      if (alert.alertedAt !== lastSeen) {
        this.setData({ hasNewAlert: true })
        wx.showModal({
          title: '⚠️ 跌倒警报',
          content: `检测到跌倒事件\n时间：${this.formatTime(alert.alertedAt)}`,
          confirmText: '查看详情',
          cancelText: '知道了',
          success: res => {
            wx.setStorageSync('lastSeenAlertAt_' + deviceNo, alert.alertedAt)
            this.setData({ hasNewAlert: false })
            if (res.confirm) {
              wx.navigateTo({ url: `/pages/alerts/alerts?deviceNo=${deviceNo}` })
            }
          }
        })
      }
    })
    .catch(() => {})
},
```

在 `goHealth` 方法之前新增 `goAlerts` 方法：

```js
goAlerts() {
  const deviceNo = this.data.deviceNo
  wx.setStorageSync('lastSeenAlertAt_' + deviceNo,
    wx.getStorageSync('lastSeenAlertAt_' + deviceNo) || '')
  this.setData({ hasNewAlert: false })
  wx.navigateTo({ url: `/pages/alerts/alerts?deviceNo=${deviceNo}` })
},
```

- [ ] **Step 2: 修改 overview.wxml，在 page-header 前插入告警横幅**

在 `<view class="page" wx:if="{{!loading}}">` 下方、`<view class="page-header">` 之前插入：

```xml
<!-- 告警横幅 -->
<view wx:if="{{hasNewAlert}}" class="alert-banner" bindtap="goAlerts">
  <text class="alert-banner-icon">⚠️</text>
  <text class="alert-banner-text">检测到跌倒警报，点击查看详情</text>
  <text class="alert-banner-arrow">›</text>
</view>
```

同时在导航入口 nav-grid 中新增告警入口，放在「健康数据」和「位置轨迹」之后：

```xml
<view class="nav-card" bindtap="goAlerts">
  <view class="nav-card-icon alert-nav-icon">
    <text>⚠️</text>
  </view>
  <text class="nav-card-label">跌倒警报</text>
  <text class="nav-card-sub">查看警报记录</text>
</view>
```

- [ ] **Step 3: 修改 overview.wxss，追加横幅样式**

在文件末尾追加：

```css
.alert-banner { display: flex; align-items: center; background: #e74c3c;
  padding: 24rpx 32rpx; gap: 16rpx; }
.alert-banner-icon { font-size: 36rpx; }
.alert-banner-text { flex: 1; font-size: 28rpx; color: #fff; font-weight: 500; }
.alert-banner-arrow { font-size: 36rpx; color: rgba(255,255,255,0.8); }

.alert-nav-icon { background: linear-gradient(135deg, #e74c3c, #c0392b); }
```

- [ ] **Step 4: 提交**

```bash
git add miniapp/pages/overview/overview.js \
        miniapp/pages/overview/overview.wxml \
        miniapp/pages/overview/overview.wxss
git commit -m "feat: overview page shows fall alert banner and polls on show"
```

---

## Task 10: 后端构建验证 & 推送

**Files:** 无新增，验证现有所有改动整体可编译

- [ ] **Step 1: 完整构建并运行所有测试**

```bash
cd backend && mvn clean test 2>&1 | tail -20
```

期望：`BUILD SUCCESS`，所有测试通过，无失败。

- [ ] **Step 2: 推送到远程**

```bash
git push
```

---
