# Frontend Menu Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure SuperSonic's flat top-nav into a 4-group mix layout (分析中心 / 智能问答 / 数据建模 / 系统管理) without changing any existing page routes or breaking deep links.

**Architecture:** Change `layout` from `top` to `mix` with `splitMenus: true` in `defaultSettings.ts`. Add 4 path-less parent group nodes to `routes.ts` wrapping existing child routes. Add a `menuDataRender` pure-function filter (extracted to `src/utils/menuFilter.ts`) in `app.tsx` that hides parent groups whose children are all hidden.

**Tech Stack:** TypeScript, React, Umi 4 / Umi Max, Ant Design Pro Layout v7 (mix layout), Jest via `max test`

---

## File Map

| File | Action | What changes |
|------|--------|--------------|
| `webapp/packages/supersonic-fe/config/defaultSettings.ts` | Modify | `layout: 'top'` → `'mix'`; uncomment `splitMenus: true` |
| `webapp/packages/supersonic-fe/src/locales/zh-CN/menu.ts` | Modify | 4 new parent keys, 2 renames, 1 new key, 1 dead-key delete |
| `webapp/packages/supersonic-fe/src/utils/menuFilter.ts` | Create | Extracted `filterEmptyGroups` pure function |
| `webapp/packages/supersonic-fe/src/utils/menuFilter.test.ts` | Create | Unit tests for `filterEmptyGroups` |
| `webapp/packages/supersonic-fe/src/app.tsx` | Modify | Import and wire `filterEmptyGroups` as `menuDataRender` |
| `webapp/packages/supersonic-fe/config/routes.ts` | Modify | 4 path-less parent group nodes + route reorder |

---

## Task 1: Switch layout to mix

**Files:**
- Modify: `webapp/packages/supersonic-fe/config/defaultSettings.ts`

- [ ] **Step 1: Edit defaultSettings.ts**

Replace lines 13–25 (the `Settings` object body):

```ts
const Settings: DefaultSetting = {
  navTheme: 'light',
  colorPrimary: BRAND_PRIMARY,
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  colorWeak: false,
  title: '',
  pwa: false,
  iconfontUrl: '//at.alicdn.com/t/c/font_4120566_x5c4www9bqm.js',
  splitMenus: true,
};
```

- [ ] **Step 2: Commit**

```bash
cd webapp/packages/supersonic-fe
git add config/defaultSettings.ts
git commit -m "feat(nav): switch layout to mix with splitMenus"
```

---

## Task 2: i18n — add parent keys, fix renames, remove dead key

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/locales/zh-CN/menu.ts`

- [ ] **Step 1: Edit menu.ts**

Replace the entire file content:

```ts
export default {
  'menu.welcome': '欢迎',
  'menu.result': '结果页',
  'menu.result.success': '成功页',
  'menu.result.fail': '失败页',
  'menu.exception': '异常页',
  'menu.exception.not-permission': '403',
  'menu.exception.not-find': '404',
  'menu.exception.server-error': '500',

  // ========== 一级分组（mix 布局顶栏） ==========
  'menu.analysisCenter': '分析中心',
  'menu.aiQuery': '智能问答',
  'menu.dataModeling': '数据建模',
  'menu.systemAdmin': '系统管理',

  // ========== 分析中心子项 ==========
  'menu.operationsCockpit': '经营驾驶舱',
  'menu.businessTopics': '经营主题',
  'menu.topicWorkspace': '主题工作台',
  'menu.reports': '固定报表',
  'menu.taskCenter': '任务中心',
  'menu.responsibilityLedger': '运行总览',
  'menu.deliveryConfig': '推送配置',
  'menu.feishuBot': '飞书机器人',
  'menu.reportSchedule': '报表调度',

  // ========== 智能问答子项 ==========
  'menu.chat': '问答对话',
  'menu.agent': '助理管理',
  'menu.plugin': '插件管理',

  // ========== 数据建模子项 ==========
  'menu.semanticModel': '语义建模',
  'menu.metricEdit': '指标编辑',
  'menu.metric': '指标市场',
  'menu.metric.metricDetail': '指标详情页',
  'menu.tag': '标签市场',
  'menu.tag.tagDetail': '标签详情页',
  'menu.database': '数据库管理',
  'menu.llm': '大模型管理',
  'menu.semanticTemplate': '语义模板',

  // ========== 系统管理子项 ==========
  'menu.platform': '平台管理',
  'menu.platform.tenants': '租户列表',          // 原'租户管理'，与 menu.tenant 同名，已修正
  'menu.platform.subscriptions': '订阅计划',
  'menu.platform.platformRoles': '平台角色',
  'menu.platform.platformPermissions': '平台权限',
  'menu.platform.platformSettings': '平台设置', // 原'系统设置'，已修正

  'menu.tenant': '租户管理',
  'menu.tenant.organization': '组织架构',
  'menu.tenant.members': '成员管理',
  'menu.tenant.tenantRoles': '角色管理',
  'menu.tenant.tenantPermissions': '权限管理',
  'menu.tenant.tenantSettings': '租户设置',
  'menu.tenant.usage': '用量统计',

  // ========== 杂项 ==========
  'menu.chatSetting': '问答设置',
  'menu.login': '登录',
  // NOTE: 'menu.datasetPermission' deleted — dead key, no matching route
};
```

- [ ] **Step 2: Commit**

```bash
git add src/locales/zh-CN/menu.ts
git commit -m "feat(nav): update i18n keys for 4-group menu restructure"
```

---

## Task 3: menuFilter utility — write failing tests first

**Files:**
- Create: `webapp/packages/supersonic-fe/src/utils/menuFilter.ts`
- Create: `webapp/packages/supersonic-fe/src/utils/menuFilter.test.ts`

- [ ] **Step 1: Create the test file with failing tests**

Create `webapp/packages/supersonic-fe/src/utils/menuFilter.test.ts`:

```ts
import { filterEmptyGroups } from './menuFilter';

