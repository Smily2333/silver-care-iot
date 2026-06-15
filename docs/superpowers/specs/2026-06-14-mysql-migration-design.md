# 切换数据源：H2 → MySQL

**日期：** 2026-06-14  
**项目：** silver-care-iot / backend  
**方案：** A（最小改动，直接替换）

---

## 背景

后端当前使用 H2 内存数据库，每次重启数据全部丢失。`kangyang-mysql`（Docker mysql:8.0）已在本机 `127.0.0.1:3306` 运行，直接接入即可实现数据持久化。

---

## 目标

- 将 datasource 切换为 MySQL，数据持久化
- 最小改动范围，不引入额外工具（Flyway 等）
- 保持 JPA `ddl-auto: update` 自动维护 schema

---

## 数据库初始化（一次性手动操作）

在 `kangyang-mysql` 容器内执行：

```sql
CREATE DATABASE silver_care CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'silvercare'@'%' IDENTIFIED BY 'silvercare123';
GRANT ALL PRIVILEGES ON silver_care.* TO 'silvercare'@'%';
FLUSH PRIVILEGES;
```

- 字符集 `utf8mb4` 支持中文及 emoji
- 使用专用账号而非 root，权限最小化

---

## 代码改动

### 1. `backend/src/main/resources/application.yml`

- `spring.datasource.url` 改为 `jdbc:mysql://127.0.0.1:3306/silver_care?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai`
- `spring.datasource.driver-class-name` 改为 `com.mysql.cj.jdbc.Driver`
- `spring.datasource.username` 改为 `silvercare`
- `spring.datasource.password` 改为 `silvercare123`
- 新增 `hibernate.dialect: org.hibernate.dialect.MySQL8Dialect`
- 移除 `spring.h2.console` 整块配置

### 2. `backend/pom.xml`

- 移除 h2 依赖块（`com.h2database:h2`）
- `mysql-connector-j` 已存在，无需新增

---

## 自动建表

启动后 JPA `ddl-auto: update` 将在 `silver_care` 库自动创建：

| 表名 | 实体 |
|------|------|
| `devices` | `Device` |
| `health_records` | `HealthRecord` |
| `location_records` | `LocationRecord` |
| `raw_packet_logs` | `RawPacketLog` |

索引由 `@Table(indexes = {...})` 注解自动创建。

---

## 约束与风险

- `ddl-auto: update` 不会删列，schema 漂移需人工跟踪——MVP 阶段可接受，生产前建议迁移到 Flyway
- 密码 `silvercare123` 为开发环境默认值，生产部署前须通过环境变量注入替换
- `kangyang-mysql` 容器重建会导致数据丢失，需确认容器挂载了 volume（当前容器已运行 2 周，状态稳定）
