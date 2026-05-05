---
name: h2
description: 配置 H2 嵌入式数据库连接信息，无需 Docker，将连接信息写入 db-connection.json
---

## 前置条件
- H2 为嵌入式内存数据库，无需 Docker
- 无需 SSH 连接

## 默认参数

| 参数 | 默认值 | 用户可覆盖 |
|------|--------|:--:|
| 版本 | 2.x（内嵌） | ✅ |
| 模式 | embedded（内存模式） | ✅（可改为 file 模式） |
| 用户名 | sa | ✅ |
| 密码 | （空） | ✅ |
| JDBC URL | jdbc:h2:mem:testdb | ✅ |

## 执行步骤

### 1. 确认参数
向用户展示默认参数，允许修改：模式、用户名、密码、JDBC URL。

### 2. 写入 db-connection.json
将连接信息写入 `db-h2/src/main/resources/db-connection.json`：
```json
{
  "host": "localhost",
  "port": 0,
  "type": "h2",
  "version": "<version>",
  "containerName": "",
  "url": "jdbc:h2:mem:testdb"
}
```

注意：H2 无需 host/port/containerName，使用额外的 `url` 字段传递 JDBC 连接串。
