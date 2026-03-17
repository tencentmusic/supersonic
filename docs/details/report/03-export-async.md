---
status: implemented
module: headless/server
key-files:
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/ExportTaskServiceImpl.java
  - headless/server/src/main/java/com/tencent/supersonic/headless/server/service/impl/DownloadServiceImpl.java
depends-on:
  - docs/details/report/02-execution-engine.md
---

# 导出与异步任务

## 目标

支持多种数据输出方式（Web 表格展示、同步导出、异步导出、API 返回），并通过同步/异步分流机制保护服务稳定性，防止大批量导出导致 OOM 或线程池耗尽。

## 当前状态

已上线。同步导出（Excel/CSV）、异步导出（`ExportTaskServiceImpl` + Export Worker 线程池 + 任务中心 UI）、推送渠道均已实现。分流策略（EXPLAIN 行数估算）已实现。

## 设计决策

**问题背景**：多个用户同时发起大数据量导出时，同步处理会导致：

| 风险 | 原因 | 后果 |
|------|------|------|
| 堆内存溢出（OOM） | ResultSet 全量加载 + Excel 对象模型 | JVM 崩溃 |
| 数据库连接耗尽 | 长查询持有 JDBC 连接 | 业务请求阻塞 |
| Web 线程饿死 | 同步导出占用 Tomcat 线程 | 页面无响应 |

**分流策略**：

```
用户发起导出请求
    │
    ├── EXPLAIN 预估行数 ≤ 阈值（默认 5000）
    │       → 同步下载（当前模式，体验好）
    │
    └── EXPLAIN 预估行数 > 阈值
            → 异步任务（返回任务 ID，后台执行）
            → 用户通过"任务中心"查看进度并下载
```

Export Worker 线程池与 Web 线程池、Quartz 调度线程池**完全独立**，最多 3 个并发导出任务，不影响其他业务请求。

**内存控制**：JDBC `fetchSize=1000` 流式读取 + EasyExcel 流式写入，单任务内存占用固定 ~50MB，与数据量无关。

## 接口契约

```
POST   /api/v1/exportTasks
    → 提交导出任务，返回任务 ID 或直接返回文件流（同步/异步分流）

GET    /api/v1/exportTasks
    → 查询当前用户的导出任务列表（?pageSize=20&pageToken=...）

GET    /api/v1/exportTasks/{taskId}
    → 查询任务状态

DELETE /api/v1/exportTasks/{taskId}
    → 取消/删除任务

GET    /api/v1/exportTasks/{taskId}:download
    → 下载结果文件（自定义方法，GET，只读操作）
```

## 数据模型

```
s2_export_task（导出任务）
├── id
├── task_name             → 任务名称（自动生成："GMV日报导出_20260203"）
├── user_id               → 发起用户
├── dataset_id            → 关联数据集
├── query_config          → JSON：完整的 QueryStructReq
├── output_format         → EXCEL / CSV
├── status                → PENDING / RUNNING / SUCCESS / FAILED / EXPIRED
├── file_location         → 结果文件路径（本地目录或 OSS URL）
├── file_size             → 文件大小（字节）
├── row_count             → 导出行数
├── error_message
├── created_at
├── expire_time           → 文件过期时间（默认 created_at + 7 天）
└── tenant_id
```

ExportTask 状态机：

```
PENDING → RUNNING → SUCCESS
                 └→ FAILED
SUCCESS/FAILED → EXPIRED（定时清理后）
```

## 实现要点

### 异步导出流程

```
1. 用户提交导出申请 → 后端创建 s2_export_task（status=PENDING），返回任务 ID
2. Export Worker 线程池拉取任务
   → 设置 TenantContext + 权限上下文
   → JDBC fetchSize=1000 流式读取
   → EasyExcel streaming write 逐行写入临时文件
   → 单任务内存占用固定 ~50MB，与数据量无关
3. 写入完成 → 文件存至导出目录（本地 / OSS）→ status=SUCCESS
4. 用户通过"任务中心"下载文件
5. 定时清理过期文件（默认 7 天）
```

### 内存控制要点

| 环节 | 措施 | 说明 |
|------|------|------|
| JDBC 读取 | `fetchSize=1000` | 流式读取，不一次性加载全部 ResultSet |
| Excel 写入 | EasyExcel `ExcelWriter` 流式模式 | 逐行写入磁盘，不在内存构建完整 Workbook |
| CSV 写入 | `BufferedWriter` 直写 | 零对象模型开销 |
| 并发控制 | Export Worker 线程数限制 | 最多同时执行 3 个导出任务 |

### 配置参数

```yaml
supersonic:
  export:
    worker-threads: 3                     # Export Worker 线程数
    max-queue-size: 50                    # 等待队列上限
    sync-threshold: 5000                  # 同步/异步分流阈值（行数）
    file-expire-days: 7                   # 导出文件保留天数
    storage-type: local                   # local / oss
    local-dir: ${java.io.tmpdir}/supersonic-export
```

### 支持的输出方式

| 输出方式 | 状态 | 实现路径 |
|---------|------|---------|
| Web 表格展示 | ✅ 已实现 | `SemanticQueryResp` → 前端 ProTable 渲染 |
| Excel 导出（同步） | ✅ 已实现 | `DownloadServiceImpl` + EasyExcel 4.0.3 |
| CSV 导出 | ✅ 已实现 | `ReportExecutionOrchestrator.writeCsv()` + `ExportTaskServiceImpl.writeCsv()` |
| JSON API | ✅ 已实现 | `SemanticQueryResp` 直接返回 JSON |
| 大数据量异步导出 | ✅ 已实现 | `ExportTaskServiceImpl` + 任务中心 |
| 飞书/钉钉/邮件推送 | ✅ 已实现 | `ReportDeliveryService` + 五种渠道 |

### 前端任务中心（ExportTaskCenter）

建议以全局抽屉（Drawer）形式呈现，不占用独立路由：

| 列 | 说明 |
|----|------|
| 任务名称 | `task_name` |
| 格式 | Excel / CSV |
| 状态 | PENDING(灰) / RUNNING(蓝) / SUCCESS(绿) / FAILED(红) / EXPIRED(灰) |
| 文件大小 | `file_size`（SUCCESS 时显示） |
| 创建时间 | `created_at` |
| 过期时间 | `expire_time` |
| 操作 | 下载（SUCCESS）/ 重试（FAILED）/ 取消（PENDING） |

## 待办

- OSS 存储后端（当前仅支持本地目录，OSS 配置项预留但未实现）
- 导出文件分片下载支持（超大文件 Range 请求）
- 任务中心全局入口（Header 铃铛图标或独立 Drawer 入口）
