# WORKFLOW-KNOWHOW.md — SuperSonic AI 原生开发工作流

## 四层架构全景

| 层级 | 解决的核心问题 | 核心文件 |
|------|---------------|---------|
| **工程层** | AI 生成代码有结构可循 | CLAUDE.md / openapi.yaml / entities.md |
| **意图层** | AI 理解「为什么」后再生成代码 | intent.md / VIBE-CHECKLIST / rule-engine.md |
| **对话层** | SPEC 由对话生成，上下文最小化，跨会话不失忆 | SPEC-DISCOVERY / CONTEXT-SLICE / SCRATCHPAD |
| **质量层** | 代码可测试可重构，Prompt 可版本化可审计 | TEST-SPEC / PROMPT-VERSIONS / PROMPT-GOLDEN-TESTS |

## 完整开发流程

```
【阶段 0】需求触发
    ↓
【阶段 1】SPEC 对话生成
    SPEC-DISCOVERY Phase 1：AI 追问 10 轮，暴露业务边界
    SPEC-DISCOVERY Phase 2：AI 扮演破坏者，挖 Anti-Goals
    SPEC-DISCOVERY Phase 3：AI 扮演合规审计员，审查数据敏感级别
    → 生成 intent.md（AI 起草，人工审定）
    → 初始化 SCRATCHPAD（含 SPEC 探索推理链）
    ↓
【阶段 2】Architect Agent
    读 SCRATCHPAD + intent.md，输出《上下文恢复确认》
    生成 openapi.yaml + state-machines.yaml + ADR
    填写 Scratchpad（含推理链 + Prompt 版本号）
    ↓
【阶段 3】Coding Agent
    查 CONTEXT-SLICE，选最小上下文切片
    确认使用 PROMPT-VERSIONS/ 中的当前推荐版本
    生成代码 + 测试文件（TEST-SPEC 规范）← 强制
    复杂逻辑标注 @AiGenerated ← 强制
    过 VIBE-CHECKLIST 六关自检
    填写 Scratchpad（含推理链 + Prompt 效果反馈）
    ↓
【阶段 4】Review Agent
    读 Scratchpad + 切片 D
    以 intent.md Anti-Goals 为基准执行 Review
    ADR 合规性检查 + 测试覆盖完整性检查 + Prompt 版本可追溯性检查
    输出三级问题报告
    ↓
【阶段 5】人工 Review + 部署
    检查 @AiGenerated 标注，决定是否人工重构
    运行 PROMPT-GOLDEN-TESTS，更新 CHANGELOG
    Scratchpad 更新（记录上线决策）
```

## SuperSonic 项目特殊约束

- **跨模块通信**：必须使用 Spring `ApplicationEvent`，严禁 reflection
- **SPI 注册**：新扩展必须在 `META-INF/spring.factories` 注册
- **多租户隔离**：新表必须包含 `tenant_id` 字段（排除表除外）
- **前端日期格式**：统一 `dayjs(value).format('YYYY-MM-DD HH:mm:ss')`
- **依赖注入**：`@RequiredArgsConstructor` + `private final`，禁止 `@Autowired`（`@Lazy`/`@Qualifier` 除外）

## 文件速查表

| 文件 | 核心作用 | 使用时机 |
|------|---------|---------|
| intent.md | 业务意图 + Anti-Goals（最高优先级） | 每次任务第一优先读取 |
| rule-engine.md | 规则 Key 定义，禁止 AI 硬编码 | 涉及规则判断时 |
| VIBE-CHECKLIST.md | AI 输出六关验收门禁 | 每次 AI 生成代码后 |
| AGENT-PROTOCOL.md | Agent 交接契约（含 ADR 守护） | 每次阶段交接 |
| SPEC-DISCOVERY.md | SPEC 四阶段对话生成流程 | 创建/修改 intent.md 前 |
| CONTEXT-SLICE.md | 任务上下文切片规则 | 每次任务启动时查表 |
| SCRATCHPAD.md | 跨会话记录（含推理链） | 每次会话结束时填写 |
| TEST-SPEC.md | 测试规范 + @AiGenerated 注解规范 | Coding Agent 生成任务时 |
| PROMPT-VERSIONS/ | Prompt 版本历史 + CHANGELOG | 修改/使用 Prompt 时 |
| PROMPT-GOLDEN-TESTS.md | Prompt 效果基准测试集 | 每次修改 Prompt 后 |
| PROMPT-LIBRARY.md | Prompt 索引（含版本号 + 通过率） | 按任务类型选用 |
| CLAUDE.md | 技术规范 + AI 行为约束 | 被切片引用 |
