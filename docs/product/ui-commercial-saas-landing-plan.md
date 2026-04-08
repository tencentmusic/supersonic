# 商业化 SaaS 视觉与体验落地方案

本文将「更商业化 B2B SaaS」拆解为可迭代阶段，与现有技术栈（Ant Design Pro、[`webapp/packages/supersonic-fe`](../../webapp/packages/supersonic-fe)）对齐，避免另起视觉体系。

## 目标与原则

- **目标**：在不大改框架的前提下，提升品牌一致性、数据工作台专业感与关键路径可信度。
- **原则**：全局先统一 Token 与密度，再收敛少数高频页；新样式以现有页面为参照复制，不引入第二套设计系统（见项目 `CLAUDE.md`）。

## 主参考：Metabase 式产品气质（信息架构与视觉节奏）

**为何选 Metabase 作为首要参照（非复刻 UI）：**

- 产品主线与「运营问数 → 保存/模板 → 定时与分发」接近，**自助探索 + 资产沉淀** 的心智一致。
- 界面气质偏 **清爽、留白、卡片与列表分区明确**，适合 B2B 运营数据工作台，而非云监控「全屏高密度指标墙」。
- 技术栈仍为 **Ant Design**，只借鉴 **布局层级、列表/结果区主次、空状态与新建入口位置感**，不迁移 Metabase 的查询构建器交互。

**刻意不对标的场景：** 若以告警排障、SLO、执行链路为主，可局部参考 Datadog / Grafana 类「高密度表格 + 强筛选」；**任务中心、调度历史** 可比问数首页略密，但仍与全站 Token 一致。

