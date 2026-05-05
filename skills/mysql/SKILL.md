---
name: mysql
description: SSH 到目标机器，通过 Docker 创建 MySQL 容器，创建数据库和用户，将连接信息写入 db-connection.json
---

> 本 Skill 遵循 [Docker 数据库公共流程](../_common.md)，以下仅声明 MySQL 特定参数。

## 默认 Docker 参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 8.0 | ✅（支持 8.0 / 5.7） |
| 镜像 | mysql:<版本> | ✅ |
| 容器名 | mysql-<版本> | ✅ |
| 端口映射 | 3306:3306 | ✅ |
| root 密码 | 询问用户指定 | ✅ |
| 内存限制 | 512m | ✅ |

## 数据库特定步骤

### 创建数据库与用户
MySQL 容器启动后，通过 docker exec 创建数据库和普通用户：

```bash
# 创建数据库
docker exec <容器名> mysql -u root -p<root密码> -e "CREATE DATABASE IF NOT EXISTS <数据库名> CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 创建用户并授权
docker exec <容器名> mysql -u root -p<root密码> -e "CREATE USER IF NOT EXISTS '<用户名>'@'%' IDENTIFIED BY '<用户密码>';"
docker exec <容器名> mysql -u root -p<root密码> -e "GRANT ALL PRIVILEGES ON <数据库名>.* TO '<用户名>'@'%'; FLUSH PRIVILEGES;"
```

默认值：数据库名=`testdb`，用户名=`testuser`，用户密码=`<与root密码相同>`，均可由用户覆盖。

### 健康检查命令
```bash
docker exec <容器名> mysqladmin ping -h localhost -u root -p<root密码>
```

### db-connection.json 格式
写入 `db-mysql/src/main/resources/db-connection.json`：
```json
{
  "host": "<ssh.host>",
  "port": <映射端口>,
  "type": "mysql",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "<用户名>",
  "database": "<数据库名>"
}
```
