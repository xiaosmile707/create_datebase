---
name: oracle
description: SSH 到目标机器，通过 Docker 创建 Oracle 23ai Free 容器，将连接信息写入 db-connection.json
---

## 前置条件
- 读取 `ssh-config.properties` 获取 SSH 连接凭据
- 若缺失则询问用户补充（host、port、user、password）

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 23ai | ✅（支持 23ai / 21c / 19c 等） |
| 镜像 | container-registry.oracle.com/database/free:latest | ✅ |
| 容器名 | oracle-<版本> | ✅ |
| 端口映射 | 1521:1521 | ✅ |
| SYS 密码 | 询问用户指定 | ✅ |
| 内存限制 | 2g | ✅ |

## 执行步骤

### 1. 读取 SSH 凭据
从 `ssh-config.properties` 中读取 ssh.host / ssh.port / ssh.user / ssh.password。

### 2. 确认参数
向用户展示默认参数，允许修改：版本、端口、容器名、SYS 密码、内存限制。

### 3. SSH 连接目标机器
```
ssh -o StrictHostKeyChecking=no -p <ssh.port> <ssh.user>@<ssh.host>
```

### 4. 清理旧容器（如存在）
```
docker rm -f <容器名>
```

### 5. 创建 Oracle 容器
```
docker run -d \
  --name <容器名> \
  -p <端口映射> \
  -e ORACLE_PWD=<SYS密码> \
  --memory=<内存限制> \
  <镜像>
```

### 6. 健康检查
循环检查 Oracle 是否就绪，最多等待 180 秒（Oracle 启动较慢）：
```
docker exec <容器名> bash -c "echo 'SELECT 1 FROM DUAL;' | sqlplus -s sys/<SYS密码>@FREE as sysdba"
```
成功后输出 "Oracle is ready"。

### 7. 写入 db-connection.json
将连接信息写入 `db-oracle/src/main/resources/db-connection.json`：
```json
{
  "host": "<ssh.host>",
  "port": <映射端口>,
  "type": "oracle",
  "version": "<版本>",
  "containerName": "<容器名>"
}
```
