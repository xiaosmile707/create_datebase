---
name: h2
description: SSH 到目标机器，通过 Docker 创建 H2 TCP Server 容器，将连接信息写入 db-connection.json
---

> 本 Skill 遵循 [Docker 数据库公共流程](../_common.md)，以下仅声明 H2 特定参数。

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 2.x | ✅ |
| 镜像 | oscarfonts/h2:latest | ✅ |
| 容器名 | h2-<版本> | ✅ |
| TCP 端口映射 | 9092:9092 | ✅ |
| Web 端口映射 | 8082:8082 | ✅ |
| 用户名 | sa | ✅ |
| 密码 | （空） | ✅ |
| 内存限制 | 256m | ✅ |

## 数据库特定步骤

### 健康检查命令
H2 TCP 服务启动后通过 Java 工具类检测端口：
```bash
docker exec <容器名> java -cp /opt/h2/bin/h2*.jar org.h2.tools.Shell -url "jdbc:h2:tcp://localhost:9092/testdb" -user sa -password "" -sql "SELECT 1"
```

最多等待 30 秒。

### 创建数据库
H2 在首次 TCP 连接时自动创建数据库，无需手动执行 CREATE DATABASE。

默认数据库名=`testdb`，用户名=`sa`，用户密码为空，均可由用户覆盖。

### db-connection.json 格式
写入 `{output_dir}/db-connection.json`（`{output_dir}` 确定方式见 [公共流程](../_common.md) 步骤9）：
```json
{
  "host": "<ssh.host>",
  "port": 9092,
  "type": "h2",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "sa",
  "password": "",
  "database": "testdb"
}
```

注意：H2 默认管理员用户为 `sa`，无密码，通过 TCP 协议连接（非嵌入式内存模式）。
