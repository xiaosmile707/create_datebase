# Docker 数据库公共流程

所有需要 Docker 容器的数据库 Skill（MySQL、PostgreSQL、Oracle）遵循此统一流程。

## 通用执行步骤

### 1. 读取 SSH 凭据
从项目根目录 `ssh-config.properties` 中读取：
- `ssh.host` — 目标机器 IP
- `ssh.port` — SSH 端口（默认 22）
- `ssh.user` — SSH 用户名
- `ssh.password` — SSH 密码

若文件不存在或缺少字段，询问用户补充。

### 2. 确认参数
向用户展示默认参数（来自各数据库 Skill 的声明），允许用户修改：
- 数据库版本
- 端口映射
- 容器名
- 密码
- 内存限制
- 数据库名、用户名

### 3. SSH 连接目标机器
```bash
ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -p <ssh.port> <ssh.user>@<ssh.host>
```

### 4. 检查端口占用
启动容器前，检查目标端口是否已被占用：
```bash
ss -tlnp | grep :<目标端口>
```
若端口被占用，提示用户更换端口映射或手动释放端口。

### 5. 清理旧容器（如存在）
```bash
docker rm -f <容器名> 2>/dev/null || true
```

### 6. 拉取镜像并创建容器
```bash
docker pull <镜像>
docker run -d \
  --name <容器名> \
  -p <端口映射> \
  -e <环境变量> \
  --memory=<内存限制> \
  <镜像>
```

若 `docker pull` 失败（网络问题、镜像不存在等），向用户报告错误，不要继续。

### 7. 健康检查
循环检查数据库是否就绪，具体命令见各数据库 Skill。

若超时仍未就绪，执行以下诊断：
```bash
docker logs --tail 50 <容器名>
```
将日志摘要展示给用户，根据日志判断原因（内存不足、端口冲突、镜像问题等）。

### 8. 创建数据库与用户
具体命令见各数据库 Skill。

若创建失败，检查容器是否仍在运行，输出错误信息。

### 9. 写入 db-connection.json
将连接信息写入对应模块的 `db-connection.json`，格式见各数据库 Skill。

## 异常处理

| 场景 | 处理方式 |
|------|----------|
| SSH 连接失败 | 检查 `ssh-config.properties` 中的 host/port/user/password 是否正确，提示用户修正 |
| Docker 未安装 | 提示用户目标机器需安装 Docker |
| 镜像拉取失败 | 检查镜像名和网络连接，向用户报告具体错误 |
| 端口被占用 | 提示用户更换端口映射，或询问是否停用占用端口的进程 |
| 容器启动后立即退出 | 执行 `docker logs <容器名>`，根据日志判断原因 |
| 健康检查超时 | 检查容器日志，提示用户可能需要等待更长时间或增加内存 |
| 内存不足 | 提示用户减小 `--memory` 限制或更换目标机器 |
| 创建数据库/用户失败 | 检查容器运行状态，输出 docker exec 错误信息 |
