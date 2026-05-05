---
name: postgresql
description: SSH 到目标机器，通过 Docker 创建 PostgreSQL 容器，将连接信息写入 db-connection.json
---

## 前置条件
- 读取 `ssh-config.properties` 获取 SSH 连接凭据
- 若缺失则询问用户补充（host、port、user、password）

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 16 | ✅（支持 16 / 15 / 14 等） |
| 镜像 | postgres:<版本> | ✅ |
| 容器名 | postgres-<版本> | ✅ |
| 端口映射 | 5432:5432 | ✅ |
| 密码 | 询问用户指定 | ✅ |
| 内存限制 | 512m | ✅ |

## 执行步骤

### 1. 读取 SSH 凭据
从 `ssh-config.properties` 中读取 ssh.host / ssh.port / ssh.user / ssh.password。

### 2. 确认参数
向用户展示默认参数，允许修改：版本、端口、容器名、密码、内存限制。

### 3. SSH 连接目标机器
```
ssh -o StrictHostKeyChecking=no -p <ssh.port> <ssh.user>@<ssh.host>
```

### 4. 清理旧容器（如存在）
```
docker rm -f <容器名>
```

### 5. 创建 PostgreSQL 容器
```
docker run -d \
  --name <容器名> \
  -p <端口映射> \
  -e POSTGRES_PASSWORD=<密码> \
  --memory=<内存限制> \
  postgres:<版本>
```

### 6. 健康检查
循环检查 PostgreSQL 是否就绪，最多等待 60 秒：
```
docker exec <容器名> pg_isready -U postgres
```
成功后输出 "PostgreSQL is ready"。

### 7. 写入 db-connection.json
将连接信息写入 `db-postgresql/src/main/resources/db-connection.json`：
```json
{
  "host": "<ssh.host>",
  "port": <映射端口>,
  "type": "postgresql",
  "version": "<版本>",
  "containerName": "<容器名>"
}
```
