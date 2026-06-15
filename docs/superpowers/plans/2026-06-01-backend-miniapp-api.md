# Backend MiniApp API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/api/miniapp/**` REST endpoints so the WeChat miniapp can query device overview, health records, and location records by deviceNo without authentication.

**Architecture:** New `MiniappSecurityConfig` permits `/api/miniapp/**` without auth. New `MiniappDeviceController` handles three endpoints. A `MiniappOverviewResponse` DTO assembles device + latest health + latest location in one response. Repositories already exist; only new query methods are needed.

**Tech Stack:** Spring Boot 3.3, Spring Security, Spring Data JPA, H2 (dev) / MySQL (prod)

---

## File Map

| Action | File |
|--------|------|
| Create | `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java` |
| Create | `backend/src/main/java/com/silvercare/iot/api/dto/MiniappOverviewResponse.java` |
| Create | `backend/src/main/java/com/silvercare/iot/config/MiniappSecurityConfig.java` |
| Modify | `backend/src/main/java/com/silvercare/iot/repository/LocationRecordRepository.java` |
| Create | `backend/src/test/java/com/silvercare/iot/api/MiniappDeviceControllerTest.java` |

---

## Task 1: Add gpsValid filter query to LocationRecordRepository

**Files:**
- Modify: `backend/src/main/java/com/silvercare/iot/repository/LocationRecordRepository.java`

- [ ] **Step 1: Add the new query method**

Open `backend/src/main/java/com/silvercare/iot/repository/LocationRecordRepository.java` and add one method:

```java
package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.LocationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRecordRepository extends JpaRepository<LocationRecord, Long> {

    Optional<LocationRecord> findFirstByDeviceIdOrderByLocatedAtDesc(Long deviceId);

    List<LocationRecord> findTop100ByDeviceIdOrderByLocatedAtDesc(Long deviceId);

    List<LocationRecord> findTopNByDeviceIdAndGpsValidTrueOrderByLocatedAtDesc(Long deviceId, int limit);
}
```

Spring Data JPA does not support dynamic `TopN` with a variable. Replace with a `Pageable`-based method instead:

```java
package com.silvercare.iot.repository;

import com.silvercare.iot.domain.entity.LocationRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRecordRepository extends JpaRepository<LocationRecord, Long> {

    Optional<LocationRecord> findFirstByDeviceIdOrderByLocatedAtDesc(Long deviceId);

    List<LocationRecord> findTop100ByDeviceIdOrderByLocatedAtDesc(Long deviceId);

    List<LocationRecord> findByDeviceIdAndGpsValidTrueOrderByLocatedAtDesc(Long deviceId, Pageable pageable);
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS, no errors.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/silvercare/iot/repository/LocationRecordRepository.java
git commit -m "feat: add gpsValid filter query to LocationRecordRepository"
```

---

## Task 2: Create MiniappOverviewResponse DTO

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/api/dto/MiniappOverviewResponse.java`

- [ ] **Step 1: Create the DTO**

```java
package com.silvercare.iot.api.dto;

import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;

