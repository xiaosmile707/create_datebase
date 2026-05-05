# create_datebase

用于通过 Docker 远程创建多种数据库实例，并提供 JDBC/Java 客户端连接、测试 SQL 执行、测试数据灌入的一体化工具。

## 技术栈

| 类别 | 技术 |
|------|------|
| 构建工具 | Maven（多模块） |
| 语言 | Java |
| 数据库 | MySQL、PostgreSQL、Oracle、H2 |
| 容器化 | Docker（通过 SSH 远程创建） |
| 连接方式 | JDBC + 各数据库专用 Java 客户端 |
| AI 辅助 | AI Agent Skill（自动化创建不同数据库） |

## 项目结构

```
create_datebase/
├── skills/                     # AI Agent Skill 定义
│   ├── README.md
│   ├── _common.md              # Docker 数据库公共流程模板
│   ├── mysql/SKILL.md
│   ├── postgresql/SKILL.md
│   ├── oracle/SKILL.md
│   └── h2/SKILL.md
├── db-common/                  # 公共模块（接口、配置、SQL 运行器）
├── db-mysql/                   # MySQL 连接模块
├── db-postgresql/              # PostgreSQL 连接模块
├── db-oracle/                  # Oracle 连接模块
├── db-h2/                      # H2 连接模块
├── certs/                      # SSL 证书（不提交 Git）
├── pom.xml                     # 父 POM
├── README.md
│
├── 以下为示例文件（提交 Git）：
├── ssh-config.properties.example
├── db-secret.properties.example
│
├── 以下为真实配置（不提交 Git，开发者自行创建）：
├── ssh-config.properties
├── db-secret.properties
│
├── 以下为 AI 生成的连接信息（不提交 Git）：
└── */db-connection.json
```

## 支持的数据库

| 数据库 | Docker 创建 | JDBC 连接 | 支持版本 |
|--------|:----------:|:---------:|----------|
| MySQL  | ✅ | ✅ | 8.0 / 5.7 |
| PostgreSQL | ✅ | ✅ | 16 / 15 / 14 |
| Oracle | ✅ | ✅ | 23ai / 21c |
| H2 | ✅ | ✅ | 2.x（TCP Server 模式） |

---

## 运行教程

### 第一步：环境准备

- **JDK**：Open JDK 8 或更高版本
- **Maven**：3.8+
- **Docker**：目标机器需安装 Docker 并开放 SSH（22 端口）
- **网络**：本地能 SSH 连接目标机器

### 第二步：克隆项目并配置

```bash
git clone <仓库地址>
cd create_datebase
```

复制示例配置文件并填入真实值：

```bash
# Linux / Mac
cp ssh-config.properties.example ssh-config.properties
cp db-secret.properties.example db-secret.properties

# Windows PowerShell
Copy-Item ssh-config.properties.example ssh-config.properties
Copy-Item db-secret.properties.example db-secret.properties
```

**编辑 ssh-config.properties**（AI 模块使用）：

```properties
ssh.host=192.168.1.100    # Docker 宿主机 IP
ssh.port=22                # SSH 端口
ssh.user=root              # SSH 用户名
ssh.password=your_password # SSH 密码
```

**编辑 db-secret.properties**（Java 模块使用）：

```properties
# MySQL
mysql.host=192.168.1.100
mysql.port=3306
mysql.version=8.0
mysql.database=testdb
mysql.user=testuser
mysql.password=your_password

# PostgreSQL
postgresql.host=192.168.1.100
postgresql.port=5432
postgresql.version=16
postgresql.database=testdb
postgresql.user=testuser
postgresql.password=your_password

# Oracle
oracle.host=192.168.1.100
oracle.port=1521
oracle.version=23ai
oracle.database=FREE
oracle.user=testuser
oracle.password=your_password

# H2
h2.host=192.168.1.100
h2.port=9092
h2.version=2.x
h2.database=testdb
h2.user=sa
h2.password=
```

### 第三步：构建项目

```bash
mvn clean install -DskipTests
```

此命令编译所有模块并安装到本地 Maven 仓库。如果依赖下载缓慢，可在 Maven 的 `settings.xml` 中配置国内镜像。

### 第四步：创建数据库实例（两种方式）

#### 方式 A：通过 AI Agent Skill 自动创建（推荐）

在 AI Agent 对话中发起自然语言指令，AI 会自动：
1. SSH 登录目标机器
2. Docker 拉取镜像并启动容器
3. 健康检查等待数据库就绪
4. 创建数据库和用户
5. 将连接信息写入 `db-connection.json`

示例指令：
```
帮我创建一台 MySQL 8.0，root 密码用 root123
```
```
在 192.168.1.100 上起一个 PostgreSQL 16，端口用 5433
```