**官方产品（设计调研用）：** [Metabase](https://www.metabase.com/)（界面随版本迭代，以「浏览集合 / 已保存问题 / 结果页」区块节奏为准）。

## 阶段一：全局主题与组件密度（优先）

### 范围

- 统一 **主色、语义色、圆角、表格表头** 等，消除 `defaultSettings` 与 `ConfigProvider` 组件级 token 的漂移（例如当前 `colorPrimary` 与 `Button.colorPrimary` 不一致）。
- 明确 **Table / Form / Modal** 的默认密度（`size="middle"` 或 `small` 二选一全局倾向）。

### 关键文件

| 文件 | 作用 |
|------|------|
| [`webapp/packages/supersonic-fe/config/defaultSettings.ts`](../../webapp/packages/supersonic-fe/config/defaultSettings.ts) | ProLayout：`colorPrimary`、`layout`、`contentWidth` |
| [`webapp/packages/supersonic-fe/config/themeSettings.ts`](../../webapp/packages/supersonic-fe/config/themeSettings.ts) | Less 变量 + `configProviderTheme`（Ant Design 5 `components`） |
| [`webapp/packages/supersonic-fe/src/app.tsx`](../../webapp/packages/supersonic-fe/src/app.tsx) | 根节点 `ConfigProvider theme={...}` |

### 交付物

1. **Token 对照表**（一页纸即可）：主色、成功/警告/错误、背景层级、边框、圆角、Table 表头背景与字色。
2. **代码改动**：合并主色来源（建议以 `defaultSettings.colorPrimary` 为单一来源，同步到 `configProviderTheme` 与 Less 变量）。
3. **验收**：任意两页并排截图，按钮主色、链接色、Table 表头一致；无「一处 `#296DF3`、一处 `#3182ce`」混用。

**已落地（代码）**：`BRAND_PRIMARY` 见 [`defaultSettings.ts`](../../webapp/packages/supersonic-fe/config/defaultSettings.ts)；`configProviderTheme` 使用 Ant Design 5 `token` + 组件级 Table/Layout/Checkbox 等，工作区背景 `#f5f7fa`；Logo 主色与 `BRAND_PRIMARY` 对齐，见 [`app.tsx`](../../webapp/packages/supersonic-fe/src/app.tsx)、[`themeSettings.ts`](../../webapp/packages/supersonic-fe/config/themeSettings.ts)。

### 周期建议

0.5～1.5 人日（视回归范围而定）。

---

## 阶段二：高频页体验收敛（样板页）

### 选定页面（与路由一致）

| 路由模块 | 路径参考 | 收敛项 |
|----------|-----------|--------|
| 问数 / 助手 | `./ChatPage` | 侧栏/输入区间距、空状态、加载态 |
| 报表调度 | `./ReportSchedule` | PageContainer 标题区、表格列宽、操作列 |
| 任务中心 | `./TaskCenter` | 与调度页同一套表格/标签规范 |
| 推送配置 | `./DeliveryConfig` | 表单分组（Divider）、Modal 内字段对齐 |

（若人力紧，可先做 **ReportSchedule + TaskCenter + DeliveryConfig** 三块「运营工作台」闭环，Chat 次之。）

### 收敛规范（不写新组件库，只定规则）

- **页面骨架**：`PageContainer` 的 `title` / `breadcrumb` / `extra` 用法与 1～2 个已满意页面保持一致。
- **Metabase 式节奏（抽象）**：主内容区 **标题区 + 主操作（新建/筛选）+ 列表/表格** 自上而下；问数/结果页 **结果区视觉优先**，次要信息折叠或次级排版。
- **表格**：统一 `scroll`、操作列宽度、`pagination` 默认 `showSizeChanger` 是否开启。
- **表单**：Modal 内 `labelCol`/`wrapperCol` 与现有成熟页一致。
- **时间**：列表时间列统一 `dayjs(...).format('YYYY-MM-DD HH:mm:ss')`（项目既有约定）。

### 交付物

1. **样板页说明**：指定「以某某页为参照」，并注明 **Metabase 式** 的「层级与主次」是否达标（不要求像素级一致），后续新页只许跟随。
2. **上述页面的 PR**：仅布局/样式/文案，不改业务逻辑。
3. **验收**：三页之间切换无「另一种边距/另一种表头高度」的突兀感；问数/列表页无明显「监控台式」压迫密度（除非任务中心等明确为高密页）。

### 周期建议

2～4 人日（按页面数与历史债务浮动）。

---

## 阶段三：空状态、加载、错误统一

### 范围

- **空列表**：统一用 `Empty` + 一句行动导向文案（如「暂无调度任务，请创建」+ 主按钮跳转）。
- **加载**：列表/详情骨架屏或 `Spin` 包裹规则统一（避免同一产品有的页面白屏等待）。
- **错误**：接口失败 `message.error` 与 Result 页的使用边界（可列表失败用 message，整页不可用 Result）。

### 工程约定（列表与请求）

| 场景 | 推荐组件 / 行为 |
|------|-----------------|
| **列表无数据**（业务上确实为空） | `PageEmpty`（[`webapp/packages/supersonic-fe/src/components/PageEmpty`](../../webapp/packages/supersonic-fe/src/components/PageEmpty)）或 antd `Empty` + 一句引导 + 可选主按钮 |
| **列表请求失败** | `message.error`；**首次加载**失败建议清空表格数据；**翻页/筛选/刷新**失败建议保留当前数据（详见 [`request.ts` 文件头注释](../../webapp/packages/supersonic-fe/src/services/request.ts)） |
| **整页不可用** | antd `Result`（403/无权限、缺配置等），不用表格内 Empty 冒充整页错误 |

独立说明全文：[前端反馈规范（空态 / 加载 / 错误）](./frontend-feedback-conventions.md)。

### 交付物

1. **文案清单**：各核心列表空状态一句中文 + 可选主操作。
2. **轻量封装（可选）**：如 `PageEmpty({ title, description, action })`，内部仍用 Ant Design `Empty`，不引入新 UI 库。

### 周期建议

1～2 人日。

---

## 阶段四：品牌字体与对外形态（可选）

### 范围

- **应用内**：若需更强「数据产品」气质，可仅对 **数字/代码块** 使用等宽字体，正文保持系统字体以降低风险。
- **对外**：独立落地页/文档站与控制台分层；控制台继续「数据密、高效」，落地页做信任与转化（与 [运营数据工作台产品设计说明书](./运营数据工作台产品设计说明书.md) 中入口职责一致）。

### 交付物

字体与落地页单独 PR，避免与阶段一二混在一起。

### 周期建议

按是否新建落地页，3～10+ 人日不等。

---

## 总排期建议（串行）

```text
阶段一（主题/Token） → 阶段二（高频页） → 阶段三（空加载错） → 阶段四（可选）
     0.5～1.5d              2～4d              1～2d           按需
```

## 与产品文档的关系

- 导航与命名收敛需与 [运营数据工作台产品设计说明书](./运营数据工作台产品设计说明书.md) 中「入口职责」「任务中心一级化」一致，避免界面仍暴露过多实现对象名。
- 对外承诺的触达形态仍以需求/设计说明书中的投递语义为准，UI 仅做展示与引导，不改变业务规则。

## 参考与对标（补充）

| 维度 | 主参考 | 说明 |
|------|--------|------|
| 信息架构与视觉节奏 | **Metabase** | 自助问数、保存资产、列表与结果区主次；见上文「主参考」节。 |
| 设计系统实现 | **Ant Design / Pro Components** | 本项目唯一组件体系；Token 与密度在阶段一收敛。 |
| 高密度排障类（可选） | Datadog、Grafana 等 | 仅用于**任务中心 / 执行记录**等「运维视角」页的表格与筛选，且与全站 Token 一致。 |
| 文案与微交互（可选） | Linear、Notion 等 | 仅借鉴空状态、引导文案，不改变数据工作台主骨架。 |

**色与质感（非强制）：** 企业向数据产品仍可采用 **深蓝主色 + 浅灰背景 + 表格层级清晰 + 筛选与加载反馈明确**；具体色值在阶段一 Token 表中定稿，不必与 Metabase 或任何工具输出逐字一致。