describe('filterEmptyGroups', () => {
  it('removes a leaf item with hideInMenu: true', () => {
    const input = [
      { name: 'a', path: '/a', hideInMenu: false },
      { name: 'b', path: '/b', hideInMenu: true },
    ];
    expect(filterEmptyGroups(input)).toEqual([
      { name: 'a', path: '/a', hideInMenu: false },
    ]);
  });

  it('keeps a parent whose children are all visible', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [
          { name: 'child', path: '/c', hideInMenu: false },
        ],
      },
    ];
    const result = filterEmptyGroups(input);
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
  });

  it('removes a parent when ALL children are hideInMenu: true', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [
          { name: 'hidden', path: '/h', hideInMenu: true },
          { name: 'also-hidden', path: '/h2', hideInMenu: true },
        ],
      },
    ];
    expect(filterEmptyGroups(input)).toHaveLength(0);
  });

  it('keeps a parent when at least one child is visible', () => {
    const input = [
      {
        name: 'group',
        hideInMenu: false,
        children: [
          { name: 'hidden', path: '/h', hideInMenu: true },
          { name: 'visible', path: '/v', hideInMenu: false },
        ],
      },
    ];
    const result = filterEmptyGroups(input);
    expect(result).toHaveLength(1);
    expect(result[0].children).toHaveLength(1);
    expect(result[0].children![0].name).toBe('visible');
  });

  it('handles a parent with no children field (leaf with no children)', () => {
    const input = [
      { name: 'leaf', path: '/l', hideInMenu: false },
    ];
    expect(filterEmptyGroups(input)).toHaveLength(1);
  });

  it('handles 3-level nesting: removes top-level group when all grandchildren are hidden', () => {
    const input = [
      {
        name: 'top',
        hideInMenu: false,
        children: [
          {
            name: 'mid',
            hideInMenu: false,
            children: [
              { name: 'leaf', path: '/leaf', hideInMenu: true },
            ],
          },
        ],
      },
    ];
    // mid becomes empty → top becomes empty → both filtered
    expect(filterEmptyGroups(input)).toHaveLength(0);
  });

  it('returns empty array for empty input', () => {
    expect(filterEmptyGroups([])).toEqual([]);
  });
});
```

- [ ] **Step 2: Run tests to verify they fail (function not yet implemented)**

```bash
cd webapp/packages/supersonic-fe
npx max test src/utils/menuFilter.test.ts --no-coverage 2>&1 | tail -10
```

Expected: `Cannot find module './menuFilter'` or similar import error.

- [ ] **Step 3: Implement menuFilter.ts**

Create `webapp/packages/supersonic-fe/src/utils/menuFilter.ts`:

```ts
export interface MenuNode {
  name?: string;
  path?: string;
  hideInMenu?: boolean;
  children?: MenuNode[];
  [key: string]: unknown;
}

