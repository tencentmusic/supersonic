# TEST-SPEC.md — AI 生成代码测试规范
# SuperSonic 演进式设计的质量守门

## 强制原则

1. 测试与代码同步生成：Coding Agent 收到任务时，测试是任务的一部分
2. 测试先于代码检视：Review Agent 必须先看测试，再看实现
3. Anti-Goals 必须有测试覆盖：每条 Anti-Goal 对应至少一个 @Test

---

## 分层测试强制要求

| 代码层 | 测试类型 | 最低覆盖率 | 框架 | 特殊要求 |
|--------|---------|-----------|------|---------|
| Controller | MockMvc 集成测试 | 80% | Spring Test | 每个接口至少一个成功用例 + 一个参数校验失败用例 |
| Service | 单元测试 | 90% | JUnit5 + Mockito | Anti-Goals 中每条规则必须有对应测试用例 |
| Mapper | SQL 验证测试 | 核心查询 100% | MyBatis Test | 包含边界值：空结果 / 最大记录数 |
| 状态流转 | 状态边界值测试 | 100% | JUnit5 | 合法流转 + 所有非法跳跃（必须抛出明确异常） |
| 规则引擎 | 规则单元测试 | 100% | JUnit5 | rule-engine.md 每个 Rule Key 对应一个测试 |

---

## SuperSonic 项目测试约定

- 测试位置：`launchers/standalone/src/test/java/`
- Controller 测试：使用 MockMvc + `@SpringBootTest`
- Service 测试：使用 Mockito mock 依赖
- 数据库测试：H2 内存数据库（默认配置）
- 多租户测试：测试前设置 `TenantContext.setTenantId("test-tenant")`，测试后清理

---

## 状态机测试规范（最严格）

### 必须覆盖的用例类型

合法流转测试（每条合法路径一个用例）：
```java
@Test void should_transit_from_SUBMIT_to_APPROVED() { ... }
@Test void should_transit_from_SUBMIT_to_REJECTED() { ... }
```

非法跳跃测试（每条非法路径一个用例，必须抛出异常）：
```java
@Test void should_throw_when_transit_from_INIT_to_DISBURSED() {
    assertThrows(IllegalStateTransitionException.class, () -> {
        service.updateStatus(entity, Status.DISBURSED);
    });
}
```

Anti-Goals 测试（intent.md 每条 Anti-Goal 对应一个）：
```java
// Anti-Goal: 不允许跨租户访问数据
@Test void should_reject_when_accessing_data_from_different_tenant() { ... }
```

---

## @AiGenerated 注解规范

当 AI 生成的代码存在以下情况时，必须标注：
1. 逻辑复杂但未经充分人工审查
2. 性能未经过基准测试
3. 依赖了 Mock 数据的临时实现
4. AI 推理链记录显示「不确定」的部分

```java
@AiGenerated(
    reason = "NL2SQL 解析逻辑待人工复核",
    generatedAt = "2025-03-06",
    reviewBy = "2025-Q2",
    scratchpadRef = "2025-03-06_接口开发"
)
public class QueryParserService { ... }
```

---

## 测试生成 Prompt（在 PROMPT-VERSIONS/generate-test/ 维护）

### 意图
为已生成的业务代码补全测试套件，确保 Anti-Goals 中每条规则都有对应的失败场景测试。

### 上下文加载（CONTEXT-SLICE 切片 G）
1. @ai-spec/domain/intent.md（Anti-Goals 全文）
2. @ai-spec/domain/state-machines.yaml
3. @ai-spec/engine/rule-engine.md
4. [待测试的代码文件]

### 验收条件
- [ ] 每条 Anti-Goal 都有对应的 @Test 方法，方法名包含业务语义
- [ ] 所有非法状态跳跃都有 assertThrows 测试
- [ ] Service 层覆盖率达到 90% 以上
- [ ] 没有 @Disabled 或空测试方法
- [ ] 测试使用 @DisplayName 包含中文业务描述
