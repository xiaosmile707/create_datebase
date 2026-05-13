---
name: clickhouse
description: SSH 到目标机器，通过 Docker 创建 ClickHouse 容器，创建数据库和用户，将连接信息写入 db-connection.json
---

> 本 Skill 遵循 [Docker 数据库公共流程](../_common.md)，以下仅声明 ClickHouse 特定参数。

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 21.8 | ✅ |
| 镜像 | clickhouse/clickhouse-server:<版本> | ✅ |
| 容器名 | clickhouse-<版本> | ✅ |
| 端口映射 | 8123:8123（HTTP）, 9000:9000（TCP） | ✅ |
| default 用户密码 | test123 | ✅ |
| 内存限制 | 512m | ✅ |

## 数据库特定步骤

### 创建数据库与用户
ClickHouse 容器启动后，通过 docker exec 创建数据库和普通用户：

```bash
# 创建数据库
docker exec <容器名> clickhouse-client --user default --password <default密码> --query "CREATE DATABASE IF NOT EXISTS <数据库名>"

# 创建用户并授权
docker exec <容器名> clickhouse-client --user default --password <default密码> --query "CREATE USER IF NOT EXISTS <用户名> IDENTIFIED BY '<用户密码>'"
docker exec <容器名> clickhouse-client --user default --password <default密码> --query "GRANT ALL ON <数据库名>.* TO <用户名>"
```

默认值：数据库名=`testdb`，用户名=`testuser`，用户密码=`<与default密码相同>`，均可由用户覆盖。

> **⚠️ 注意：需要启用访问控制**
>
> 默认情况下 ClickHouse 不启用 SQL 驱动的访问管理（CREATE USER/GRANT 等）。
> 启动容器时需设置环境变量 `CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT=1` 才能使用 SQL 命令管理用户和权限。

### 健康检查命令
```bash
docker exec <容器名> clickhouse-client --user default --password <default密码> --query "SELECT version()"
```

### db-connection.json 格式
写入 `{output_dir}/db-connection.json`（`{output_dir}` 确定方式见 [公共流程](../_common.md) 步骤9）：
```json
{
  "host": "<ssh.host>",
  "port": 8123,
  "type": "clickhouse",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "<用户名>",
  "password": "<用户密码>",
  "database": "<数据库名>"
}
```

> **⚠️ 注意：端口选择**
>
> ClickHouse JDBC 驱动（`ru.yandex.clickhouse:clickhouse-jdbc`）默认使用 HTTP 协议，
> 因此 db-connection.json 中的 `port` 应填 HTTP 端口（默认 8123），而非原生 TCP 端口 9000。

---

## ClickHouse 特殊注意事项

### 事务支持
ClickHouse **不支持传统 ACID 事务**（无 BEGIN/COMMIT/ROLLBACK）。
JDBC 驱动会静默忽略 `setAutoCommit(false)` 和 `rollback()` 调用，因此与 `DatabaseBootstrap` 兼容，
但失败时无法真正回滚已执行的 DDL/DML。cleanup.sql 必须覆盖所有建表操作以确保幂等清理。

### SQL 方言差异
- 不支持 AUTO_INCREMENT，使用 UInt32/UInt64 类型
- 不支持外键约束
- 必须指定表引擎（如 MergeTree）和 ORDER BY 子句
- INSERT 语法与标准 SQL 基本兼容
- 使用 `IF NOT EXISTS` / `IF EXISTS` 保证幂等

### 端口冲突
TCP 端口 9000 容易与其他服务冲突（如 Milvus MinIO）。
若端口被占用，优先更换 ClickHouse 的 TCP 映射端口（如 9100:9000），HTTP 端口 8123 通常无冲突。
