# db-lab 后续改动计划

> 生成日期：2026-05-07
> 状态：规划中

---

## 已完成的本次改动

- [x] **SLF4J + Logback 日志框架** — 替换全局 System.out/err 为分级日志（根 pom + db-common/pom + logback.xml）
- [x] **多源配置加载** — ConfigLoader 支持 classpath → 文件系统 fallback 读取 db-connection.json
- [x] **密码安全加固** — 环境变量 `DB_{TYPE}_PASSWORD` 优先 → JSON `password` 字段 fallback → 空字符串兜底
- [x] **SSL 支持激活** — `getSslConfig()` 从 JSON `ssl` 字段读取证书路径，不再硬编码 null
- [x] **配置简化** — 删除 db-secret.properties，所有配置统一到 db-connection.json
- [x] **事务回滚 + 失败清理** — DatabaseBootstrap 事务包裹 SQL 执行，失败回滚并执行 cleanup.sql
- [x] **错误控制增强** — SqlRunner 新增 `stopOnError` 参数，默认抛出异常确保事务正确回滚
- [x] **冗余注解删除** — 移除 ConfigLoader 中与 JSON 键名一致的 `@JSONField` 注解
- [x] **cleanup.sql 全覆盖** — 所有数据库模块的所有版本均有清理脚本

---

## TODO: 待补充的测试代码

### 1. ConfigLoader 单元测试
- 测试 classpath JSON 加载
- 测试文件系统 JSON fallback
- 测试环境变量密码覆盖 JSON 中的 password
- 测试 JSON password 字段读取
- 测试 JSON ssl 字段读取
- 测试 JSON 格式错误时的容错

### 2. SqlRunner 单元测试
- 测试 splitStatements() 各种边界情况（引号内分号、转义引号、空语句、注释行）
- 测试 stopOnError=true 时抛出异常
- 测试 stopOnError=false 时记录错误并继续
- 测试资源文件不存在时的异常

### 3. DatabaseBootstrap 集成测试
- 测试正常流程（连接 → schema → test → seed → 提交）
- 测试 schema 失败时回滚 + 清理
- 测试 seed 失败时回滚 + 清理
- 测试连接重试机制

---

## TODO: 后续优化方向

### 性能优化
- [ ] **连接池集成**（HikariCP）— 目前测试场景不需要，正式环境考虑
- [ ] **Statement 复用** — SqlRunner 中每次创建新 Statement，可池化复用
- [ ] **SQL 批量执行** — seed.sql 多条 INSERT 可合并为 batch

### 代码质量
- [ ] **日志脱敏** — 密码脱敏处理，避免日志泄露
- [ ] **配置热加载** — 支持运行时重载 db-connection.json

### 测试
- [ ] **全模块编译验证** — CI 流水线中自动 mvn compile

---

## 文件清单

| 文件 | 状态 |
|------|------|
| `pom.xml` | 已修改 |
| `db-common/pom.xml` | 已修改 |
| `db-common/src/main/resources/logback.xml` | 新建 |
| `db-common/.../config/ConfigLoader.java` | 已重写 |
| `db-common/.../runner/DatabaseBootstrap.java` | 已修改 |
| `db-common/.../runner/SqlRunner.java` | 已修改 |
| `db-mysql/src/main/resources/sql/8.0/cleanup.sql` | 新建 |
| `db-mysql/src/main/resources/sql/5.7/cleanup.sql` | 新建 |
| `db-postgresql/src/main/resources/sql/14/cleanup.sql` | 新建 |
| `db-postgresql/src/main/resources/sql/15/cleanup.sql` | 新建 |
| `db-postgresql/src/main/resources/sql/16/cleanup.sql` | 新建 |
| `db-oracle/src/main/resources/sql/21c/cleanup.sql` | 新建 |
| `db-oracle/src/main/resources/sql/23ai/cleanup.sql` | 新建 |
| `db-h2/src/main/resources/sql/default/cleanup.sql` | 新建 |
