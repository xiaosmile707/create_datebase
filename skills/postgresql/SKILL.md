---
name: postgresql
description: SSH 到目标机器，通过 Docker 创建 PostgreSQL 容器，创建数据库和用户，将连接信息写入 db-connection.json
---

> 本 Skill 遵循 [Docker 数据库公共流程](../_common.md)，以下仅声明 PostgreSQL 特定参数。

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 16 | ✅（支持 16 / 15 / 14） |
| 镜像 | postgres:<版本> | ✅ |
| 容器名 | postgres-<版本> | ✅ |
| 端口映射 | 5432:5432 | ✅ |
| 密码 | test123 | ✅ |
| 内存限制 | 512m | ✅ |

## 数据库特定步骤

### 创建数据库与用户
PostgreSQL 容器启动后，通过 docker exec 创建数据库和普通用户：

```bash
# 创建用户
docker exec <容器名> psql -U postgres -c "CREATE USER <用户名> WITH PASSWORD '<用户密码>';"

# 创建数据库并授权
docker exec <容器名> psql -U postgres -c "CREATE DATABASE <数据库名> OWNER <用户名>;"
```

默认值：数据库名=`testdb`，用户名=`testuser`，用户密码=`<与postgres密码相同>`，均可由用户覆盖。

### 健康检查命令
```bash
docker exec <容器名> pg_isready -U postgres
```

### db-connection.json 格式
写入 `{output_dir}/db-connection.json`（`{output_dir}` 确定方式见 [公共流程](../_common.md) 步骤9）：
```json
{
  "host": "<ssh.host>",
  "port": 5432,
  "type": "postgresql",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "<用户名>",
  "password": "<用户密码>",
  "database": "<数据库名>"
}
```