> 注意：`db-connection.json` 中的 host/port/version/user/database 会被 Java 模块自动读取，
> 因此如果使用 AI 方式创建，`db-secret.properties` 中只需填写 password 即可。

#### 方式 B：手动 Docker 创建

如果不使用 AI，你也可以手动在目标机器上执行 Docker 命令。

**MySQL 8.0 示例**：
```bash
docker rm -f mysql-8.0 2>/dev/null || true
docker run -d --name mysql-8.0 -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 --memory=512m mysql:8.0

# 等待就绪后创建业务数据库和用户
docker exec mysql-8.0 mysql -u root -proot123 \
  -e "CREATE DATABASE IF NOT EXISTS testdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
docker exec mysql-8.0 mysql -u root -proot123 \
  -e "CREATE USER 'testuser'@'%' IDENTIFIED BY 'root123'; GRANT ALL ON testdb.* TO 'testuser'@'%'; FLUSH PRIVILEGES;"
```

**PostgreSQL 16 示例**：
```bash
docker rm -f postgres-16 2>/dev/null || true
docker run -d --name postgres-16 -p 5432:5432 \
  -e POSTGRES_PASSWORD=root123 --memory=512m postgres:16

# 等待就绪后创建业务用户和数据库
docker exec postgres-16 psql -U postgres -c "CREATE USER testuser WITH PASSWORD 'root123';"
docker exec postgres-16 psql -U postgres -c "CREATE DATABASE testdb OWNER testuser;"
```

**H2 示例**：
```bash
docker rm -f h2-2.x 2>/dev/null || true
docker run -d --name h2-2.x -p 9092:9092 -p 8082:8082 \
  --memory=256m oscarfonts/h2:latest
```

### 第五步：运行 Java 模块验证

确保 `db-secret.properties` 中对应数据库的 host/port/user/password 已填写正确，
然后执行：

```bash
# MySQL
mvn exec:java -pl db-mysql

# PostgreSQL
mvn exec:java -pl db-postgresql

# Oracle
mvn exec:java -pl db-oracle

# H2
mvn exec:java -pl db-h2
```

也可指定自定义配置文件路径：
```bash
mvn exec:java -pl db-mysql -Dexec.args="my-db-secret.properties"
```

运行成功后会看到类似输出：
```
===== db-mysql =====
Type: mysql | Version: 8.0
Host: 192.168.1.100:3306
Connected.
Connection test:
  1
Executing: sql/8.0/schema.sql
  0 row(s) affected
  0 row(s) affected
  OK
Executing: sql/8.0/test.sql
  ...
Executing: sql/8.0/seed.sql
  3 row(s) affected
  5 row(s) affected
  OK
Done.
```

### 第六步（可选）：配置 SSL 加密连接

将证书文件放入 `certs/<type>/` 目录，并在 `db-secret.properties` 中配置路径：

```properties
mysql.ssl.ca=certs/mysql/ca.pem
mysql.ssl.cert=certs/mysql/cert.pem
mysql.ssl.key=certs/mysql/key.pem
```

Java 模块检测到证书配置后会自动启用 SSL 加密连接。

---

## SQL 脚本说明

每个数据库模块的 `src/main/resources/sql/<version>/` 下包含三类脚本：

| 文件 | 用途 |
|------|------|
| `schema.sql` | DDL 建表（test_users、test_orders） |
| `test.sql` | 测试查询（统计用户数、订单数、联表查询） |
| `seed.sql` | 种子数据灌入（3 条用户 + 5 条订单） |

所有模块共享相同的表结构：

```
test_users (id, name, email, created_at)
test_orders (id, user_id, amount, status, created_at)  → FK → test_users
```

---

## 配置说明

### 配置源优先级

Java 模块读取配置的优先级：**db-secret.properties > db-connection.json > 默认值**

- `db-secret.properties`：主配置源，含 host/port/version/user/password/ssl
- `db-connection.json`：AI Skill 创建的连接信息，作为 fallback
- 密码**仅**从 `db-secret.properties` 读取，不会出现在 JSON 中

### db-secret.properties 字段说明

| Key | 说明 |
|-----|------|
| `<type>.host` | 数据库所在机器 IP |
| `<type>.port` | 数据库端口 |
| `<type>.version` | 数据库版本（如 8.0 / 16 / 23ai / 2.x） |
| `<type>.database` | 数据库名 |
| `<type>.user` | 登录用户名 |
| `<type>.password` | 登录密码 |
| `<type>.ssl.ca` | SSL CA 证书路径（可选） |
| `<type>.ssl.cert` | SSL 客户端证书路径（可选） |
| `<type>.ssl.key` | SSL 客户端私钥路径（可选） |

### AI 模块文档

详细 Skill 说明和自定义方法见 [skills/README.md](skills/README.md)。
