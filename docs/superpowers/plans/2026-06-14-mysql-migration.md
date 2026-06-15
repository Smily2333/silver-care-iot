# MySQL 数据源切换实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 silver-care-iot 后端从 H2 内存数据库切换到本机 Docker MySQL（kangyang-mysql），实现数据持久化。

**Architecture:** 在 `kangyang-mysql` 容器中创建专用数据库和账号，修改 `application.yml` 指向 MySQL，移除 H2 依赖。JPA `ddl-auto: update` 在首次启动时自动建表。

**Tech Stack:** Spring Boot 3.3.5, Spring Data JPA, MySQL 8.0 (Docker), mysql-connector-j

---

## 文件变更清单

- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/pom.xml`

---

### Task 1: 在 kangyang-mysql 中创建数据库和账号

**Files:**
- 无代码文件改动，仅容器内 SQL 操作

- [ ] **Step 1: 进入 MySQL 容器**

```bash
docker exec -it kangyang-mysql mysql -uroot
```

- [ ] **Step 2: 执行初始化 SQL**

在 MySQL 提示符下依次执行：

```sql
CREATE DATABASE silver_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'silvercare'@'%' IDENTIFIED BY 'silvercare123';
GRANT ALL PRIVILEGES ON silver_care.* TO 'silvercare'@'%';
FLUSH PRIVILEGES;
```

- [ ] **Step 3: 验证数据库和用户创建成功**

```sql
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user = 'silvercare';
```

预期输出：`SHOW DATABASES` 结果中包含 `silver_care`；`SELECT` 返回一行 `silvercare | %`。

- [ ] **Step 4: 退出容器**

```sql
EXIT;
```

---

### Task 2: 修改 application.yml

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 替换整个 datasource 和 h2 配置块**

将 `application.yml` 改为以下内容（完整文件）：

```yaml
server:
  port: 8080

spring:
  application:
    name: silver-care-iot-backend
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/silver_care?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: silvercare
    password: silvercare123
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect

silver-care:
  admin:
    username: ${SILVER_CARE_ADMIN_USERNAME:admin}
    password: ${SILVER_CARE_ADMIN_PASSWORD:change-me}
  gateway:
    enabled: true
    port: 9001
    idle-timeout-seconds: 180
    max-client-threads: 200
    client-queue-capacity: 200
    accept-backlog: 128
    max-frame-length-bytes: 8192
```

关键变化：
- `spring.datasource` 全部替换为 MySQL 配置
- 新增 `hibernate.dialect: org.hibernate.dialect.MySQL8Dialect`
- `spring.h2.console` 整块移除

---

### Task 3: 移除 pom.xml 中的 H2 依赖

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: 删除 h2 依赖块**

找到并删除以下整段：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

`mysql-connector-j` 已在 pom.xml 中存在，无需新增。

---

### Task 4: 验证启动和自动建表

**Files:**
- 无改动，仅验证步骤

- [ ] **Step 1: 在 backend 目录下运行测试，确认现有单元测试仍通过**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn test 2>&1 | tail -20
```

预期：`BUILD SUCCESS`，测试不依赖数据库（均使用 mock），应全部通过。

- [ ] **Step 2: 启动应用**

```bash
cd /home/ubuntu/www/silver-care-iot/backend && mvn spring-boot:run 2>&1 | grep -E "(Started|ERROR|HHH|Creating table|Updating table)" | head -30
```

预期日志中出现：
- `Creating table: devices`
- `Creating table: health_records`
- `Creating table: location_records`
- `Creating table: raw_packet_logs`
- `Started SilverCareIotApplication`

- [ ] **Step 3: 验证表已在 MySQL 中创建**

```bash
docker exec kangyang-mysql mysql -uroot -e "USE silver_care; SHOW TABLES; SHOW INDEX FROM devices;"
```

预期：列出 4 张表，`devices` 表有 `idx_devices_device_no` 唯一索引。

- [ ] **Step 4: 停止应用（Ctrl+C），提交改动**

```bash
cd /home/ubuntu/www/silver-care-iot/backend
git add src/main/resources/application.yml pom.xml
git commit -m "feat: switch datasource from H2 to MySQL (kangyang-mysql)"
```
