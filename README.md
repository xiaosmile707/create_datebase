# create_datebase

用于通过 Docker 远程创建多种数据库实例，并提供 JDBC/Java 客户端连接、测试 SQL 执行、测试数据灌入的一体化工具。

## 技术栈

| 类别 | 技术 |
|------|------|
| 构建工具 | Maven（多模块） |
| 语言 | Java |
| 数据库 | MySQL、PostgreSQL、Oracle、H2（后续扩展更多） |
| 容器化 | Docker（通过 SSH 远程创建） |
| 连接方式 | JDBC + 各数据库专用 Java 客户端 |
| AI 辅助 | AI Agent Skill（自动化创建不同数据库） |

## 项目结构

```
create_datebase/
├── .opencode/skills/           # AI Agent Skill 定义
│   ├── README.md
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
│   ├── README.md
│   ├── mysql/
│   ├── postgresql/
│   ├── oracle/
│   └── h2/
├── pom.xml                     # 父 POM
├── README.md
│
├── 以下为示例文件（提交 Git）：
├── ssh-config.properties.example
├── db-secret.properties.example
│
├── 以下为真实配置（不提交 Git，开发者自行创建）：
├── ssh-config.properties
└── db-secret.properties
```

## 功能

### AI 模块（ai-skill）→ [详细文档](.opencode/skills/README.md)

通过 AI Agent Skill 实现一键创建数据库，无需手动操作 Docker。

#### 使用方式
在 AI Agent 对话中输入自然语言指令，例如：
- "帮我创建一台 MySQL 8.0"
- "在 192.168.1.100 上用 Docker 起一个 PostgreSQL，端口用 3307"

#### 核心流程
1. AI Agent 加载对应数据库的 Skill 定义
2. 从 `ssh-config.properties` 读取 SSH 凭据
3. SSH 到目标机器执行 Docker 命令创建容器
4. 健康检查通过后将连接信息写入对应模块的 `resources/db-connection.json`

### Java 模块

每个数据库独立为一个 Maven 模块，公共逻辑抽取到 `db-common`。

| 模块 | 数据库 | 驱动 | 运行命令 |
|------|--------|------|----------|
| `db-mysql` | MySQL | mysql-connector-j | `mvn exec:java -pl db-mysql` |
| `db-postgresql` | PostgreSQL | postgresql | `mvn exec:java -pl db-postgresql` |
| `db-oracle` | Oracle | ojdbc8 | `mvn exec:java -pl db-oracle` |
| `db-h2` | H2 | h2 | `mvn exec:java -pl db-h2` |
| `db-common` | 公共接口 | — | — |

#### 核心流程
1. 读取 `db-secret.properties` 获取全部连接信息
2. 每个模块内置对应数据库的 JDBC 驱动
3. 执行连接测试 → DDL 建表 → 测试 SQL → 灌入数据

## 支持的数据库

| 数据库 | Docker 创建 | JDBC 连接 | 原生客户端 | 状态 |
|--------|:----------:|:---------:|:---------:|:----:|
| MySQL  | ✅ | ✅ | ✅ | 规划中 |
| PostgreSQL | ✅ | ✅ | ✅ | 规划中 |
| Oracle | ✅ | ✅ | ✅ | 规划中 |
| H2 | ❌（嵌入式） | ✅ | ✅ | 规划中 |

## 快速开始

### 环境要求
- Open JDK 8
- Maven 3.8+
- Docker（目标机器上）
- SSH 连接目标机器

### 初始化配置

首次 clone 后，复制示例文件并填入真实值：

```bash
cp ssh-config.properties.example ssh-config.properties
cp db-secret.properties.example db-secret.properties
# 编辑 ssh-config.properties 和 db-secret.properties
```

将 SSL 证书（如有）放入 `certs/<type>/` 对应目录。

### 构建

```bash
mvn clean install
```

### 使用 AI Agent Skill 创建数据库

通过 AI Agent 执行对应 Skill，自动 Docker 拉起数据库容器。

### 使用 Java 模块连接测试

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

## 配置

项目依赖三个配置文件，含密码的文件均不纳入 Git。
首次使用请复制 `.example` 文件并填入真实值：

```bash
cp ssh-config.properties.example ssh-config.properties
cp db-secret.properties.example db-secret.properties
```

### 1. SSH 连接配置（AI 模块使用）

`ssh-config.properties` — 不提交 Git，由 AI Skill 读取用于远程创建容器：

```properties
# ssh-config.properties
ssh.host=192.168.1.100
ssh.port=22
ssh.user=root
ssh.password=your_password
```

### 2. 数据库连接配置（Java 模块使用）

`db-secret.properties` — 不提交 Git，Java 模块直接读取，格式为 `<type>.<key>`，可同时配置多种数据库：

```properties
# db-secret.properties
mysql.host=192.168.1.100
mysql.port=3306
mysql.version=8.0
mysql.user=admin
mysql.password=your_password

# SSL 证书路径（可选，配置后自动启用）
# mysql.ssl.ca=certs/mysql/ca.pem
# mysql.ssl.cert=certs/mysql/cert.pem
# mysql.ssl.key=certs/mysql/key.pem

postgresql.host=192.168.1.100
postgresql.port=5432
postgresql.version=16
postgresql.user=admin
postgresql.password=your_password
```

| Key | Description |
|-----|-------------|
| `<type>.host` | 数据库所在机器 IP |
| `<type>.port` | 数据库端口 |
| `<type>.version` | 数据库版本（如 8.0 / 16 / 23ai） |
| `<type>.user` | 数据库登录用户名 |
| `<type>.password` | 数据库登录密码 |
| `<type>.ssl.ca` | SSL CA 证书路径（可选，配置后自动启用 SSL） |
| `<type>.ssl.cert` | SSL 客户端证书路径（可选） |
| `<type>.ssl.key` | SSL 客户端私钥路径（可选） |

SSL 证书文件放在 `certs/<type>/` 目录下，整个 `certs/` 目录不提交 Git。

### 3. 容器连接信息（AI 模块记录）

`db-connection.json` 位于每个模块 `db-<type>/src/main/resources/` 下，AI Skill 创建容器后写入，供记录确认，**Java 模块不读取**：

```json
{
  "host": "192.168.1.100",
  "port": 3306,
  "type": "mysql",
  "version": "8.0",
  "containerName": "mysql-test"
}
```
