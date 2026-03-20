# VIBE-CHECKLIST.md — AI 输出六关验收门禁

## 【第一关】意图对齐
- [ ] AI 输出是否解决了 intent.md 里的核心业务问题？
- [ ] intent.md 中的 Anti-Goals 有没有被违反？（逐条核对）
- [ ] AI 有没有发明 intent.md 未定义的业务规则？

## 【第二关】上下文遗漏
- [ ] state-machines.yaml 中所有状态都被正确处理了？
- [ ] 命名与 CLAUDE.md 完全一致？
- [ ] 没有多出 openapi.yaml 未定义的字段？

## 【第三关】AI 幻觉风险
- [ ] 没有自行发明 openapi.yaml 里没有的字段或接口？
- [ ] 没有硬编码的魔法数字（应引用 rule-engine.md 的 Rule Key）？
- [ ] 注释描述的逻辑和代码实际实现一致？

## 【第四关】SuperSonic 项目专项
- [ ] 跨模块通信使用 Spring ApplicationEvent？（禁止 reflection）
- [ ] 新 DO 类包含 tenant_id 字段？（排除表除外）
- [ ] 依赖注入使用 @RequiredArgsConstructor + private final？（禁止 @Autowired）
- [ ] SPI 扩展在 META-INF/spring.factories 注册？
- [ ] 前端日期格式使用 dayjs(value).format('YYYY-MM-DD HH:mm:ss')？
- [ ] MySQL 迁移脚本避免使用 `ADD COLUMN IF NOT EXISTS`？
- [ ] Flyway 迁移同时提供 MySQL 和 PostgreSQL 版本？

## 【第五关】代码质量底线
- [ ] 没有 e.printStackTrace()？
- [ ] 所有 TODO 注释有对应 Issue？
- [ ] 没有 @SuppressWarnings("all")？
- [ ] Controller 中不含业务逻辑（if/else 业务判断）？
- [ ] 没有 Class.forName 跨模块反射调用？

## 【第六关】重构信号检测
- [ ] 这个类的职责超过一个了吗？（单一职责检查）
- [ ] 有没有超过 20 行的 Service 方法？（方法过长信号）
- [ ] 有没有超过 3 层的 if 嵌套？（复杂度过高信号）
- [ ] 有没有重复出现 3 次以上的代码模式？（重复代码信号）
- [ ] 复杂逻辑是否已标注 @AiGenerated 注解？
- [ ] Scratchpad 推理链中标注为「不确定」的部分是否已标注？

## 【附加】测试覆盖验收
- [ ] 是否同步生成了测试文件？
- [ ] Anti-Goals 中每条规则是否有对应 @Test 方法？
- [ ] 所有非法状态跳跃是否有 assertThrows 测试？

---

检查结果：日期：____ 版本：____ 检查人：____

问题数：严重 __ / 一般 __ / 建议 __ | 测试通过：__/__
