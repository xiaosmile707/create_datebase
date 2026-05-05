# db-lab

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
db-lab/
├── skills/                     # AI Agent Skill 模块（独立可复用）
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
├── 以下为真实配置（不提交 Git，开发者自行创建）：
├── skills/ssh-config.properties # Skills 模块自带 SSH 配置
│
├── 以下为 Skill 生成的连接信息（不提交 Git）：
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
cd db-lab
```

复制示例配置文件并填入真实值：

```bash
# Linux / Mac
cp skills/ssh-config.properties.example skills/ssh-config.properties

# Windows PowerShell
Copy-Item skills/ssh-config.properties.example skills/ssh-config.properties
```

**编辑 skills/ssh-config.properties**（Skills 模块使用）：

```properties
ssh.host=192.168.1.100    # Docker 宿主机 IP
ssh.port=22                # SSH 端口
ssh.user=root              # SSH 用户名
ssh.password=your_password # SSH 密码
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
5. 将连接信息写入 `{output_dir}/db-connection.json`（默认输出到当前目录，可通过指令指定）

示例指令：
```
帮我创建一台 MySQL 8.0，root 密码用 root123
```
```
在 192.168.1.100 上起一个 PostgreSQL 16，端口用 5433，输出到 ./config/
```

> **输出路径优先级**：用户指定 > 当前目录 `./`

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

确保 `db-connection.json` 已就绪，然后执行：

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

将证书文件放入 `certs/<type>/` 目录。当前 SSL 配置暂不支持通过 JSON 设置，如需 SSL 请使用环境变量或自定义 ConfigLoader。

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

所有连接信息从 **db-connection.json** 读取，无需额外配置文件。

### db-connection.json 字段说明

| Key | 说明 |
|-----|------|
| `host` | 数据库所在机器 IP |
| `port` | 数据库端口 |
| `type` | 数据库类型（mysql / postgresql / oracle / h2） |
| `version` | 数据库版本（如 8.0 / 16 / 23ai / 2.x） |
| `user` | 登录用户名 |
| `password` | 登录密码 |
| `database` | 数据库名 |
| `containerName` | Docker 容器名 |

### AI 模块文档

详细 Skill 说明、独立使用方法和自定义指南见 [skills/README.md](skills/README.md)。
