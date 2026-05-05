# AI 模块 — Agent Skill

通过 AI Agent Skill 实现一键远程创建数据库容器，免去手动 Docker 操作。

→ [返回主 README](../README.md)

## 概述

本模块包含一组 AI Agent Skill，每个 Skill 对应一种数据库。AI Agent 加载 Skill 后自动完成：
1. SSH 远程登录目标机器
2. Docker 拉取镜像并启动容器
3. 健康检查等待数据库就绪
4. 将连接信息写入对应模块的 `db-<type>/src/main/resources/db-connection.json`

## Skill 列表

| Skill | 数据库 | 默认版本 | 默认端口 | 文档 |
|-------|--------|----------|----------|------|
| `mysql` | MySQL | 8.0（支持 8.0 / 5.7） | 3306 | [SKILL.md](mysql/SKILL.md) |
| `postgresql` | PostgreSQL | 16（支持 16 / 15 / 14） | 5432 | [SKILL.md](postgresql/SKILL.md) |
| `oracle` | Oracle | 23ai（支持 23ai / 21c / 19c） | 1521 | [SKILL.md](oracle/SKILL.md) |
| `h2` | H2 嵌入式 | 2.x（内嵌） | — | [SKILL.md](h2/SKILL.md) |

## 使用方式

在 AI Agent 对话中用自然语言发起：

```
帮我创建一台 MySQL 8.0，密码用 root123
```

```
在 192.168.1.100 上起一个 PostgreSQL，端口用 5433
```

## 工作流程

```
用户指令
  ↓
AI Agent 加载对应 SKILL.md
  ↓
读取 ssh-config.properties（SSH 凭据）
  ↓
展示默认 Docker 参数，用户可覆盖
  ↓
SSH → docker run → health check
  ↓
写入 db-connection.json
  ↓
Java 模块从 db-secret.properties 读取连接信息执行测试
```

## 配置文件

本模块依赖 `ssh-config.properties`（不提交 Git），首次使用请复制示例文件：

```bash
cp ssh-config.properties.example ssh-config.properties
```

格式：

```properties
ssh.host=192.168.1.100
ssh.port=22
ssh.user=root
ssh.password=your_password
```

## 添加新数据库 Skill

1. 在 `.opencode/skills/` 下新建目录，目录名即 Skill 名
2. 创建 `SKILL.md`，参考已有 Skill 模板
3. 更新本 README 的 Skill 列表

模板结构见 [mysql/SKILL.md](mysql/SKILL.md)。
