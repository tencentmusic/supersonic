# 前端反馈规范：空态、加载、错误

与 [商业化 SaaS 视觉与体验落地方案](./ui-commercial-saas-landing-plan.md) **阶段三**一致，供评审与迭代对照。

## 列表空态（业务数据为空）

- 使用 antd `Empty`，或封装组件 **`PageEmpty`**（`webapp/packages/supersonic-fe/src/components/PageEmpty`）。
- 文案：**一句行动导向**（例如「暂无调度任务」+ 主按钮「创建调度」）。
- 表格场景：优先 `locale.emptyText={<PageEmpty ... />}`，保持与表格宽度一致。

## 列表请求失败

- 统一 `message.error` 提示简短原因。
- **数据是否清空**（与全局 `request` 行为无关，由页面处理）：
  - **首次进入页面 / 第一次拉列表**：失败时建议 `setData([])`，避免展示上一次会话的脏数据。
  - **翻页、改筛选、手动刷新**：失败时建议 **保留** 当前 `data`，仅提示错误。
- 详细约定见 `webapp/packages/supersonic-fe/src/services/request.ts` 文件头注释。

## 整页不可用

- 使用 antd **`Result`**（含 `status`、`title`、`extra` 重试或返回）。
- 典型：无权限、租户未就绪、关键配置缺失；**不要**仅用表格空态表达整页错误。

## 加载

- 列表：`Table` 的 `loading` 或 `Spin` 包裹内容区；避免长时间白屏无反馈。
- 首屏大块内容：骨架屏或 `Spin` 与产品阶段三节奏一致即可。
