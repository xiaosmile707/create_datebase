# Skills 模块 — 独立可复用的数据库 Skill 集合

通过 AI Agent Skill 实现一键远程创建数据库容器，免去手动 Docker 操作。
本模块与具体项目解耦，可独立复制到任意 Java 项目中使用。

## 概述

本模块包含一组 AI Agent Skill，每个 Skill 对应一种数据库。AI Agent 加载 Skill 后自动完成：
1. SSH 远程登录目标机器
2. Docker 拉取镜像并启动容器
3. 健康检查等待数据库就绪
4. 创建数据库与用户
5. 将连接信息写入 `{output_dir}/db-connection.json`

## 独立使用方式

### 1. 复制 skills 目录
将整个 `skills/` 目录复制到你的项目中：
```
your-project/
├── skills/                    ← 复制此目录
│   ├── _common.md
│   ├── ssh-config.properties
│   ├── mysql/SKILL.md
│   └── ...
└── ...
```

### 2. 配置 SSH 凭据
编辑 `skills/ssh-config.properties`，填入目标 Docker 宿主机的 SSH 信息：
```properties
ssh.host=192.168.1.100
ssh.port=22
ssh.user=root
ssh.password=your_password
```

### 3. AI Agent 发起指令
在 AI Agent 对话中用自然语言发起，可指定输出目录：
```
帮我创建一台 MySQL 8.0，密码用 root123，输出到 /my-project/config/
```

### 4. Java 侧读取连接信息
Skill 生成的 `db-connection.json` 已包含完整连接信息（host/port/user/password/database），Java 模块直接使用即可：
```java
// 读取 Skill 生成的连接信息（路径可由用户指定输出目录）
ConfigLoader config = new ConfigLoader("path/to/db-connection.json");
String host = config.getHost();
int port = config.getPort();
String user = config.getUser();
String password = config.getPassword(); // 优先环境变量，fallback JSON
String database = config.getDatabase();
```

## 输出路径优先级

`{output_dir}` 按以下优先级确定：
1. **用户明确指定** — 如 "输出到 /my-project/config/"
2. **默认值** — 当前工作目录 `./`

## Skill 列表

| Skill | 数据库 | 默认版本 | 默认端口 | 文档 |
|-------|--------|----------|----------|------|
| `mysql` | MySQL | 8.0（支持 8.0 / 5.7） | 3306 | [SKILL.md](mysql/SKILL.md) |
| `postgresql` | PostgreSQL | 16（支持 16 / 15 / 14） | 5432 | [SKILL.md](postgresql/SKILL.md) |
| `oracle` | Oracle | 23ai（支持 23ai / 21c） | 1521 | [SKILL.md](oracle/SKILL.md) |
| `h2` | H2 嵌入式 | 2.x（内嵌） | — | [SKILL.md](h2/SKILL.md) |

## 工作流程

```
用户指令
  ↓
AI Agent 加载对应 SKILL.md
  ↓
读取 skills/ssh-config.properties（SSH 凭据）
  ↓
展示默认 Docker 参数，用户可覆盖
  ↓
SSH → docker run → health check
  ↓
创建数据库与用户
  ↓
写入 {output_dir}/db-connection.json（含完整连接信息，密码可被环境变量覆盖）
  ↓
调用方 Java 模块通过 ConfigLoader 读取 db-connection.json 获取所有连接信息
```

## 设计原则

- **JSON 自包含**：`db-connection.json` 包含所有连接所需字段，Java 模块零额外配置即可连接
- **独立 SSH 配置**：skills 目录自带 `ssh-config.properties`，不依赖外部项目
- **可配置输出**：输出路径由用户指令指定，默认输出到当前工作目录

## 公共模板

所有 Docker 数据库 Skill 遵循统一流程，差异仅在数据库特定参数。详见 [_common.md](_common.md)。

## 添加新数据库 Skill

1. 在 `skills/` 下新建目录，目录名即 Skill 名
2. 创建 `SKILL.md`，参考已有 Skill 模板和 `_common.md`
3. 更新本 README 的 Skill 列表

模板结构见 [mysql/SKILL.md](mysql/SKILL.md)。
