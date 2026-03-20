# CONTEXT-SLICE.md — 任务上下文切片规则

## 原则
每次任务只加载最小必要上下文，减少 AI 幻觉概率，提高生成质量。

---

## 切片 A：API 接口生成
加载：
1. @ai-spec/domain/intent.md（Anti-Goals + 数据敏感级别）
2. @ai-spec/contracts/openapi.yaml（仅本次涉及路径）
3. @ai-spec/domain/state-machines.yaml
4. @CLAUDE.md
5. @TEST-SPEC.md（测试要求）

跳过：rule-engine.md、user-journey.md

---

## 切片 B：Service 业务逻辑生成
加载：
1. @ai-spec/domain/intent.md（完整）
2. @ai-spec/domain/state-machines.yaml
3. @ai-spec/engine/rule-engine.md
4. @CLAUDE.md

跳过：openapi.yaml、user-journey.md

---

## 切片 C：数据层（DO/Mapper）生成
加载：
1. @ai-spec/domain/entities.md
2. @CLAUDE.md（MyBatis-Plus 约定）
3. 已有的 DO 类示例

跳过：intent.md、openapi.yaml

---

## 切片 D：Review 任务
加载：
1. @ai-spec/domain/intent.md（Anti-Goals 全文）
2. @VIBE-CHECKLIST.md
3. @SCRATCHPAD.md（最近 3 条）
4. 待 Review 的代码 + 测试文件

跳过：openapi.yaml、entities.md

---

## 切片 E：前端页面生成
加载：
1. @ai-spec/contracts/openapi.yaml（对应 API）
2. @CLAUDE.md（前端约定部分）
3. 已有的同类页面组件（作为 pattern 参考）

跳过：intent.md、state-machines.yaml、rule-engine.md

---

## 切片 F：Flyway 迁移脚本
加载：
1. @ai-spec/domain/entities.md
2. @CLAUDE.md（Flyway 迁移约定）
3. 最新的迁移脚本示例（MySQL + PostgreSQL）

跳过：intent.md、openapi.yaml

---

## 切片 G：测试生成任务
加载：
1. @ai-spec/domain/intent.md（Anti-Goals 全文）
2. @ai-spec/domain/state-machines.yaml
3. @ai-spec/engine/rule-engine.md
4. @TEST-SPEC.md
5. [待测试的代码文件]

跳过：openapi.yaml、user-journey.md

---

## 切片 H：Prompt 版本审查
加载：
1. @PROMPT-VERSIONS/[Prompt名]/CHANGELOG.md
2. @PROMPT-GOLDEN-TESTS.md（对应测试用例）
3. [上次使用该 Prompt 生成的代码样本]

跳过：业务 spec 文件

---

## 切片 I：跨模块事件通信
加载：
1. @ai-spec/domain/intent.md（事件触发条件）
2. @CLAUDE.md（ApplicationEvent 约定）
3. 已有的 ApplicationEvent 实现示例

跳过：openapi.yaml、前端相关文件
