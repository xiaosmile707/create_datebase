# AGENTS.md — db-lab 失败账本

> 每条规则都应可追溯到一个真实约束或失败场景，而非通用最佳实践。

---

## 架构边界

### 规则 1：skills/ 与代码模块完全隔离

```yaml
rule: skills/ 目录与 db-* 代码模块禁止互相引用
context: |
  skills/ 是 AI Agent 的技能定义（纯 Markdown），必须独立于 Java 代码运行。
  db-* 模块是 JDBC 客户端代码，必须独立于 skills 运行。
  两者的生命周期、运行环境、使用者完全不同。
enforcement: bin/check-boundary 脚本 + CI gate
consequence: |
  违反此规则将导致：
  - skills 无法被其他项目独立复用
  - 代码模块编译/运行依赖非代码资源
  - Agent 执行 skill 时产生错误的上下文假设
```

**具体禁止行为：**
- skills/*.md 中禁止引用 `db-*/src/` 下的任何文件路径或 Java 类名
- db-*/src/**/*.java 中禁止引用 `skills/` 下的任何文件
- db-*/src/main/resources/ 中禁止引用 skills/ 内容
- skills/*.md 中禁止假设代码模块的内部实现细节（如类名、方法签名）

**允许的共享：**
- `db-connection.json` 是两者的接口契约（格式由 skills 定义，代码模块消费）
- `certs/` 目录是两者的共享资源（skills 生成证书，代码模块使用证书）

---

### 规则 2：db-connection.json 是唯一接口契约

```yaml
rule: skills 与代码模块之间仅通过 db-connection.json 文件格式通信
context: skills 负责创建数据库并输出连接信息，代码模块负责读取连接信息
enforcement: 两者各自定义 schema，CI 验证兼容性
```

---

## 可用工具

```
bin/check-boundary    — 检查 skills/ 与代码模块的隔离性（结构性测试）
bin/check-stale       — 检查 skills/ 声明版本与 sql/ 目录的一致性（过期检测）
mvn compile           — 编译所有 Java 模块
mvn exec:java -pl db-<name> — 运行指定数据库客户端
```

## 禁止操作

```
- 在 skills/*.md 中 import/引用 Java 源码路径
- 在 Java 代码中读取 skills/ 目录下的任何文件
- 在 pom.xml 中将 skills/ 作为 resource 目录
- 删除 db-connection.json 的字段（只能新增，保持向后兼容）
```

---

## 模块职责

| 模块 | 职责 | 运行环境 |
|------|------|----------|
| `skills/` | AI Agent 技能定义，Docker 数据库部署流程 | AI Agent（纯文本解析） |
| `db-common/` | 公共 JDBC 工具类、配置加载 | JVM 8+ |
| `db-mysql/` | MySQL JDBC 客户端 + SQL 脚本 | JVM 8+ |
| `db-postgresql/` | PostgreSQL JDBC 客户端 + SQL 脚本 | JVM 8+ |
| `db-oracle/` | Oracle JDBC 客户端 + SQL 脚本 | JVM 8+ |
| `db-h2/` | H2 JDBC 客户端 + SQL 脚本 | JVM 8+ |
| `db-clickhouse/` | ClickHouse JDBC 客户端 + SQL 脚本 | JVM 8+ |
| `certs/` | SSL 证书存放（共享资源） | 文件系统 |

---

## 依赖方向

```
skills/ ──写入──> db-connection.json <──读取── db-*/src/main/resources/
skills/ ──生成──> certs/*            <──使用── db-*/src/main/java/
```

skills 和 db-* 之间无直接依赖，仅通过文件系统上的 `db-connection.json` 和 `certs/` 间接关联。

---

## 故障诊断流程

当 Agent 产出坏代码或违反约束时，按以下顺序排查（**不得跳步**）：

```yaml
step_1:
  action: 检查 AGENTS.md
  question: 该约束是否已记录？
  if_no: 添加规则到 AGENTS.md（Pillar 1 修复）
  if_yes: 进入 step_2

step_2:
  action: 检查 CI / 结构性测试
  question: bin/check-boundary 或 mvn compile 是否能捕获此违规？
  if_no: 添加检查到 bin/check-boundary 或新建结构性测试脚本（Pillar 2 修复）
  if_yes: 进入 step_3

step_3:
  action: 检查错误信息
  question: 失败输出是否包含修复建议（↳ 修复: ...）？
  if_no: 改进错误信息，添加修复指引（Pillar 2.1 修复）
  if_yes: harness 正确，排查 Agent 本身的问题
```

**绝对禁止：** 直接认定 "Agent 有 bug" 而不走完以上三步。大多数 Agent 错误是 harness 缺陷导致的。

---

## 版本一致性约束

```yaml
rule: skills/ 声明的支持版本 必须与 db-*/src/main/resources/sql/ 下的版本目录一一对应
context: |
  skills/*.md 中 "默认 Docker 参数" 表格声明了支持的数据库版本列表，
  db-* 模块的 sql/ 目录包含对应版本的 SQL 脚本。
  两者不一致会导致 Agent 创建了一个版本的数据库却找不到对应 SQL 脚本。
enforcement: bin/check-stale 脚本
consequence: |
  新增数据库版本时，必须同时：
  1. 更新 skills/<db>/SKILL.md 的版本支持声明
  2. 创建 db-<name>/src/main/resources/sql/<版本>/ 目录及四个 SQL 文件
```

当前版本映射：

| 数据库 | skills/ 声明版本 | sql/ 目录版本 |
|--------|-----------------|---------------|
| MySQL | 8.0 / 5.7 | 8.0 / 5.7 |
| PostgreSQL | 16 / 15 / 14 | 16 / 15 / 14 |
| Oracle | 23ai / 21c | 23ai / 21c |
| ClickHouse | 21.8 / 22.8 | 21.8 / 22.8 |
| H2 | 2.x | default |
