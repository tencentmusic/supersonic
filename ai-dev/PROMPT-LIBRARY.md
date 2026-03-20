# PROMPT-LIBRARY.md
# 本文件为索引，具体 Prompt 内容见 PROMPT-VERSIONS/ 对应目录

## 使用规则

1. 每次使用前，先确认使用的是 PROMPT-VERSIONS/ 中的最新推荐版本
2. 使用后在 SCRATCHPAD 记录版本号和效果反馈
3. 发现 Prompt 问题时，在 CHANGELOG.md 中记录，不要直接修改 Library

---

## Prompt 索引

### P-01 生成 API 接口层
当前推荐版本：PROMPT-VERSIONS/generate-api/v1.0.md
Golden Test 最新通过率：待建立基准
上次更新：初始版本

### P-02 生成数据层（DO/Mapper/Service）
当前推荐版本：PROMPT-VERSIONS/generate-entity/v1.0.md
Golden Test 最新通过率：待建立基准
上次更新：2026-03-20（技能包创建，迁移脚本生成拆分至 P-06）

### P-03 生成测试套件
当前推荐版本：PROMPT-VERSIONS/generate-test/v1.0.md
Golden Test 最新通过率：待建立基准
上次更新：初始版本

### P-04 AI Code Review
当前推荐版本：PROMPT-VERSIONS/code-review/v1.0.md
Golden Test 最新通过率：待建立基准
上次更新：初始版本

### P-05 SPEC 生成（Phase 1~3）
当前推荐版本：见 SPEC-DISCOVERY.md（不做版本化，每次对话定制）

### P-06 生成 Flyway 迁移脚本（MySQL + PostgreSQL）
当前推荐版本：PROMPT-VERSIONS/generate-migration/v1.0.md
Golden Test 最新通过率：待建立基准
上次更新：2026-03-20（初始版本，从 generate-entity 拆分）
关键护栏：MySQL 禁用 `ADD COLUMN IF NOT EXISTS`；双方言文件强制同步

### P-07 生成前端页面（React/TypeScript/Ant Design Pro）
当前推荐版本：PROMPT-VERSIONS/generate-frontend/v1.0.md
Golden Test 最新通过率：待建立基准
上次更新：2026-03-20（初始版本）
关键护栏：时间戳强制 dayjs 格式化；禁止硬编码 API base URL；禁止 `any` 类型；禁止 `fetch()` 直调

---

## 版本降级程序

如果最新版本通过率低于上一版本：
1. 在 CHANGELOG.md 中标注「回退」
2. 将上一版本设为当前推荐版本
3. 分析原因后再尝试修复
