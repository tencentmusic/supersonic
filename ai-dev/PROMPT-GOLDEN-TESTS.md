# PROMPT-GOLDEN-TESTS.md — Prompt 效果基准测试

## 使用流程

1. 修改 PROMPT-VERSIONS/ 中任何 Prompt 文件前，先记录当前版本号
2. 修改后，用下方对应的 Golden Test 输入跑一次
3. 对比输出特征是否全部通过
4. 将结果记入对应 CHANGELOG.md
5. 通过率低于上一版本时，禁止升级为当前默认版本

---

## Golden Test A：generate-api

### 标准输入（每次测试使用完全相同的输入）

intent.md 片段：
- Anti-Goals: 不允许跨租户访问数据
- 数据敏感级别: userId=高敏（日志脱敏）

openapi.yaml 片段：
- POST /api/semantic/model/create
- requestBody: { name: string, domainId: long, tenantId: string }

### 预期输出特征（逐条判断）
- [ ] T-A-01: Controller 方法体不超过 10 行
- [ ] T-A-02: 入参 DTO 包含 @Valid + @NotBlank/@NotNull 约束注解
- [ ] T-A-03: 依赖注入使用 @RequiredArgsConstructor + private final
- [ ] T-A-04: DO 类包含 tenantId 字段
- [ ] T-A-05: 没有 @Autowired 字段注入
- [ ] T-A-06: 包含《生成决策说明》且提到了 Anti-Goals
- [ ] T-A-07: 同时生成了 Service 接口 + ServiceImpl 骨架
- [ ] T-A-08: 测试骨架中包含针对「跨租户访问」的测试方法名

### 通过率历史

| 版本 | 日期 | 通过数 | 总数 | 通过率 |
|------|------|--------|------|--------|
| v1.0 | 初始 | - | 8 | 待测 |

---

## Golden Test B：code-review

### 标准输入
待 Review 代码：包含以下已知问题的样本代码
- @Autowired 字段注入（应为构造器注入）
- Controller 中包含 if (status == APPROVED) 业务判断
- 跨模块使用 Class.forName 反射调用
- DO 类缺少 tenantId 字段

### 预期输出特征
- [ ] T-B-01: 发现并标注 @Autowired 问题为「严重」级
- [ ] T-B-02: 发现 Controller 业务逻辑问题为「严重」级
- [ ] T-B-03: 发现 Class.forName 反射问题为「严重」级
- [ ] T-B-04: 发现 tenantId 缺失问题为「严重」级
- [ ] T-B-05: 输出按「严重 / 一般 / 建议」三级分类

---

## Golden Test C：generate-test

### 标准输入
state-machines.yaml: DRAFT→ONLINE/OFFLINE→DELETED
intent.md Anti-Goals:
- 不允许未发布的模型被数据集引用
- 删除模型时必须检查关联数据集

### 预期输出特征
- [ ] T-C-01: 包含至少 3 个合法流转测试方法
- [ ] T-C-02: 包含「DRAFT→DELETED」非法跳跃测试，使用 assertThrows
- [ ] T-C-03: 包含「未发布模型被引用」的业务规则测试
- [ ] T-C-04: 测试方法名使用中文 @DisplayName 描述业务语义
- [ ] T-C-05: 没有空测试方法或 @Disabled 注解
