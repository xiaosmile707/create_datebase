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
| root 密码 | test123 | ✅ |
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
写入 `{output_dir}/db-connection.json`（`{output_dir}` 确定方式见 [公共流程](../_common.md) 步骤9）：
```json
{
  "host": "<ssh.host>",
  "port": 3306,
  "type": "mysql",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "<用户名>",
  "password": "<用户密码>",
  "database": "<数据库名>"
}
```

---

## SSL 加密连接流程

当用户要求开启 SSL 时，执行以下完整流程：

### 1. 生成 SSL 证书（远程服务器）

在目标机器上生成 CA、Server、Client 三组证书：

```bash
mkdir -p /root/mysql-ssl && cd /root/mysql-ssl

# CA 根证书
openssl genrsa 2048 > ca-key.pem
openssl req -new -x509 -nodes -days 3650 -key ca-key.pem -out ca.pem -subj "/CN=MySQL-CA/O=db-lab"

# Server 证书
openssl req -newkey rsa:2048 -nodes -keyout server-key.pem -out server-req.pem -subj "/CN=mysql-server/O=db-lab"
openssl x509 -req -in server-req.pem -days 3650 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem

# Client 证书
openssl req -newkey rsa:2048 -nodes -keyout client-key.pem -out client-req.pem -subj "/CN=mysql-client/O=db-lab"
openssl x509 -req -in client-req.pem -days 3650 -CA ca.pem -CAkey ca-key.pem -set_serial 02 -out client-cert.pem
```

### 2. 复制证书到容器并配置 MySQL

```bash
# 复制证书
docker cp /root/mysql-ssl/ca.pem <容器名>:/var/lib/mysql/ca.pem
docker cp /root/mysql-ssl/server-cert.pem <容器名>:/var/lib/mysql/server-cert.pem
docker cp /root/mysql-ssl/server-key.pem <容器名>:/var/lib/mysql/server-key.pem
docker cp /root/mysql-ssl/client-cert.pem <容器名>:/var/lib/mysql/client-cert.pem
docker cp /root/mysql-ssl/client-key.pem <容器名>:/var/lib/mysql/client-key.pem

# 设置权限
docker exec <容器名> chown mysql:mysql /var/lib/mysql/ca.pem /var/lib/mysql/server-cert.pem /var/lib/mysql/server-key.pem /var/lib/mysql/client-cert.pem /var/lib/mysql/client-key.pem
docker exec <容器名> chmod 600 /var/lib/mysql/server-key.pem /var/lib/mysql/client-key.pem
```

> **⚠️ 踩坑：MySQL SSL 变量不支持运行时修改**
>
> `ssl_ca`、`ssl_cert`、`ssl_key` 是只读系统变量，**不能**通过 `ALTER SYSTEM SET` 或 `SET GLOBAL` 修改。
> 必须通过写入配置文件 + 重启容器的方式生效。

### 3. 写入 SSL 配置并重启

```bash
# 写入 MySQL 配置文件
echo "[mysqld]
ssl-ca=/var/lib/mysql/ca.pem
ssl-cert=/var/lib/mysql/server-cert.pem
ssl-key=/var/lib/mysql/server-key.pem
require_secure_transport=ON" > /root/mysql-ssl/ssl.cnf

docker cp /root/mysql-ssl/ssl.cnf <容器名>:/etc/mysql/conf.d/ssl.cnf
docker restart <容器名>
```

重启后再次执行健康检查确认数据库就绪。

### 4. 创建 SSL 用户

```bash
# REQUIRE X509 = 强制客户端提供证书（双向 TLS）
# REQUIRE SSL = 仅要求加密通道（单向 TLS）
docker exec <容器名> mysql -u root -p<root密码> -e "CREATE USER IF NOT EXISTS '<用户名>'@'%' IDENTIFIED BY '<用户密码>' REQUIRE X509;"
docker exec <容器名> mysql -u root -p<root密码> -e "GRANT ALL PRIVILEGES ON <数据库名>.* TO '<用户名>'@'%'; FLUSH PRIVILEGES;"
```

### 5. 输出证书与连接信息

将客户端所需证书文件通过 SCP 拉取到本地 `{output_dir}/certs/mysql/`：
- `ca.pem` — CA 根证书
- `client-cert.pem` — 客户端证书
- `client-key.pem` — 客户端私钥

### 6. SSL 模式的 db-connection.json

```json
{
  "host": "<ssh.host>",
  "port": 3306,
  "type": "mysql",
  "version": "<版本>",
  "containerName": "<容器名>",
  "user": "<用户名>",
  "password": "<用户密码>",
  "database": "<数据库名>",
  "ssl": {
    "caPath": "certs/mysql/ca.pem",
    "certPath": "certs/mysql/client-cert.pem",
    "keyPath": "certs/mysql/client-key.pem"
  }
}
```