/**
 * Recursively filter menu nodes.
 * A node is kept if:
 *   1. Its own hideInMenu is falsy, AND
 *   2. It has no children, OR its children array is non-empty after filtering.
 *
 * Used as menuDataRender in app.tsx to hide parent groups whose
 * children are all hidden (by hideInMenu or env/access filtering).
 */
export function filterEmptyGroups(items: MenuNode[]): MenuNode[] {
  return items
    .map((item) => ({
      ...item,
      children: item.children ? filterEmptyGroups(item.children) : undefined,
    }))
    .filter(
      (item) =>
        !item.hideInMenu &&
        (item.children === undefined || item.children === null || item.children.length > 0),
    );
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
npx max test src/utils/menuFilter.test.ts --no-coverage 2>&1 | tail -15
```

Expected output:

```
PASS src/utils/menuFilter.test.ts
  filterEmptyGroups
    ✓ removes a leaf item with hideInMenu: true
    ✓ keeps a parent whose children are all visible
    ✓ removes a parent when ALL children are hideInMenu: true
    ✓ keeps a parent when at least one child is visible
    ✓ handles a parent with no children field (leaf with no children)
    ✓ handles 3-level nesting: removes top-level group when all grandchildren are hidden
    ✓ returns empty array for empty input

Tests: 7 passed, 7 total
```

- [ ] **Step 5: Commit**

```bash
git add src/utils/menuFilter.ts src/utils/menuFilter.test.ts
git commit -m "feat(nav): add filterEmptyGroups utility with unit tests"
```

---

## Task 4: Wire menuDataRender into app.tsx

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/app.tsx`

- [ ] **Step 1: Add import at the top of app.tsx**

Find the existing import block near the top of `src/app.tsx`. Add the following import alongside the other local imports:

```ts
import { filterEmptyGroups } from '@/utils/menuFilter';
```

- [ ] **Step 2: Add menuDataRender to the layout export**

Inside the `export const layout` function, add `menuDataRender` to the returned object. The function currently returns an object with `...initialState?.settings` spread at the end. Add `menuDataRender` **before** that spread so settings can't accidentally override it:

```ts
export const layout = ({ initialState }: { initialState: InitialState }) => {
  return {
    // ... all existing properties unchanged (rightContentRender, onPageChange, etc.) ...
    menuDataRender: (menuData: any[]) => filterEmptyGroups(menuData),
    ...initialState?.settings,
  };
};
```

Important: do not remove or reorder any existing properties. Only add `menuDataRender`.

- [ ] **Step 3: Verify TypeScript compiles**

```bash
cd /Users/xudong/git/supersonic
mvn compile -pl launchers/standalone -am -q 2>&1 | tail -5
```

Then verify frontend types:

```bash
cd webapp/packages/supersonic-fe
npx tsc --noEmit 2>&1 | grep -E "error TS" | head -10
```

Expected: no new TypeScript errors.

- [ ] **Step 4: Commit**

```bash
git add src/app.tsx
git commit -m "feat(nav): wire filterEmptyGroups as menuDataRender in app layout"
```

---

## Task 5: Restructure routes.ts — add 4 parent groups

**Files:**
- Modify: `webapp/packages/supersonic-fe/config/routes.ts`

This is the largest change. Replace the entire `ROUTES` array (everything after the `ROUTE_AUTH_CODES` and `ENV_KEY` declarations) with the structure below. The `ROUTE_AUTH_CODES` constant and `ENV_KEY` constant stay unchanged.

- [ ] **Step 1: Replace the ROUTES array**

Replace from `const ROUTES = [` through `export default ROUTES;` with:

```ts
const ROUTES = [
  // ──────────────────────────────────────────────────────────
  // layout:false routes — bypass Pro Layout entirely.
  // MUST be declared before any group parent nodes.
  // ──────────────────────────────────────────────────────────
  {
    path: '/chat/mobile',
    name: 'chat',
    component: './ChatPage',
    hideInMenu: true,
    layout: false,
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/chat/external',
    name: 'chat',
    component: './ChatPage',
    hideInMenu: true,
    layout: false,
    envEnableList: [ENV_KEY.CHAT],
  },

  // ========== 分析中心 ==========
  {
    name: 'analysisCenter',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/operations-cockpit',
        name: 'operationsCockpit',
        component: './OperationsCockpit',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/business-topics',
        name: 'businessTopics',
        component: './BusinessTopics',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/business-topics/workspace/:topicId',
        name: 'topicWorkspace',
        component: './BusinessTopics/TopicWorkspace',
        envEnableList: [ENV_KEY.SEMANTIC],
        hideInMenu: true,
      },
      {
        path: '/reports',
        name: 'reports',
        component: './Reports',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/task-center',
        name: 'taskCenter',
        component: './TaskCenter',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/responsibility-ledger',
        name: 'responsibilityLedger',
        component: './ResponsibilityLedger',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/delivery-config',
        name: 'deliveryConfig',
        component: './DeliveryConfig',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/feishu-bot',
        name: 'feishuBot',
        component: './FeishuBot',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/report-schedule',
        name: 'reportSchedule',
        component: './ReportSchedule',
        envEnableList: [ENV_KEY.SEMANTIC],
        hideInMenu: true,
      },
    ],
  },

  // ========== 智能问答 ==========
  {
    name: 'aiQuery',
    envEnableList: [ENV_KEY.CHAT],
    routes: [
      {
        path: '/chat',
        name: 'chat',
        component: './ChatPage',
        envEnableList: [ENV_KEY.CHAT],
        access: ROUTE_AUTH_CODES.MENU_CHAT,
      },
      {
        path: '/agent',
        name: 'agent',
        component: './Agent',
        envEnableList: [ENV_KEY.CHAT],
        access: ROUTE_AUTH_CODES.MENU_AGENT,
      },
      {
        path: '/plugin',
        name: 'plugin',
        component: './ChatPlugin',
        envEnableList: [ENV_KEY.CHAT],
        access: ROUTE_AUTH_CODES.MENU_PLUGIN,
      },
    ],
  },

  // ========== 数据建模 ==========
  {
    name: 'dataModeling',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/model/metric/edit/:metricId',
        name: 'metricEdit',
        hideInMenu: true,
        component: './SemanticModel/Metric/Edit',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/model/',
        component: './SemanticModel/',
        name: 'semanticModel',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_MODEL,
        routes: [
          {
            path: '/model/',
            redirect: '/model/domain',
          },
          {
            path: '/model/domain/',
            component: './SemanticModel/OverviewContainer',
            routes: [
              {
                path: '/model/domain/:domainId',
                component: './SemanticModel/DomainManager',
                routes: [
                  {
                    path: '/model/domain/:domainId/:menuKey',
                    component: './SemanticModel/DomainManager',
                  },
                ],
              },
              {
                path: '/model/domain/manager/:domainId/:modelId',
                component: './SemanticModel/ModelManager',
                routes: [
                  {
                    path: '/model/domain/manager/:domainId/:modelId/:menuKey',
                    component: './SemanticModel/ModelManager',
                  },
                ],
              },
            ],
          },
          {
            path: '/model/dataset/:domainId/:datasetId',
            component: './SemanticModel/View/components/Detail',
            envEnableList: [ENV_KEY.SEMANTIC],
            routes: [
              {
                path: '/model/dataset/:domainId/:datasetId/:menuKey',
                component: './SemanticModel/View/components/Detail',
              },
            ],
          },
          {
            path: '/model/metric/:domainId/:modelId/:metricId',
            component: './SemanticModel/Metric/Edit',
            envEnableList: [ENV_KEY.SEMANTIC],
          },
          {
            path: '/model/dimension/:domainId/:modelId/:dimensionId',
            component: './SemanticModel/Dimension/Detail',
            envEnableList: [ENV_KEY.SEMANTIC],
          },
        ],
      },
      {
        path: '/metric',
        name: 'metric',
        component: './SemanticModel/Metric',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_METRIC,
        routes: [
          {
            path: '/metric',
            redirect: '/metric/market',
          },
          {
            path: '/metric/market',
            component: './SemanticModel/Metric/Market',
            hideInMenu: true,
            envEnableList: [ENV_KEY.SEMANTIC],
          },
          {
            path: '/metric/detail/:metricId',
            name: 'metricDetail',
            hideInMenu: true,
            component: './SemanticModel/Metric/Detail',
            envEnableList: [ENV_KEY.SEMANTIC],
          },
          {
            path: '/metric/detail/edit/:metricId',
            name: 'metricDetail',
            hideInMenu: true,
            component: './SemanticModel/Metric/Edit',
            envEnableList: [ENV_KEY.SEMANTIC],
          },
        ],
      },
      {
        path: '/tag',
        name: 'tag',
        component: './SemanticModel/Insights',
        envEnableList: [ENV_KEY.SEMANTIC],
        hideInMenu: process.env.SHOW_TAG ? false : true,
        routes: [
          {
            path: '/tag',
            redirect: '/tag/market',
          },
          {
            path: '/tag/market',
            component: './SemanticModel/Insights/Market',
            hideInMenu: true,
            envEnableList: [ENV_KEY.SEMANTIC],
          },
          {
            path: '/tag/detail/:tagId',
            name: 'tagDetail',
            hideInMenu: true,
            component: './SemanticModel/Insights/Detail',
            envEnableList: [ENV_KEY.SEMANTIC],
          },
        ],
      },
      {
        path: '/database',
        name: 'database',
        component: './SemanticModel/components/Database/DatabaseTable',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_DATABASE,
      },
      {
        path: '/llm',
        name: 'llm',
        component: './SemanticModel/components/LLM/LlmTable',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_LLM,
      },
      {
        path: '/semantic-template',
        name: 'semanticTemplate',
        component: './SemanticTemplate',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_SEMANTIC_TEMPLATE,
      },
    ],
  },

  // ========== 系统管理 ==========
  {
    name: 'systemAdmin',
    routes: [
      {
        path: '/platform',
        name: 'platform',
        access: ROUTE_AUTH_CODES.PLATFORM_ADMIN,
        routes: [
          {
            path: '/platform',
            redirect: '/platform/tenants',
          },
          {
            path: '/platform/tenants',
            name: 'tenants',
            component: './AdminTenant',
            access: ROUTE_AUTH_CODES.PLATFORM_TENANT_MANAGE,
          },
          {
            path: '/platform/subscriptions',
            name: 'subscriptions',
            component: './Platform/SubscriptionManagement',
            access: ROUTE_AUTH_CODES.PLATFORM_SUBSCRIPTION,
          },
          {
            path: '/platform/roles',
            name: 'platformRoles',
            component: './Platform/RoleManagement',
            access: ROUTE_AUTH_CODES.PLATFORM_ROLE_MANAGE,
          },
          {
            path: '/platform/permissions',
            name: 'platformPermissions',
            component: './Platform/PermissionManagement',
            access: ROUTE_AUTH_CODES.PLATFORM_PERMISSION,
          },
          {
            path: '/platform/settings',
            name: 'platformSettings',
            component: './System',
            access: ROUTE_AUTH_CODES.PLATFORM_SETTINGS,
          },
        ],
      },
      {
        path: '/tenant',
        name: 'tenant',
        access: ROUTE_AUTH_CODES.TENANT_ADMIN,
        routes: [
          {
            path: '/tenant',
            redirect: '/tenant/organization',
          },
          {
            path: '/tenant/organization',
            name: 'organization',
            component: './System/OrganizationManagement',
            access: ROUTE_AUTH_CODES.TENANT_ORG_MANAGE,
          },
          {
            path: '/tenant/members',
            name: 'members',
            component: './Tenant/MemberManagement',
            access: ROUTE_AUTH_CODES.TENANT_MEMBER_MANAGE,
          },
          {
            path: '/tenant/roles',
            name: 'tenantRoles',
            component: './Tenant/RoleManagement',
            access: ROUTE_AUTH_CODES.TENANT_ROLE_MANAGE,
          },
          {
            path: '/tenant/permissions',
            name: 'tenantPermissions',
            component: './Tenant/PermissionManagement',
            access: ROUTE_AUTH_CODES.TENANT_PERMISSION,
          },
          {
            path: '/tenant/settings',
            name: 'tenantSettings',
            component: './TenantSettings',
            access: ROUTE_AUTH_CODES.TENANT_SETTINGS,
          },
          {
            path: '/tenant/usage',
            name: 'usage',
            component: './UsageDashboard',
            access: ROUTE_AUTH_CODES.TENANT_USAGE_VIEW,
          },
        ],
      },
    ],
  },

  // ========== 系统路由（不出现在菜单中） ==========
  {
    path: '/login',
    name: 'login',
    layout: false,
    hideInMenu: true,
    component: './Login',
  },
  {
    path: '/login/callback',
    name: 'oauthCallback',
    layout: false,
    hideInMenu: true,
    component: './Login/OAuthCallback',
  },
  {
    path: '/',
    redirect: '/operations-cockpit',
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd webapp/packages/supersonic-fe
npx tsc --noEmit 2>&1 | grep -E "error TS" | head -10
```

Expected: no TypeScript errors.

- [ ] **Step 3: Commit**

```bash
git add config/routes.ts
git commit -m "feat(nav): add 4 path-less group parents to routes.ts"
```

---

## Task 6: Manual smoke test checklist

Start the dev server and verify each item below:

```bash
cd webapp/packages/supersonic-fe
npm run dev
# Open http://localhost:8000
```

- [ ] **Top nav shows 4 groups** — "分析中心 / 智能问答 / 数据建模 / 系统管理" in the top bar (not a flat list of pages)
- [ ] **分析中心 left sidebar** — clicking 分析中心 shows: 经营驾驶舱 / 经营主题 / 固定报表 / 任务中心 / 运行总览 / 推送配置 / 飞书机器人 (in that order, no 报表调度)
- [ ] **Deep link — direct URL** — navigate directly to `http://localhost:8000/webapp/reports` → page loads, 分析中心 active in top nav, 固定报表 highlighted in sidebar
- [ ] **Deep link — /model/ routes** — navigate to `http://localhost:8000/webapp/model/domain` → page loads, 数据建模 active in top nav
- [ ] **Logo click** — click logo → navigates to `/operations-cockpit`
- [ ] **/ redirect** — visiting `http://localhost:8000/webapp/` → redirects to `http://localhost:8000/webapp/operations-cockpit`
- [ ] **/chat/mobile bypass** — navigate to `http://localhost:8000/webapp/chat/mobile` → renders ChatPage without the mix layout chrome (no top nav, no sidebar)
- [ ] **splitMenus isolation** — click 分析中心 tab → left sidebar shows only 分析中心 sub-items. Click 数据建模 → left sidebar now shows only 数据建模 sub-items. No bleed-through.
- [ ] **系统管理 3-level** — click 系统管理 → left sidebar shows 平台管理 and 租户管理. Expand 平台管理 → shows 5 sub-items with the corrected names (租户列表, 订阅计划, 平台角色, 平台权限, 平台设置). Switch to 分析中心 then back to 系统管理 → expansion state is restored correctly. If it's broken (wrong item highlighted, sidebar collapse bug), revert `splitMenus: true` to `splitMenus: false` in `defaultSettings.ts` and open a follow-up issue.
- [ ] **Mobile viewport** — resize browser to 768px wide → top nav groups collapse to hamburger icon, clicking shows Drawer with all 4 groups
- [ ] **/chat height** — click 问答对话 → chat page fills the viewport without vertical scrollbar or clipped content

- [ ] **Final commit if all checks pass**

```bash
git add -A
git commit -m "feat(nav): frontend menu restructure — mix layout, 4 groups, i18n"
```

---

## Known limitations / follow-up items

| Item | Status | Notes |
|------|--------|-------|
| `envEnableList` / `patchRoutes` filtering | Pre-existing, not fixed here | `patchRoutes` in `app.tsx` is commented out. Env-based group hiding (`CHAT 关闭时智能问答消失`) requires re-enabling it separately. |
| English i18n (en-US) | Not in scope | Add the same 4 parent keys and 2 renames to `src/locales/en-US/menu.ts` in a follow-up. |
| `aria-label` on nav landmarks | Not in scope | Add `aria-label="主导航"` to top nav and `aria-label="二级菜单"` to sidebar in a follow-up accessibility pass. |
| First-login onboarding toast | Not in scope | `localStorage.nav_v2_intro_seen` toast for users familiar with old flat nav — implement in a follow-up. |

## Implementation deviation note

- Parent groups were implemented as explicit routes (`/analysis-center`, `/ai-query`, `/data-modeling`, `/system-admin`) with `RouteGroupRedirect` instead of strictly path-less nodes.
- This keeps deep links intact and provides a stable, bookmarkable group entry point.
- To avoid blank pages when a user can access a group route but no child route, `RouteGroupRedirect` now falls back to `/401`.
