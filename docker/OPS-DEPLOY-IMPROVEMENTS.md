# 运维部署改进说明

本文档记录已实施的部署/运维改进及后续可选优化建议。

## 已实施的改进

### 1. Docker Compose：应用健康检查

- **问题**：`supersonic_standalone` 未配置 healthcheck，编排无法判断应用是否就绪或异常。
- **改动**：为 `supersonic_standalone` 增加 healthcheck，基于 `/actuator/health`，便于：
  - `depends_on: condition: service_healthy` 的后续扩展；
  - 容器平台（如 K8s）就绪/存活探测。
- **配置**：`interval: 15s`，`start_period: 90s`（给应用足够启动时间），镜像中已安装 `curl` 用于探测。

### 2. Docker：容器内 Java 进程接收 SIGTERM（优雅退出）

- **问题**：原 CMD 为 `daemon.sh restart ... && tail -f /dev/null`，PID 1 为 `tail`，`docker stop` 只向 PID 1 发 SIGTERM，Java 子进程收不到信号，无法优雅关闭。
- **改动**：
  - 在 `supersonic-daemon.sh` 中新增 **`run`** 命令：以前台方式执行 Java（`exec java ...`），使 Java 进程成为 PID 1。
  - Dockerfile CMD 改为：`bin/supersonic-daemon.sh run standalone`，数据库 profile 由环境变量 `S2_DB_TYPE` 决定。
- **效果**：`docker stop` / K8s 终止 Pod 时，Java 能收到 SIGTERM，配合 Spring Boot 优雅关闭，减少请求中断和资源强杀。

### 3. 应用配置：显式优雅关闭与超时

- **改动**（`application.yaml`）：
  - `server.shutdown: graceful`：显式开启 Web 服务器优雅关闭（与 Spring Boot 3 默认一致，便于运维理解）。
  - `spring.lifecycle.timeout-per-shutdown-phase: 30s`：关闭阶段最长等待时间。
- **说明**：Spring Boot 3 默认已是 graceful，此处主要为配置可读与可调。

### 4. 自定义线程池优雅关闭

- **问题**：`ThreadPoolConfig` 中定义的 `ThreadPoolExecutor` Bean 为原生 JDK 线程池，Spring 不会自动在上下文关闭时调用 `shutdown()`。
- **改动**：新增 `ExecutorGracefulShutdown` 组件，实现 `DisposableBean`，在上下文销毁时对所有自定义线程池执行 `shutdown()` + `awaitTermination()`，超时后 `shutdownNow()`，避免进程退出时仍有任务在跑。

---

## 后续可选优化建议

| 项 | 说明 |
|----|------|
| **生产日志级别** | 在 `application-prd.yaml` 中显式设置 `logging.level.root: INFO`（或按包调整），避免生产环境默认继承 dev 的 DEBUG。 |
| **.env.example 与编排一致** | 当前 docker-compose 默认使用 Postgres，`.env.example` 中为 MySQL；可补充注释说明“默认编排为 Postgres”，或增加 `docker-compose.mysql.yml` 示例。 |
| **K8s 就绪/存活探针** | 若提供 Kubernetes 部署，建议使用 `readinessProbe` / `livenessProbe` 指向 `httpGet: path: /actuator/health`，与当前 Docker healthcheck 一致。 |
| **资源 limit 与 JVM** | 若在 K8s 中为 Pod 设置 memory limit，建议同时配置 JVM `-Xmx` 等，使其略小于容器 limit，避免 OOMKilled（当前 Dockerfile 已使用 `-Xmx2048m`，与 compose 中 2048M 一致）。 |
| **Actuator 安全** | 生产环境若将 `/actuator` 暴露到公网，建议通过 Spring Security 或网关限制访问，或仅暴露 `health`。 |

---

## 本地/CI 验证建议

- **Docker 构建与启动**：  
  `docker build -f docker/Dockerfile -t supersonic:local . && docker run --rm -e S2_DB_TYPE=h2 -p 9080:9080 supersonic:local`  
  使用 H2 时无需外置 DB，便于快速验证启动与 healthcheck。
- **优雅关闭**：在容器运行后执行 `docker stop <id>`，观察日志中是否有 Spring 的 “Completing in-flight requests” 及线程池关闭日志，且无 `kill -9` 导致的堆栈。
- **健康检查**：`curl -s http://localhost:9080/actuator/health` 应返回 `{"status":"UP",...}`。
