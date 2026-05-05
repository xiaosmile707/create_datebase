---
name: oracle
description: SSH 到目标机器，通过 Docker 创建 Oracle 容器，创建用户，将连接信息写入 db-connection.json
---

> 本 Skill 遵循 [Docker 数据库公共流程](../_common.md)，以下仅声明 Oracle 特定参数。

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 23ai | ✅（支持 23ai / 21c） |
| 镜像 | container-registry.oracle.com/database/free:latest | ✅ |
| 容器名 | oracle-<版本> | ✅ |
| 端口映射 | 1521:1521 | ✅ |
| SYS 密码 | 询问用户指定 | ✅ |
| 内存限制 | 2g | ✅ |

## 数据库特定步骤

### 创建用户
Oracle 容器启动后，通过 docker exec 创建普通用户并授权：

```bash
docker exec <容器名> bash -c "echo \"CREATE USER <用户名> IDENTIFIED BY \\\"<用户密码>\\\" DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS; GRANT CONNECT, RESOURCE TO <用户名>;\" | sqlplus -s sys/<SYS密码>@FREE as sysdba"
```

默认值：用户名=`testuser`，用户密码=`<与SYS密码相同>`，均可由用户覆盖。

### 健康检查命令
```bash
docker exec <容器名> bash -c "echo 'SELECT 1 FROM DUAL;' | sqlplus -s sys/<SYS密码>@FREE as sysdba"
```

最多等待 180 秒（Oracle 启动较慢）。

### db-connection.json 格式
写入 `db-oracle/src/main/resources/db-connection.json`：
```json
{
  "host": "<ssh.host>",
  "port": <映射端口>,
  "type": "oracle",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "<用户名>",
  "database": "FREE"
}
```

注意：Oracle 免费版默认服务名为 `FREE`。