public record MiniappOverviewResponse(
        Device device,
        HealthRecord latestHealth,
        LocationRecord latestLocation
) {
    public static MiniappOverviewResponse of(Device device,
                                              HealthRecord latestHealth,
                                              LocationRecord latestLocation) {
        return new MiniappOverviewResponse(device, latestHealth, latestLocation);
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
cd backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/silvercare/iot/api/dto/MiniappOverviewResponse.java
git commit -m "feat: add MiniappOverviewResponse DTO"
```

---

## Task 3: Create MiniappDeviceController

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java`

- [ ] **Step 1: Write the failing test first**

Create `backend/src/test/java/com/silvercare/iot/api/MiniappDeviceControllerTest.java`:

```java
package com.silvercare.iot.api;

import com.silvercare.iot.domain.DeviceStatus;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MiniappDeviceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired DeviceRepository deviceRepository;
    @Autowired HealthRecordRepository healthRecordRepository;
    @Autowired LocationRecordRepository locationRecordRepository;

    private Device savedDevice;

    @BeforeEach
    void setUp() {
        locationRecordRepository.deleteAll();
        healthRecordRepository.deleteAll();
        deviceRepository.deleteAll();

        Device d = new Device();
        d.setDeviceNo("TEST001");
        d.setModel("TestWatch");
        d.setStatus(DeviceStatus.ONLINE);
        d.setBatteryLevel(80);
        d.setStepCount(1000);
        savedDevice = deviceRepository.save(d);
    }

    @Test
    void overview_returnsDeviceWithNullHealthAndLocation_whenNoRecords() throws Exception {
        mockMvc.perform(get("/api/miniapp/devices/TEST001/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device.deviceNo").value("TEST001"))
                .andExpect(jsonPath("$.latestHealth").isEmpty())
                .andExpect(jsonPath("$.latestLocation").isEmpty());
    }

    @Test
    void overview_returnsLatestHealthAndLocation_whenRecordsExist() throws Exception {
        HealthRecord hr = new HealthRecord();
        hr.setDeviceId(savedDevice.getId());
        hr.setHeartRate(72);
        hr.setSystolicPressure(120);
        hr.setDiastolicPressure(80);
        hr.setSourceCommand("bphrt");
        healthRecordRepository.save(hr);

        LocationRecord lr = new LocationRecord();
        lr.setDeviceId(savedDevice.getId());
        lr.setLatitude(new BigDecimal("22.570720"));
        lr.setLongitude(new BigDecimal("113.862017"));
        lr.setGpsValid(true);
        lr.setSourceCommand("UD");
        locationRecordRepository.save(lr);

        mockMvc.perform(get("/api/miniapp/devices/TEST001/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestHealth.heartRate").value(72))
                .andExpect(jsonPath("$.latestLocation.latitude").value(22.570720));
    }

    @Test
    void overview_returns404_whenDeviceNotFound() throws Exception {
        mockMvc.perform(get("/api/miniapp/devices/NOTEXIST/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthRecords_returnsRecords_defaultSize20() throws Exception {
        for (int i = 0; i < 25; i++) {
            HealthRecord hr = new HealthRecord();
            hr.setDeviceId(savedDevice.getId());
            hr.setHeartRate(60 + i);
            hr.setSourceCommand("bphrt");
            healthRecordRepository.save(hr);
        }

        mockMvc.perform(get("/api/miniapp/devices/TEST001/health-records")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void locationRecords_returnsOnlyGpsValidRecords() throws Exception {
        LocationRecord valid = new LocationRecord();
        valid.setDeviceId(savedDevice.getId());
        valid.setGpsValid(true);
        valid.setLatitude(new BigDecimal("22.5"));
        valid.setLongitude(new BigDecimal("113.8"));
        valid.setSourceCommand("UD");
        locationRecordRepository.save(valid);

        LocationRecord invalid = new LocationRecord();
        invalid.setDeviceId(savedDevice.getId());
        invalid.setGpsValid(false);
        invalid.setSourceCommand("UD");
        locationRecordRepository.save(invalid);

        mockMvc.perform(get("/api/miniapp/devices/TEST001/location-records")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void miniappEndpoints_requireNoAuthentication() throws Exception {
        // No Authorization header — must still return 200
        mockMvc.perform(get("/api/miniapp/devices/TEST001/overview"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
cd backend && ./mvnw test -pl . -Dtest=MiniappDeviceControllerTest -q 2>&1 | tail -20
```

Expected: FAIL — `MiniappDeviceController` does not exist yet.

- [ ] **Step 3: Create the controller**

```java
package com.silvercare.iot.api;

import com.silvercare.iot.api.dto.MiniappOverviewResponse;
import com.silvercare.iot.domain.entity.Device;
import com.silvercare.iot.domain.entity.HealthRecord;
import com.silvercare.iot.domain.entity.LocationRecord;
import com.silvercare.iot.repository.DeviceRepository;
import com.silvercare.iot.repository.HealthRecordRepository;
import com.silvercare.iot.repository.LocationRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/miniapp/devices")
public class MiniappDeviceController {

    private final DeviceRepository deviceRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final LocationRecordRepository locationRecordRepository;

    public MiniappDeviceController(DeviceRepository deviceRepository,
                                   HealthRecordRepository healthRecordRepository,
                                   LocationRecordRepository locationRecordRepository) {
        this.deviceRepository = deviceRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.locationRecordRepository = locationRecordRepository;
    }

    @GetMapping("/{deviceNo}/overview")
    public MiniappOverviewResponse overview(@PathVariable String deviceNo) {
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        HealthRecord latestHealth = healthRecordRepository
                .findFirstByDeviceIdOrderByMeasuredAtDesc(device.getId()).orElse(null);
        LocationRecord latestLocation = locationRecordRepository
                .findFirstByDeviceIdOrderByLocatedAtDesc(device.getId()).orElse(null);
        return MiniappOverviewResponse.of(device, latestHealth, latestLocation);
    }

    @GetMapping("/{deviceNo}/health-records")
    public List<HealthRecord> healthRecords(@PathVariable String deviceNo,
                                            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return healthRecordRepository.findTop100ByDeviceIdOrderByMeasuredAtDesc(device.getId())
                .stream().limit(clampedSize).toList();
    }

    @GetMapping("/{deviceNo}/location-records")
    public List<LocationRecord> locationRecords(@PathVariable String deviceNo,
                                                @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        Device device = deviceRepository.findByDeviceNo(deviceNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        return locationRecordRepository.findByDeviceIdAndGpsValidTrueOrderByLocatedAtDesc(
                device.getId(), PageRequest.of(0, clampedSize));
    }
}
```

- [ ] **Step 4: Run the tests again — expect failure on the auth test**

```bash
cd backend && ./mvnw test -pl . -Dtest=MiniappDeviceControllerTest -q 2>&1 | tail -20
```

Expected: `miniappEndpoints_requireNoAuthentication` still fails with 401 — security config not added yet.

- [ ] **Step 5: Commit the controller**

```bash
git add backend/src/main/java/com/silvercare/iot/api/MiniappDeviceController.java \
        backend/src/test/java/com/silvercare/iot/api/MiniappDeviceControllerTest.java
git commit -m "feat: add MiniappDeviceController with overview, health-records, location-records"
```

---

## Task 4: Add MiniappSecurityConfig to permit /api/miniapp/** without auth

**Files:**
- Create: `backend/src/main/java/com/silvercare/iot/config/MiniappSecurityConfig.java`

- [ ] **Step 1: Create the security config**

```java
package com.silvercare.iot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class MiniappSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain miniappSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/miniapp/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/miniapp/**"))
                .build();
    }
}
```

Note: `@Order(1)` ensures this chain is evaluated before `AdminSecurityConfig` (which has no explicit order, defaulting to a lower priority).

- [ ] **Step 2: Run all miniapp tests**

```bash
cd backend && ./mvnw test -pl . -Dtest=MiniappDeviceControllerTest -q 2>&1 | tail -20
```

Expected: All 6 tests PASS.

- [ ] **Step 3: Run the full test suite to check for regressions**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -20
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/silvercare/iot/config/MiniappSecurityConfig.java
git commit -m "feat: permit /api/miniapp/** without authentication"
```

---

## Task 5: Manual smoke test

- [ ] **Step 1: Start the backend**

```bash
cd backend && ./mvnw spring-boot:run -q &
sleep 8
```

- [ ] **Step 2: Verify 404 for unknown device**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/miniapp/devices/NOTEXIST/overview
```

Expected: `404`

- [ ] **Step 3: Verify overview returns 200 without auth**

```bash
curl -s http://localhost:8080/api/miniapp/devices/TEST001/overview | head -c 200
```

Expected: JSON with `device`, `latestHealth`, `latestLocation` keys (device may not exist in dev DB — 404 is also acceptable here; the key check is that no 401 is returned).

- [ ] **Step 4: Verify admin API still requires auth**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/admin/devices
```

Expected: `401`

- [ ] **Step 5: Stop the backend**

```bash
pkill -f spring-boot || true
```

- [ ] **Step 6: Final commit if any fixes were needed**

```bash
git status
# commit any remaining changes
```
