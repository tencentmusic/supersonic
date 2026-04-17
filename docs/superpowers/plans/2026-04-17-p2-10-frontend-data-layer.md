# P2-10 Frontend Data Layer Migration to React-Query Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Progressively migrate SuperSonic's webapp data fetching from manual `useState`/`useEffect` + `umi-request` to TanStack Query v5, starting with the `ReportSchedule` page as a pilot, while keeping the existing `umi-request` service functions as the transport layer.

**Architecture:**
- Install `@tanstack/react-query` v5 and `@tanstack/react-query-devtools` (dev-only) inside `webapp/packages/supersonic-fe`.
- Wrap the Umi root in a single `QueryClientProvider` via Umi's `rootContainer` export from `src/app.tsx`. Devtools is gated on `REACT_APP_ENV !== 'prod'`.
- A shared `QueryClient` (`src/utils/queryClient.ts`) sets `staleTime: 30_000`, `refetchOnWindowFocus: false`, and a `retry` policy that skips 4xx.
- Query keys live in a typed factory (`src/utils/queryKeys.ts`) keyed by `tenantId` derived from `localStorage` (since tenant is header-injected in `request.ts`) so caches do not leak across tenant switches.
- Each domain gets a `src/hooks/queries/<domain>.ts` file exposing `useXxxQuery` and `useXxxMutation` hooks that call existing `src/services/*.ts` functions. The API layer is NOT rewritten.
- Migration is strictly incremental — one page at a time — with a documented conversion recipe and a fallback `<QueryBoundary>` component for consistent `Spin`/`Skeleton` and error rendering.

**Tech Stack:**
- Package manager: **pnpm 9.12.3** (from `packageManager` in `webapp/packages/supersonic-fe/package.json`).
- Node: **18+** (see `webapp/.nvmrc` → `20`).
- React 18.3.1, TypeScript 5.4.5, antd 5.29.3, `@umijs/max` 4.6.40, `umi-request` 1.4.0.
- Test: existing Jest unit harness in `tests/unit/` driven by `scripts/run-unit-tests.cjs` (compiles TS → CJS via `tsc`, runs Jest).
- New: `@tanstack/react-query@^5.56.2`, `@tanstack/react-query-devtools@^5.56.2`, `@testing-library/react@^16.0.1`, `@testing-library/jest-dom@^6.5.0`, `@types/testing-library__jest-dom@^5.14.9`.

---

## File Structure

Files created:
- `webapp/packages/supersonic-fe/src/utils/queryClient.ts` — shared `QueryClient` + global `QueryCache`/`MutationCache` error handlers that call `antd` `notification.error`.
- `webapp/packages/supersonic-fe/src/utils/queryKeys.ts` — typed `queryKeys` factory namespaced by `tenantId`.
- `webapp/packages/supersonic-fe/src/utils/queryUtils.ts` — helper `extractHttpStatus(err)` + `shouldRetry` (retry skip 4xx).
- `webapp/packages/supersonic-fe/src/components/QueryBoundary/index.tsx` — `<QueryBoundary>` helper that renders `Spin`/`Skeleton` and centralized error for `UseQueryResult`.
- `webapp/packages/supersonic-fe/src/hooks/queries/reportSchedule.ts` — `useScheduleListQuery`, `useScheduleSaveMutation`, `useScheduleDeleteMutation` (pilot domain).
- `webapp/packages/supersonic-fe/tests/unit/queryClient.test.js` — Jest tests for retry/error policy.
- `webapp/packages/supersonic-fe/tests/unit/reportScheduleQuery.test.js` — Jest + RTL test for `useScheduleListQuery`.
- `webapp/packages/supersonic-fe/tests/unit/setupRtl.js` — RTL test setup (`@testing-library/jest-dom`).
- `docs/details/platform/04-frontend-data-layer.md` — the frontend data-layer guide (created in Task 11).

Files modified:
- `webapp/packages/supersonic-fe/package.json` — dependencies added, new `test:query` script.
- `webapp/packages/supersonic-fe/src/app.tsx` — add `rootContainer` export wrapping children in `QueryClientProvider` + `ReactQueryDevtools`.
- `webapp/packages/supersonic-fe/src/pages/ReportSchedule/index.tsx` — replace `getScheduleList` `useState/useEffect` with `useScheduleListQuery`; replace `createSchedule`/`updateSchedule`/`deleteSchedule` calls with `useMutation` (Tasks 5, 6, 9).
- `webapp/packages/supersonic-fe/scripts/run-unit-tests.cjs` — compile the new TS files and add Jest setup for RTL.
- `webapp/packages/supersonic-fe/jest.unit.config.cjs` — register `setupRtl.js`.
- `webapp/packages/supersonic-fe/README.md` — add a "Data Fetching with React Query" section.
- `webapp/packages/supersonic-fe/tsconfig.json` — no change required; `@tanstack/*` ships types.

---

## Task 1: Install dependencies and wire `QueryClientProvider`

**Files:**
- Modify: `webapp/packages/supersonic-fe/package.json`
- Modify: `webapp/packages/supersonic-fe/src/app.tsx`

- [ ] **Step 1: Install runtime dependencies with pnpm**

The webapp uses pnpm workspaces (see `webapp/pnpm-workspace.yaml`). Install from the package directory so only `supersonic-fe` gets the new deps.

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm add @tanstack/react-query@5.56.2
pnpm add -D @tanstack/react-query-devtools@5.56.2 @testing-library/react@16.0.1 @testing-library/jest-dom@6.5.0 @types/testing-library__jest-dom@5.14.9
```

Expected `package.json` diff (in `webapp/packages/supersonic-fe/package.json`):

```diff
   "dependencies": {
     ...
     "@ant-design/pro-components": "2.7.0",
+    "@tanstack/react-query": "5.56.2",
     "@antv/dom-util": "^2.0.4",
     ...
   },
   "devDependencies": {
     ...
     "@ant-design/pro-cli": "^2.1.5",
+    "@tanstack/react-query-devtools": "5.56.2",
+    "@testing-library/jest-dom": "6.5.0",
+    "@testing-library/react": "16.0.1",
+    "@types/testing-library__jest-dom": "5.14.9",
     ...
   },
```

- [ ] **Step 2: Verify install — run `pnpm run tsc`**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run tsc
```

Expected: exits 0 (no new errors; only pre-existing if any).

- [ ] **Step 3: Add `rootContainer` export in `src/app.tsx`**

Umi's `@umijs/max` supports a `rootContainer(container)` runtime export that wraps the whole app. Insert **after** the `Spin.setDefaultIndicator(...)` block and **before** `const getAuthCodes` (around line 22). Do NOT remove any existing exports.

Full snippet to insert in `webapp/packages/supersonic-fe/src/app.tsx`:

```tsx
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { queryClient } from '@/utils/queryClient';

export function rootContainer(container: React.ReactNode) {
  const showDevtools = process.env.REACT_APP_ENV !== 'prod';
  return (
    <QueryClientProvider client={queryClient}>
      {container}
      {showDevtools ? <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-left" /> : null}
    </QueryClientProvider>
  );
}
```

NOTE: `queryClient` module is created in Task 2 — running the dev server before Task 2 will fail to import. Commit this step and Task 2 together, or commit Task 2 first (order is flexible; the plan commits Task 2 immediately below in Step 5).

- [ ] **Step 4: Verify the `ReactQueryDevtools` is tree-shaken from prod**

Confirm with an env check:

```bash
cd webapp/packages/supersonic-fe
grep -n "process.env.REACT_APP_ENV" src/app.tsx
```

Expected: the grep lists your new line plus any pre-existing. The `prod` guard keeps devtools out of `build:os`.

- [ ] **Step 5: Commit**

```bash
git add webapp/packages/supersonic-fe/package.json \
        webapp/pnpm-lock.yaml \
        webapp/packages/supersonic-fe/src/app.tsx
git commit -m "feat(webapp): install @tanstack/react-query and wire rootContainer"
```

---

## Task 2: Create shared `QueryClient` with defaults + global error handler

**Files:**
- Create: `webapp/packages/supersonic-fe/src/utils/queryUtils.ts`
- Create: `webapp/packages/supersonic-fe/src/utils/queryClient.ts`
- Test: `webapp/packages/supersonic-fe/tests/unit/queryClient.test.js`

- [ ] **Step 1: Write the failing test**

Create `webapp/packages/supersonic-fe/tests/unit/queryClient.test.js`:

```js
const {
  extractHttpStatus,
  shouldRetry,
} = require('../../.tmp-unit/utils/queryUtils.js');

describe('extractHttpStatus', () => {
  it('reads response.status from umi-request error', () => {
    expect(extractHttpStatus({ response: { status: 404 } })).toBe(404);
  });

  it('reads data.status (some server errors wrap in data)', () => {
    expect(extractHttpStatus({ data: { status: 500 } })).toBe(500);
  });

  it('returns undefined when no status', () => {
    expect(extractHttpStatus(new Error('network'))).toBeUndefined();
    expect(extractHttpStatus(null)).toBeUndefined();
    expect(extractHttpStatus(undefined)).toBeUndefined();
  });
});

describe('shouldRetry', () => {
  it('does not retry on 4xx', () => {
    expect(shouldRetry(0, { response: { status: 400 } })).toBe(false);
    expect(shouldRetry(0, { response: { status: 403 } })).toBe(false);
    expect(shouldRetry(0, { response: { status: 499 } })).toBe(false);
  });

  it('retries once on 5xx', () => {
    expect(shouldRetry(0, { response: { status: 500 } })).toBe(true);
    expect(shouldRetry(1, { response: { status: 500 } })).toBe(false);
  });

  it('retries once on network errors (no status)', () => {
    expect(shouldRetry(0, new Error('network'))).toBe(true);
    expect(shouldRetry(1, new Error('network'))).toBe(false);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: FAIL with "Cannot find module '../../.tmp-unit/utils/queryUtils.js'" (module not yet compiled).

- [ ] **Step 3: Create `src/utils/queryUtils.ts`**

```ts
// src/utils/queryUtils.ts
/**
 * Extract HTTP status from an error thrown by umi-request.
 * umi-request attaches the Response on `err.response`, and sometimes
 * wraps backend payloads on `err.data`.
 */
export function extractHttpStatus(err: unknown): number | undefined {
  if (!err || typeof err !== 'object') return undefined;
  const anyErr = err as { response?: { status?: number }; data?: { status?: number } };
  if (anyErr.response && typeof anyErr.response.status === 'number') {
    return anyErr.response.status;
  }
  if (anyErr.data && typeof anyErr.data.status === 'number') {
    return anyErr.data.status;
  }
  return undefined;
}

/**
 * React-Query `retry` function: retry at most once, and skip 4xx.
 * Signature matches TanStack's `(failureCount: number, error: unknown) => boolean`.
 */
export function shouldRetry(failureCount: number, err: unknown): boolean {
  if (failureCount >= 1) return false;
  const status = extractHttpStatus(err);
  if (status !== undefined && status >= 400 && status < 500) return false;
  return true;
}
```

- [ ] **Step 4: Create `src/utils/queryClient.ts`**

```ts
// src/utils/queryClient.ts
import { QueryCache, QueryClient, MutationCache } from '@tanstack/react-query';
import { notification } from 'antd';
import { shouldRetry, extractHttpStatus } from './queryUtils';

const FOUR_OH_ONE = 401;
const FOUR_OH_THREE = 403;

function describeError(err: unknown): { message: string; description?: string } {
  if (err && typeof err === 'object') {
    const anyErr = err as { message?: string; data?: { msg?: string; message?: string } };
    const description = anyErr.data?.msg || anyErr.data?.message;
    return {
      message: anyErr.message || '请求失败',
      description,
    };
  }
  return { message: '请求失败' };
}

function handleGlobalError(err: unknown, meta?: Record<string, unknown>) {
  const status = extractHttpStatus(err);
  // 401/403 are redirected by services/request.ts responseInterceptor — do not notify twice.
  if (status === FOUR_OH_ONE || status === FOUR_OH_THREE) return;
  // Opt-out: a hook can pass meta.silent = true to suppress this notification.
  if (meta && meta.silent === true) return;
  const { message, description } = describeError(err);
  notification.error({ message, description, duration: 4 });
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
      retry: shouldRetry,
    },
    mutations: {
      retry: false,
    },
  },
  queryCache: new QueryCache({
    onError: (err, query) => handleGlobalError(err, query.meta),
  }),
  mutationCache: new MutationCache({
    onError: (err, _vars, _ctx, mutation) => handleGlobalError(err, mutation.meta),
  }),
});
```

- [ ] **Step 5: Register `queryUtils.ts` for Jest compilation**

Modify `webapp/packages/supersonic-fe/scripts/run-unit-tests.cjs`. Add the file to the `execFileSync` compile list:

```diff
     path.join(projectRoot, 'src/utils/menuFilter.ts'),
     path.join(projectRoot, 'src/pages/RouteGroupRedirect/resolveRedirect.ts'),
     path.join(projectRoot, 'src/pages/ReportSchedule/utils/scheduleFormValidation.ts'),
+    path.join(projectRoot, 'src/utils/queryUtils.ts'),
```

- [ ] **Step 6: Run test to verify it passes**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: all tests PASS including the new `queryClient.test.js`.

- [ ] **Step 7: Commit**

```bash
git add webapp/packages/supersonic-fe/src/utils/queryUtils.ts \
        webapp/packages/supersonic-fe/src/utils/queryClient.ts \
        webapp/packages/supersonic-fe/tests/unit/queryClient.test.js \
        webapp/packages/supersonic-fe/scripts/run-unit-tests.cjs
git commit -m "feat(webapp): shared QueryClient with retry + global error handler"
```

---

## Task 3: Type-safe `queryKeys` factory

**Files:**
- Create: `webapp/packages/supersonic-fe/src/utils/queryKeys.ts`
- Test: `webapp/packages/supersonic-fe/tests/unit/queryKeys.test.js`

- [ ] **Step 1: Write the failing test**

Create `webapp/packages/supersonic-fe/tests/unit/queryKeys.test.js`:

```js
const { queryKeys, createQueryKeys } = require('../../.tmp-unit/utils/queryKeys.js');

describe('queryKeys factory (singleton reads tenant from localStorage)', () => {
  beforeEach(() => {
    global.localStorage = {
      _s: {},
      getItem(k) { return this._s[k] ?? null; },
      setItem(k, v) { this._s[k] = v; },
      removeItem(k) { delete this._s[k]; },
    };
  });

  it('namespaces all keys by current tenant id', () => {
    global.localStorage.setItem('X-Tenant-Id', '7');
    expect(queryKeys.reportSchedule.all()).toEqual(['t:7', 'reportSchedule']);
    expect(queryKeys.reportSchedule.list({ current: 1, pageSize: 20 })).toEqual([
      't:7',
      'reportSchedule',
      'list',
      { current: 1, pageSize: 20 },
    ]);
    expect(queryKeys.reportSchedule.detail(42)).toEqual(['t:7', 'reportSchedule', 'detail', 42]);
  });

  it('defaults to tenant 1 when header missing', () => {
    expect(queryKeys.reportSchedule.all()).toEqual(['t:1', 'reportSchedule']);
  });

  it('createQueryKeys allows injecting a tenant explicitly (for tests)', () => {
    const k = createQueryKeys(99);
    expect(k.reportSchedule.all()).toEqual(['t:99', 'reportSchedule']);
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: FAIL — module not yet compiled.

- [ ] **Step 3: Create `src/utils/queryKeys.ts`**

```ts
// src/utils/queryKeys.ts
import { getStoredTenantIdNumber } from '@/services/request';

type TenantNs = readonly [string];

function tenantNs(tenantId?: number): TenantNs {
  const id = tenantId ?? getStoredTenantIdNumber(1);
  return [`t:${id}`] as const;
}

export function createQueryKeys(tenantId?: number) {
  const ns = tenantNs(tenantId);
  return {
    reportSchedule: {
      all: () => [...ns, 'reportSchedule'] as const,
      list: (params: { current?: number; pageSize?: number; datasetId?: number; enabled?: boolean }) =>
        [...ns, 'reportSchedule', 'list', params] as const,
      detail: (id: number) => [...ns, 'reportSchedule', 'detail', id] as const,
      executions: (scheduleId: number, params?: Record<string, unknown>) =>
        [...ns, 'reportSchedule', 'executions', scheduleId, params ?? {}] as const,
    },
    deliveryConfig: {
      all: () => [...ns, 'deliveryConfig'] as const,
      list: (params: { pageNum?: number; pageSize?: number }) =>
        [...ns, 'deliveryConfig', 'list', params] as const,
    },
    dataSet: {
      all: () => [...ns, 'dataSet'] as const,
      valid: () => [...ns, 'dataSet', 'valid'] as const,
      schema: (dataSetId: number) => [...ns, 'dataSet', 'schema', dataSetId] as const,
    },
    taskCenter: {
      all: () => [...ns, 'taskCenter'] as const,
      alertRules: (params: Record<string, unknown>) => [...ns, 'taskCenter', 'alertRules', params] as const,
      alertEvents: (params: Record<string, unknown>) => [...ns, 'taskCenter', 'alertEvents', params] as const,
      exportTasks: (params: Record<string, unknown>) => [...ns, 'taskCenter', 'exportTasks', params] as const,
    },
  } as const;
}

/** Default singleton — resolves tenant lazily at call time. */
export const queryKeys = {
  reportSchedule: {
    all: () => createQueryKeys().reportSchedule.all(),
    list: (p: Parameters<ReturnType<typeof createQueryKeys>['reportSchedule']['list']>[0]) =>
      createQueryKeys().reportSchedule.list(p),
    detail: (id: number) => createQueryKeys().reportSchedule.detail(id),
    executions: (scheduleId: number, p?: Record<string, unknown>) =>
      createQueryKeys().reportSchedule.executions(scheduleId, p),
  },
  deliveryConfig: {
    all: () => createQueryKeys().deliveryConfig.all(),
    list: (p: Parameters<ReturnType<typeof createQueryKeys>['deliveryConfig']['list']>[0]) =>
      createQueryKeys().deliveryConfig.list(p),
  },
  dataSet: {
    all: () => createQueryKeys().dataSet.all(),
    valid: () => createQueryKeys().dataSet.valid(),
    schema: (id: number) => createQueryKeys().dataSet.schema(id),
  },
  taskCenter: {
    all: () => createQueryKeys().taskCenter.all(),
    alertRules: (p: Record<string, unknown>) => createQueryKeys().taskCenter.alertRules(p),
    alertEvents: (p: Record<string, unknown>) => createQueryKeys().taskCenter.alertEvents(p),
    exportTasks: (p: Record<string, unknown>) => createQueryKeys().taskCenter.exportTasks(p),
  },
} as const;

/** Type helper: extract key tuple type for typing custom hooks. */
export type QueryKeyOf<Fn extends (...args: any[]) => readonly unknown[]> = ReturnType<Fn>;
```

- [ ] **Step 4: Register `queryKeys.ts` for Jest compilation**

Modify `scripts/run-unit-tests.cjs`:

```diff
     path.join(projectRoot, 'src/utils/queryUtils.ts'),
+    path.join(projectRoot, 'src/utils/queryKeys.ts'),
```

Because `queryKeys.ts` imports from `@/services/request` via path alias, also add a `--paths` entry to the `tsc` invocation. Update the compile command to use `tsconfig.unit.json` instead of inline flags. Create `webapp/packages/supersonic-fe/tsconfig.unit.json`:

```json
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "module": "commonjs",
    "target": "es2019",
    "rootDir": "./src",
    "outDir": "./.tmp-unit",
    "esModuleInterop": true,
    "skipLibCheck": true,
    "noEmit": false,
    "isolatedModules": false,
    "declaration": false,
    "allowJs": false
  },
  "include": [
    "src/utils/menuFilter.ts",
    "src/pages/RouteGroupRedirect/resolveRedirect.ts",
    "src/pages/ReportSchedule/utils/scheduleFormValidation.ts",
    "src/utils/queryUtils.ts",
    "src/utils/queryKeys.ts",
    "src/services/request.ts"
  ]
}
```

Then simplify `scripts/run-unit-tests.cjs` to invoke `tsc -p tsconfig.unit.json`:

```js
// scripts/run-unit-tests.cjs (replace the body of compileMenuFilter)
function compileMenuFilter() {
  fs.rmSync(outDir, { recursive: true, force: true });
  execFileSync(process.execPath, [
    tscBin,
    '-p',
    path.join(projectRoot, 'tsconfig.unit.json'),
  ], { stdio: 'inherit' });
}
```

NOTE: `services/request.ts` imports from `@umijs/max` and `umi-request`. Those types are present in node_modules, so `skipLibCheck: true` is sufficient. If a runtime path like `@umijs/max` fails at Jest resolve time, shim it in `tests/unit/setupEnv.js`:

```js
// tests/unit/setupEnv.js — append
jest.mock('@umijs/max', () => ({ history: { location: { search: '' } } }), { virtual: true });
jest.mock('antd', () => ({ notification: { error: jest.fn() } }), { virtual: true });
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: PASS for `queryKeys.test.js` and unchanged for earlier tests.

- [ ] **Step 6: Commit**

```bash
git add webapp/packages/supersonic-fe/src/utils/queryKeys.ts \
        webapp/packages/supersonic-fe/tsconfig.unit.json \
        webapp/packages/supersonic-fe/scripts/run-unit-tests.cjs \
        webapp/packages/supersonic-fe/tests/unit/queryKeys.test.js \
        webapp/packages/supersonic-fe/tests/unit/setupEnv.js
git commit -m "feat(webapp): typed queryKeys factory namespaced by tenant"
```

---

## Task 4: Document the conversion recipe

**Files:**
- Create: `webapp/packages/supersonic-fe/src/hooks/queries/README.md`

- [ ] **Step 1: Write the recipe document**

Create `webapp/packages/supersonic-fe/src/hooks/queries/README.md`:

````md
# React Query Conversion Recipe

Migrate one `useEffect`-fetch at a time. Never rewrite the service layer.

## 4-Step Recipe

### 1. Leave the service function alone
Example service already in `src/services/reportSchedule.ts`:
```ts
export function getScheduleList(params: { current?: number; pageSize?: number }) {
  return request(BASE, { method: 'GET', params });
}
```

### 2. Create a `useXxxQuery` hook under `src/hooks/queries/<domain>.ts`
```ts
import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/utils/queryKeys';
import { getScheduleList } from '@/services/reportSchedule';

export function useScheduleListQuery(params: { current: number; pageSize: number }) {
  return useQuery({
    queryKey: queryKeys.reportSchedule.list(params),
    queryFn: () => getScheduleList(params),
    placeholderData: (prev) => prev, // keep last page while new page loads
  });
}
```

### 3. Replace `useEffect` + `useState` in the page
BEFORE:
```tsx
const [data, setData] = useState([]);
const [loading, setLoading] = useState(false);
useEffect(() => { (async () => {
  setLoading(true);
  try { setData((await getScheduleList({ current: 1, pageSize: 20 })).records); }
  finally { setLoading(false); }
})(); }, []);
```
AFTER:
```tsx
const [pagination, setPagination] = useState({ current: 1, pageSize: 20 });
const { data: resp, isLoading, isFetching } = useScheduleListQuery(pagination);
const rows = resp?.records ?? [];
```

### 4. Render loading/error consistently
Use `<QueryBoundary query={query}>` or pass `loading={isLoading}` to antd `Table`. Global errors are already surfaced by `queryClient`'s `QueryCache.onError` — DO NOT also call `message.error` locally unless the hook opts out via `meta: { silent: true }`.

## Mutation Recipe

```ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/utils/queryKeys';
import { createSchedule, updateSchedule } from '@/services/reportSchedule';

export function useScheduleSaveMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id?: number; values: Partial<ReportSchedule> }) =>
      v.id ? updateSchedule(v.id, v.values) : createSchedule(v.values),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() }),
  });
}
```
````

- [ ] **Step 2: Commit**

```bash
git add webapp/packages/supersonic-fe/src/hooks/queries/README.md
git commit -m "docs(webapp): add react-query conversion recipe"
```

---

## Task 5: Pilot migration — `ReportSchedule` list query

**Rationale for pilot:** `ReportSchedule` (`src/pages/ReportSchedule/index.tsx`) has three parallel `useEffect` fetches (schedules, delivery configs, dataset names), explicit loading state, a defensive `listLoadSucceededRef`, and mutations that refetch on success — it exercises list, cross-domain lookups, pagination, and cache invalidation in one page. If the pattern works here, it generalizes to every other list page.

**Files:**
- Create: `webapp/packages/supersonic-fe/src/hooks/queries/reportSchedule.ts`
- Modify: `webapp/packages/supersonic-fe/src/pages/ReportSchedule/index.tsx:25-86,107-125`

- [ ] **Step 1: Create the query hooks file**

```ts
// src/hooks/queries/reportSchedule.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/utils/queryKeys';
import {
  getScheduleList,
  getValidDataSetList,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  pauseSchedule,
  resumeSchedule,
  triggerSchedule,
  type ReportSchedule,
} from '@/services/reportSchedule';
import { getConfigList, type DeliveryConfig } from '@/services/deliveryConfig';

export interface ScheduleListParams {
  current: number;
  pageSize: number;
  datasetId?: number;
  enabled?: boolean;
}

export function useScheduleListQuery(params: ScheduleListParams) {
  return useQuery({
    queryKey: queryKeys.reportSchedule.list(params),
    queryFn: () => getScheduleList(params) as Promise<{ records: ReportSchedule[]; total: number }>,
    placeholderData: (prev) => prev,
  });
}

export function useValidDataSetMapQuery() {
  return useQuery({
    queryKey: queryKeys.dataSet.valid(),
    queryFn: async () => {
      const list = await getValidDataSetList();
      const arr = Array.isArray(list) ? list : [];
      const map: Record<number, string> = {};
      arr.forEach((d: { id: number; name: string }) => { map[d.id] = d.name; });
      return map;
    },
    staleTime: 5 * 60_000, // lookup data — cache longer
  });
}

export function useDeliveryConfigMapQuery() {
  return useQuery({
    queryKey: queryKeys.deliveryConfig.list({ pageNum: 1, pageSize: 100 }),
    queryFn: async () => {
      const res = await getConfigList({ pageNum: 1, pageSize: 100 });
      const map: Record<number, DeliveryConfig> = {};
      (res.records || []).forEach((c: DeliveryConfig) => { map[c.id] = c; });
      return map;
    },
    staleTime: 2 * 60_000,
  });
}

export function useScheduleSaveMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id?: number; values: Partial<ReportSchedule> }) =>
      v.id ? updateSchedule(v.id, v.values) : createSchedule(v.values),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() });
      qc.invalidateQueries({ queryKey: queryKeys.deliveryConfig.all() });
    },
  });
}

export function useScheduleDeleteMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteSchedule(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() }),
  });
}

export function useScheduleToggleMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; enabled: boolean }) =>
      v.enabled ? resumeSchedule(v.id) : pauseSchedule(v.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() }),
  });
}

export function useScheduleTriggerMutation() {
  return useMutation({
    mutationFn: (id: number) => triggerSchedule(id),
  });
}
```

- [ ] **Step 2: Migrate the list query in `pages/ReportSchedule/index.tsx`**

BEFORE (lines 26-86):

```tsx
const [data, setData] = useState<ReportSchedule[]>([]);
const [loading, setLoading] = useState(false);
const listLoadSucceededRef = useRef(false);
const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
// ...
const [deliveryConfigMap, setDeliveryConfigMap] = useState<Record<number, DeliveryConfig>>({});
const [datasetNameMap, setDatasetNameMap] = useState<Record<number, string>>({});

const fetchData = async (current = 1, pageSize = 20) => { /* manual */ };
const fetchDeliveryConfigs = async () => { /* manual */ };
const fetchDatasetNames = async () => { /* manual */ };

useEffect(() => {
  fetchData();
  fetchDeliveryConfigs();
  fetchDatasetNames();
}, []);
```

AFTER:

```tsx
import {
  useScheduleListQuery,
  useValidDataSetMapQuery,
  useDeliveryConfigMapQuery,
} from '@/hooks/queries/reportSchedule';

const [pagination, setPagination] = useState({ current: 1, pageSize: 20 });

const listQuery = useScheduleListQuery(pagination);
const deliveryConfigMapQuery = useDeliveryConfigMapQuery();
const datasetNameMapQuery = useValidDataSetMapQuery();

const data = (listQuery.data?.records ?? []) as ReportSchedule[];
const total = listQuery.data?.total ?? 0;
const loading = listQuery.isLoading || listQuery.isFetching;
const deliveryConfigMap = deliveryConfigMapQuery.data ?? {};
const datasetNameMap = datasetNameMapQuery.data ?? {};
```

- [ ] **Step 3: Update the `Table` pagination props**

In the `<Table>` element (line 286-291), replace the pagination block:

```tsx
pagination={{
  current: pagination.current,
  pageSize: pagination.pageSize,
  total,
  showSizeChanger: true,
  showTotal: (t) => `共 ${t} 条`,
  onChange: (page, size) => setPagination({ current: page, pageSize: size }),
}}
```

- [ ] **Step 4: Run dev server and verify page still renders**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run dev:os
```

Expected visual state (same as before the change):
- Table loads with a `<Spin>` overlay (antd `Table loading` prop).
- Rows populate from the `/api/v1/reportSchedules` response.
- Delivery-channel tags show with correct colors because `deliveryConfigMap` resolved.
- Dataset column shows `name (id)` format because `datasetNameMap` resolved.
- Clicking the pager refetches with the new page size — URL changes in network tab.
- If the backend returns 500, an antd `notification` error appears (new — from `QueryCache.onError`).

- [ ] **Step 5: Run type-check**

```bash
cd webapp/packages/supersonic-fe
pnpm run tsc
```

Expected: exits 0.

- [ ] **Step 6: Commit**

```bash
git add webapp/packages/supersonic-fe/src/hooks/queries/reportSchedule.ts \
        webapp/packages/supersonic-fe/src/pages/ReportSchedule/index.tsx
git commit -m "feat(webapp): migrate ReportSchedule list to useQuery (pilot)"
```

---

## Task 6: Pilot mutation — create/update/delete `ReportSchedule` via `useMutation`

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/pages/ReportSchedule/index.tsx:98-125`

- [ ] **Step 1: Replace `handleFormSubmit` with the save mutation**

BEFORE:

```tsx
const handleFormSubmit = async (values: Partial<ReportSchedule>) => {
  if (editRecord?.id) {
    await updateSchedule(editRecord.id, values);
    message.success(MSG.UPDATE_SUCCESS);
  } else {
    await createSchedule(values);
    message.success(MSG.CREATE_SUCCESS);
  }
  setFormVisible(false);
  fetchData(pagination.current, pagination.pageSize);
  fetchDeliveryConfigs();
};
```

AFTER:

```tsx
import {
  useScheduleSaveMutation,
  useScheduleDeleteMutation,
  useScheduleToggleMutation,
  useScheduleTriggerMutation,
} from '@/hooks/queries/reportSchedule';

const saveMutation = useScheduleSaveMutation();
const deleteMutation = useScheduleDeleteMutation();
const toggleMutation = useScheduleToggleMutation();
const triggerMutation = useScheduleTriggerMutation();

const handleFormSubmit = async (values: Partial<ReportSchedule>) => {
  await saveMutation.mutateAsync({ id: editRecord?.id, values });
  message.success(editRecord?.id ? MSG.UPDATE_SUCCESS : MSG.CREATE_SUCCESS);
  setFormVisible(false);
  // No refetch needed — onSuccess invalidated queryKeys.reportSchedule.all().
};
```

- [ ] **Step 2: Replace `handleDelete`**

```tsx
const handleDelete = async (id: number) => {
  await deleteMutation.mutateAsync(id);
  message.success(MSG.DELETE_SUCCESS);
};
```

- [ ] **Step 3: Replace `handleToggle` and `handleTrigger`**

```tsx
const handleToggle = async (record: ReportSchedule, checked: boolean) => {
  await toggleMutation.mutateAsync({ id: record.id, enabled: checked });
};

// Replace the manual Set-based in-flight tracking: useMutation.isPending is per hook instance,
// so we track a set locally for per-row button state (the page triggers multiple rows).
const [triggeringScheduleIds, setTriggeringScheduleIds] = useState<Record<number, boolean>>({});
const handleTrigger = async (id: number) => {
  if (triggeringScheduleIds[id]) return;
  setTriggeringScheduleIds((p) => ({ ...p, [id]: true }));
  try {
    await triggerMutation.mutateAsync(id);
    message.success('已触发执行');
  } finally {
    setTriggeringScheduleIds((p) => { const n = { ...p }; delete n[id]; return n; });
  }
};
```

- [ ] **Step 4: Remove unused imports**

In `src/pages/ReportSchedule/index.tsx` delete the now-unused direct imports:
- `createSchedule`, `updateSchedule`, `deleteSchedule`, `pauseSchedule`, `resumeSchedule`, `triggerSchedule`, `getValidDataSetList`, `getScheduleList`, `getConfigList` (still needed if unused — check before removing).
- The `useRef` import if `listLoadSucceededRef` and `triggeringScheduleIdsRef` are both removed.

- [ ] **Step 5: Verify in the dev server**

Run:

```bash
cd webapp/packages/supersonic-fe
pnpm run dev:os
```

Manual steps:
1. Create a schedule via the drawer — expect the table to automatically reflect the new row (no manual refetch) and `message.success` to show.
2. Edit that schedule — same.
3. Toggle the `enabled` switch — row refreshes.
4. Click "立即执行" twice fast — second click is disabled (row-level guard).
5. Delete — row disappears from the table.

- [ ] **Step 6: Commit**

```bash
git add webapp/packages/supersonic-fe/src/pages/ReportSchedule/index.tsx
git commit -m "feat(webapp): migrate ReportSchedule mutations to useMutation"
```

---

## Task 7: Integration test with `@testing-library/react` + `QueryClientProvider`

**Files:**
- Create: `webapp/packages/supersonic-fe/tests/unit/setupRtl.js`
- Create: `webapp/packages/supersonic-fe/tests/unit/reportScheduleQuery.test.js`
- Modify: `webapp/packages/supersonic-fe/jest.unit.config.cjs`

- [ ] **Step 1: Add RTL setup**

Create `webapp/packages/supersonic-fe/tests/unit/setupRtl.js`:

```js
require('@testing-library/jest-dom');
// Shim ResizeObserver for antd
global.ResizeObserver = global.ResizeObserver || class {
  observe() {}
  unobserve() {}
  disconnect() {}
};
// Shim matchMedia for antd
if (!global.window.matchMedia) {
  global.window.matchMedia = (q) => ({
    matches: false, media: q, addListener() {}, removeListener() {},
    addEventListener() {}, removeEventListener() {}, dispatchEvent() { return false; },
    onchange: null,
  });
}
```

- [ ] **Step 2: Register `setupRtl.js` in `jest.unit.config.cjs`**

```diff
 module.exports = {
   testEnvironment: 'jest-environment-jsdom',
   testMatch: ['<rootDir>/tests/unit/**/*.test.js'],
   passWithNoTests: false,
   verbose: false,
-  setupFiles: ['<rootDir>/tests/unit/setupEnv.js'],
+  setupFiles: ['<rootDir>/tests/unit/setupEnv.js'],
+  setupFilesAfterEach: ['<rootDir>/tests/unit/setupRtl.js'],
 };
```

- [ ] **Step 3: Write the failing test**

Create `webapp/packages/supersonic-fe/tests/unit/reportScheduleQuery.test.js`:

```js
/**
 * @jest-environment jsdom
 */
const React = require('react');
const { QueryClient, QueryClientProvider } = require('@tanstack/react-query');
const { renderHook, waitFor } = require('@testing-library/react');

// Stub the service module before requiring the hook.
jest.mock('../../.tmp-unit/services/reportSchedule.js', () => ({
  getScheduleList: jest.fn(),
  getValidDataSetList: jest.fn(),
  createSchedule: jest.fn(),
  updateSchedule: jest.fn(),
  deleteSchedule: jest.fn(),
  pauseSchedule: jest.fn(),
  resumeSchedule: jest.fn(),
  triggerSchedule: jest.fn(),
}), { virtual: true });
jest.mock('../../.tmp-unit/services/deliveryConfig.js', () => ({
  getConfigList: jest.fn(),
}), { virtual: true });

const reportScheduleSvc = require('../../.tmp-unit/services/reportSchedule.js');
const { useScheduleListQuery } = require('../../.tmp-unit/hooks/queries/reportSchedule.js');

function wrapper(client) {
  return ({ children }) => React.createElement(QueryClientProvider, { client }, children);
}

describe('useScheduleListQuery', () => {
  it('calls getScheduleList with params and returns records', async () => {
    reportScheduleSvc.getScheduleList.mockResolvedValueOnce({
      records: [{ id: 1, name: 'Daily KPI' }],
      total: 1,
    });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: 0 } },
    });
    const { result } = renderHook(
      () => useScheduleListQuery({ current: 1, pageSize: 20 }),
      { wrapper: wrapper(client) },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data.records).toHaveLength(1);
    expect(reportScheduleSvc.getScheduleList).toHaveBeenCalledWith({ current: 1, pageSize: 20 });
  });

  it('surfaces errors to the query result', async () => {
    reportScheduleSvc.getScheduleList.mockRejectedValueOnce(new Error('boom'));
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const { result } = renderHook(
      () => useScheduleListQuery({ current: 1, pageSize: 20 }),
      { wrapper: wrapper(client) },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error.message).toBe('boom');
  });
});
```

- [ ] **Step 4: Compile the hook + its service deps for Jest**

Update `tsconfig.unit.json` `include` to add:

```json
    "src/hooks/queries/reportSchedule.ts",
    "src/services/reportSchedule.ts",
    "src/services/deliveryConfig.ts",
    "src/utils/queryClient.ts"
```

Since `queryClient.ts` imports antd and the compiled hook indirectly pulls it, the `jest.mock('antd', ...)` in `tests/unit/setupEnv.js` (added in Task 3) must also export `Spin`, `notification`, etc. Expand the shim:

```js
// tests/unit/setupEnv.js — replace the antd mock
jest.mock('antd', () => ({
  notification: { error: jest.fn(), success: jest.fn() },
  message: { error: jest.fn(), success: jest.fn() },
  Spin: () => null,
}), { virtual: true });
```

- [ ] **Step 5: Run tests**

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: all tests PASS, including the two new `useScheduleListQuery` cases.

- [ ] **Step 6: Commit**

```bash
git add webapp/packages/supersonic-fe/tests/unit/setupRtl.js \
        webapp/packages/supersonic-fe/tests/unit/reportScheduleQuery.test.js \
        webapp/packages/supersonic-fe/jest.unit.config.cjs \
        webapp/packages/supersonic-fe/tsconfig.unit.json \
        webapp/packages/supersonic-fe/tests/unit/setupEnv.js
git commit -m "test(webapp): useScheduleListQuery success + error cases"
```

---

## Task 8: `<QueryBoundary>` helper for consistent loading + error UX

**Files:**
- Create: `webapp/packages/supersonic-fe/src/components/QueryBoundary/index.tsx`
- Create: `webapp/packages/supersonic-fe/tests/unit/queryBoundary.test.js`

- [ ] **Step 1: Write the failing RTL test**

Create `webapp/packages/supersonic-fe/tests/unit/queryBoundary.test.js`:

```js
/**
 * @jest-environment jsdom
 */
const React = require('react');
const { render, screen } = require('@testing-library/react');

// Before requiring QueryBoundary, partially unmock antd to expose real components we need.
jest.unmock('antd');

const { default: QueryBoundary } = require('../../.tmp-unit/components/QueryBoundary/index.js');

describe('<QueryBoundary>', () => {
  it('renders Skeleton when loading', () => {
    const query = { isLoading: true, isError: false, data: undefined, error: null };
    render(React.createElement(QueryBoundary, { query }, 'content'));
    expect(document.querySelector('.ant-skeleton')).toBeTruthy();
  });

  it('renders Result when error (and no cached data)', () => {
    const query = { isLoading: false, isError: true, data: undefined, error: new Error('x') };
    render(React.createElement(QueryBoundary, { query }, 'content'));
    expect(screen.getByText(/加载失败/)).toBeInTheDocument();
  });

  it('renders children when data is present', () => {
    const query = { isLoading: false, isError: false, data: { ok: true }, error: null };
    render(React.createElement(QueryBoundary, { query }, React.createElement('div', null, 'hello')));
    expect(screen.getByText('hello')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: FAIL — `QueryBoundary` not compiled yet.

- [ ] **Step 3: Create `<QueryBoundary>`**

```tsx
// src/components/QueryBoundary/index.tsx
import React from 'react';
import { Skeleton, Result, Button } from 'antd';
import type { UseQueryResult } from '@tanstack/react-query';

interface Props<TData> {
  query: Pick<UseQueryResult<TData>, 'isLoading' | 'isError' | 'data' | 'error' | 'refetch'>
       & Partial<Pick<UseQueryResult<TData>, 'isFetching'>>;
  skeletonRows?: number;
  /**
   * If true, fall through to children even on error when cached data exists.
   * Default true — keep user's view stable on transient failures.
   */
  keepStaleOnError?: boolean;
  errorTitle?: string;
  children: React.ReactNode;
}

function QueryBoundary<TData>({
  query,
  skeletonRows = 4,
  keepStaleOnError = true,
  errorTitle = '加载失败',
  children,
}: Props<TData>) {
  if (query.isLoading && !query.data) {
    return <Skeleton active paragraph={{ rows: skeletonRows }} />;
  }
  if (query.isError && !(keepStaleOnError && query.data)) {
    const msg = (query.error as { message?: string } | null)?.message;
    return (
      <Result
        status="error"
        title={errorTitle}
        subTitle={msg}
        extra={
          query.refetch ? (
            <Button type="primary" onClick={() => query.refetch!()}>重试</Button>
          ) : null
        }
      />
    );
  }
  return <>{children}</>;
}

export default QueryBoundary;
```

- [ ] **Step 4: Register for Jest compilation**

Add to `tsconfig.unit.json` `include`:

```json
    "src/components/QueryBoundary/index.tsx"
```

And change `tsconfig.unit.json` `"jsx"`:

```diff
   "compilerOptions": {
     ...
+    "jsx": "react",
     ...
   },
```

Also remove the antd mock for this test by splitting setups: keep a bare-antd-mock for the non-RTL tests and unmock for RTL tests with `jest.unmock('antd')`.

- [ ] **Step 5: Run tests**

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: all PASS including the three `<QueryBoundary>` cases.

- [ ] **Step 6: Apply `<QueryBoundary>` to the ReportSchedule page (optional usage)**

For the pilot page, the antd `<Table loading={isLoading}>` + `<Spin>` default already gives consistent UX, so the component is not required there. But future pages (e.g. detail drawers or charts) should use it. Add a usage example in `src/hooks/queries/README.md`:

```md
### Using QueryBoundary for non-Table contents
```tsx
const q = useScheduleDetailQuery(id);
return <QueryBoundary query={q}><ScheduleDetailView data={q.data} /></QueryBoundary>;
```
```

- [ ] **Step 7: Commit**

```bash
git add webapp/packages/supersonic-fe/src/components/QueryBoundary/index.tsx \
        webapp/packages/supersonic-fe/tests/unit/queryBoundary.test.js \
        webapp/packages/supersonic-fe/tsconfig.unit.json \
        webapp/packages/supersonic-fe/src/hooks/queries/README.md
git commit -m "feat(webapp): QueryBoundary helper for Skeleton/Result UX"
```

---

## Task 9: Optimistic update — delete row with rollback on failure

**Files:**
- Modify: `webapp/packages/supersonic-fe/src/hooks/queries/reportSchedule.ts`
- Test: `webapp/packages/supersonic-fe/tests/unit/scheduleOptimisticDelete.test.js`

- [ ] **Step 1: Write the failing test**

Create `webapp/packages/supersonic-fe/tests/unit/scheduleOptimisticDelete.test.js`:

```js
/**
 * @jest-environment jsdom
 */
const React = require('react');
const { QueryClient, QueryClientProvider } = require('@tanstack/react-query');
const { renderHook, waitFor, act } = require('@testing-library/react');

jest.mock('../../.tmp-unit/services/reportSchedule.js', () => ({
  getScheduleList: jest.fn(),
  getValidDataSetList: jest.fn(),
  createSchedule: jest.fn(),
  updateSchedule: jest.fn(),
  deleteSchedule: jest.fn(),
  pauseSchedule: jest.fn(),
  resumeSchedule: jest.fn(),
  triggerSchedule: jest.fn(),
}), { virtual: true });
jest.mock('../../.tmp-unit/services/deliveryConfig.js', () => ({ getConfigList: jest.fn() }), { virtual: true });

const svc = require('../../.tmp-unit/services/reportSchedule.js');
const {
  useScheduleDeleteMutation,
  useScheduleListQuery,
} = require('../../.tmp-unit/hooks/queries/reportSchedule.js');

function wrap(client) {
  return ({ children }) => React.createElement(QueryClientProvider, { client }, children);
}

describe('optimistic delete', () => {
  it('removes row immediately and keeps it removed on success', async () => {
    svc.getScheduleList.mockResolvedValue({ records: [{ id: 1, name: 'a' }, { id: 2, name: 'b' }], total: 2 });
    svc.deleteSchedule.mockResolvedValue(undefined);
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const listHook = renderHook(() => useScheduleListQuery({ current: 1, pageSize: 20 }), { wrapper: wrap(client) });
    const mutHook = renderHook(() => useScheduleDeleteMutation(), { wrapper: wrap(client) });
    await waitFor(() => expect(listHook.result.current.isSuccess).toBe(true));

    await act(async () => { await mutHook.result.current.mutateAsync(1); });
    // Optimistic update is via invalidation + setQueryData in onMutate
    expect(listHook.result.current.data.records.find((r) => r.id === 1)).toBeUndefined();
  });

  it('rolls back on failure', async () => {
    svc.getScheduleList.mockResolvedValue({ records: [{ id: 1, name: 'a' }, { id: 2, name: 'b' }], total: 2 });
    svc.deleteSchedule.mockRejectedValue(new Error('nope'));
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const listHook = renderHook(() => useScheduleListQuery({ current: 1, pageSize: 20 }), { wrapper: wrap(client) });
    const mutHook = renderHook(() => useScheduleDeleteMutation(), { wrapper: wrap(client) });
    await waitFor(() => expect(listHook.result.current.isSuccess).toBe(true));

    await act(async () => {
      try { await mutHook.result.current.mutateAsync(1); } catch (_) {}
    });
    // rollback restores the row
    await waitFor(() => {
      expect(listHook.result.current.data.records.find((r) => r.id === 1)).toBeDefined();
    });
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: FAIL on the "removes row immediately" assertion (current `useScheduleDeleteMutation` only invalidates, not optimistic).

- [ ] **Step 3: Add optimistic update to `useScheduleDeleteMutation`**

Edit `src/hooks/queries/reportSchedule.ts`, replace `useScheduleDeleteMutation`:

```ts
export function useScheduleDeleteMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteSchedule(id),
    onMutate: async (id) => {
      // Cancel any in-flight refetch so it doesn't overwrite our optimistic state.
      await qc.cancelQueries({ queryKey: queryKeys.reportSchedule.all() });
      // Snapshot all list queries (pagination produces many keys).
      const snapshots = qc.getQueriesData<{ records: ReportSchedule[]; total: number }>({
        queryKey: queryKeys.reportSchedule.all(),
      });
      snapshots.forEach(([key, data]) => {
        if (!data) return;
        qc.setQueryData(key, {
          ...data,
          records: data.records.filter((r) => r.id !== id),
          total: Math.max(0, (data.total ?? 0) - 1),
        });
      });
      return { snapshots };
    },
    onError: (_err, _id, ctx) => {
      ctx?.snapshots.forEach(([key, data]) => qc.setQueryData(key, data));
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: queryKeys.reportSchedule.all() });
    },
  });
}
```

- [ ] **Step 4: Run tests again**

```bash
cd webapp/packages/supersonic-fe
pnpm run test:unit
```

Expected: both cases PASS.

- [ ] **Step 5: Manually verify in dev server**

```bash
cd webapp/packages/supersonic-fe
pnpm run dev:os
```

- Delete a row — row disappears immediately; brief network call; no flicker.
- Simulate a 500 (devtools "Network blocked" or backend down) — row reappears within ~1s and a `notification.error` fires.

- [ ] **Step 6: Commit**

```bash
git add webapp/packages/supersonic-fe/src/hooks/queries/reportSchedule.ts \
        webapp/packages/supersonic-fe/tests/unit/scheduleOptimisticDelete.test.js
git commit -m "feat(webapp): optimistic delete for ReportSchedule with rollback"
```

---

## Task 10: Rollout plan for remaining pages

**Files:**
- Create: `docs/superpowers/plans/2026-04-17-p2-10-rollout.md` (tracking checklist only; linked from the frontend-data-layer guide in Task 11).

- [ ] **Step 1: Write the rollout tracker**

Create `docs/superpowers/plans/2026-04-17-p2-10-rollout.md`:

```md
# P2-10 Rollout Tracker

Definition of done (per page): zero `useEffect` blocks that kick off a data fetch. All remote reads go through `useQuery`; all writes through `useMutation`. Loading uses antd `<Table loading>`, `<Spin>`, `<Skeleton>`, or `<QueryBoundary>`.

**Rollback strategy:** each page is migrated in its own commit. Keep the old service calls imported until the page is fully cut over; if production regresses, revert the single page commit.

Order is least-risk first, most-risk last.

| # | Page | Path | Queries today | Risk | Status |
|---|------|------|---------------|------|--------|
| 1 | ReportSchedule list (pilot) | `pages/ReportSchedule/index.tsx` | list + delivery + datasets | L | DONE Task 5-6 |
| 2 | ReportSchedule ExecutionList | `pages/ReportSchedule/components/ExecutionList.tsx` | executions, download | L | [ ] |
| 3 | TaskCenter ScheduleTab | `pages/TaskCenter/ScheduleTab.tsx` | schedules | L | [ ] |
| 4 | TaskCenter AlertEventsTab | `pages/TaskCenter/AlertEventsTab.tsx` | events, filters | M | [ ] |
| 5 | TaskCenter AlertRuleTab | `pages/TaskCenter/AlertRuleTab.tsx` | rules | L | [ ] |
| 6 | TaskCenter ExportTaskTab | `pages/TaskCenter/ExportTaskTab.tsx` | tasks, download | M | [ ] |
| 7 | DeliveryConfig | `pages/DeliveryConfig/` | list + forms | L | [ ] |
| 8 | Connections | `pages/Connections/` | list + test | L | [ ] |
| 9 | Reports | `pages/Reports/` | fixed-reports list + preview | M | [ ] |
| 10 | OperationsCockpit | `pages/OperationsCockpit/` | dashboard widgets | M | [ ] |
| 11 | BusinessTopics | `pages/BusinessTopics/` | CRUD | L | [ ] |
| 12 | SemanticTemplate | `pages/SemanticTemplate/` | templates, deploy, history | H | [ ] |
| 13 | SemanticModel | `pages/SemanticModel/` | domain/model/metric trees | H | [ ] |
| 14 | TenantSettings / AdminTenant / Tenant/* | `pages/Tenant*/`, `pages/AdminTenant/` | RBAC, roles, permissions | H | [ ] |
| 15 | Agent | `pages/Agent/` | agent CRUD | M | [ ] |
| 16 | ChatPage | `pages/ChatPage/` | conversation streams | H (SSE — may stay manual) | [ ] |
| 17 | FeishuBot | `pages/FeishuBot/` | mapping, logs | M | [ ] |
| 18 | UsageDashboard | `pages/UsageDashboard/` | metrics | M | [ ] |
| 19 | ResponsibilityLedger | `pages/ResponsibilityLedger/` | ledger list | L | [ ] |
| 20 | System / Platform | `pages/System/`, `pages/Platform/` | config screens | M | [ ] |
| 21 | Login | `pages/Login/index.tsx` | login mutation only | L | [ ] |

Each migration PR:
1. Add the domain hook file in `src/hooks/queries/<domain>.ts`.
2. Replace exactly one page's `useEffect`-fetches.
3. Add at least one `renderHook` test for the new query hook.
4. Update `docs/details/platform/04-frontend-data-layer.md` "Migrated Pages" list.

`Copilot` (chat SDK, SSE) is intentionally out-of-scope for React Query — streaming state machines are a different shape.
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/plans/2026-04-17-p2-10-rollout.md
git commit -m "docs: P2-10 frontend migration rollout tracker"
```

---

## Task 11: Developer documentation

**Files:**
- Modify: `webapp/packages/supersonic-fe/README.md`
- Create: `docs/details/platform/04-frontend-data-layer.md`

- [ ] **Step 1: Append a section to `webapp/packages/supersonic-fe/README.md`**

Append to the end of `webapp/packages/supersonic-fe/README.md`:

```md
## Data Fetching with React Query

Since P2-10, all new remote reads go through **TanStack Query v5**. See:

- `src/utils/queryClient.ts` — shared `QueryClient` with `staleTime: 30s`, `refetchOnWindowFocus: false`, 4xx-skipping retry, and global `notification.error` for 5xx/network errors.
- `src/utils/queryKeys.ts` — typed `queryKeys` factory namespaced by `tenantId` from `localStorage`.
- `src/hooks/queries/<domain>.ts` — `useXxxQuery` / `useXxxMutation` hooks.
- `src/hooks/queries/README.md` — 4-step conversion recipe with before/after example.
- `src/components/QueryBoundary/index.tsx` — `<QueryBoundary>` for `Skeleton` / `Result` rendering.

DevTools are enabled automatically when `REACT_APP_ENV !== 'prod'`.

### Writing a new query
```ts
export function useMyListQuery(params: MyParams) {
  return useQuery({
    queryKey: queryKeys.myDomain.list(params),
    queryFn: () => getMyList(params),
    placeholderData: (prev) => prev,
  });
}
```

### Writing a new mutation
```ts
const qc = useQueryClient();
const save = useMutation({
  mutationFn: (v) => v.id ? update(v.id, v.values) : create(v.values),
  onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.myDomain.all() }),
});
```

DO NOT call `message.error` manually on failure — the global handler already does it. Opt out with `meta: { silent: true }` on the hook if a page needs inline error UI instead.

### Tests
Query hooks are tested with `@testing-library/react`'s `renderHook` wrapped in a fresh `QueryClient`. See `tests/unit/reportScheduleQuery.test.js` for the canonical pattern.

Run: `pnpm run test:unit` (jsdom + Jest via `scripts/run-unit-tests.cjs`).
```

- [ ] **Step 2: Create `docs/details/platform/04-frontend-data-layer.md`**

```md
---
status: active
module: webapp
key-files:
  - webapp/packages/supersonic-fe/src/utils/queryClient.ts
  - webapp/packages/supersonic-fe/src/utils/queryKeys.ts
  - webapp/packages/supersonic-fe/src/hooks/queries/*.ts
  - webapp/packages/supersonic-fe/src/components/QueryBoundary/index.tsx
  - webapp/packages/supersonic-fe/src/app.tsx (rootContainer)
---

# Frontend Data Layer (React Query)

## 边界

- **传输层** 仍然使用 `umi-request`（见 `src/services/request.ts`）。React Query 负责缓存、去重、重试、错误 UX；不替换 API 调用。
- **单向依赖**：`pages/*` → `hooks/queries/<domain>` → `services/<domain>` → `services/request`。`hooks/queries/*` 不允许直接调用 `request`，必须复用 service 导出的函数。
- **租户隔离**：Query keys 第一段为 `t:<tenantId>`，从 `localStorage.X-Tenant-Id` 读取（`services/request.ts:getStoredTenantIdNumber`）。切换租户时如果不刷新页面，缓存自然隔离——但**推荐 reload**，因为 umi-request 的拦截器基于 `localStorage` 在请求时读取。

## 主链路

```
<QueryClientProvider>                      // src/app.tsx rootContainer
    │
    └── page component
            │
            ├── useXxxQuery()              // src/hooks/queries/<domain>.ts
            │     └── queryFn: getXxxList() // src/services/<domain>.ts
            │           └── request(...)    // src/services/request.ts (umi-request + headers)
            │
            └── useXxxMutation()
                  ├── mutationFn: updateXxx()
                  └── onSuccess/onMutate → queryClient.invalidateQueries / setQueryData
```

全局错误处理：`QueryCache.onError` + `MutationCache.onError` 调用 antd `notification.error`；401/403 由 `responseInterceptor` 直接重定向到 `/login`，不重复弹窗。

## 默认值

| 配置 | 值 | 原因 |
|------|-----|------|
| `staleTime` | 30s | 业务工具，非实时看板；避免重复请求 |
| `gcTime` | 5min | 默认 |
| `refetchOnWindowFocus` | false | 避免后台标签页切回时打断用户 |
| `retry` | 一次，跳过 4xx | 4xx 是业务错误，重试无意义 |
| Devtools | `REACT_APP_ENV !== 'prod'` | 生产环境剥离 |

## 迁移策略

见 `docs/superpowers/plans/2026-04-17-p2-10-rollout.md`。按页逐个迁移，每页一次 commit，可单独回滚。

## 测试

- 单测：`renderHook` + `QueryClientProvider` wrapper；在 hook 层 mock `src/services/*`。
- 代表性测试：`tests/unit/reportScheduleQuery.test.js`、`tests/unit/scheduleOptimisticDelete.test.js`。

## 注意事项

- **不要** 在页面里再写 `try/catch` + `message.error`——全局 handler 已处理。若确需页面内错误 UI，在 hook 调用处传 `meta: { silent: true }` 并自行处理 `query.error`。
- **不要** 把 `queryClient` 直接 import 到组件里手动 `fetchQuery`；使用 hook。
- **不要** 在 mutation 成功后手动 `refetch`——改用 `invalidateQueries`。
- **优化更新** 只在"用户会注意延迟"的地方使用（如列表删除）。一般更新保持简单 invalidate。
- SSE / 流式响应（如 `Copilot`）不适合 React Query，保持现有模式。
```

- [ ] **Step 3: Commit**

```bash
git add webapp/packages/supersonic-fe/README.md \
        docs/details/platform/04-frontend-data-layer.md
git commit -m "docs: frontend data-layer guide for react-query migration"
```

---

## Self-Review

1. **Spec coverage:** All 12 plan requirements addressed (Tasks 1-11 correspond to request items 1-12, with rollout (Task 10) + docs (Task 11) split cleanly). File structure section lists every new file. Conversion recipe appears in Task 4 with concrete before/after code. Pilot page rationale appears at the top of Task 5.
2. **Placeholder scan:** Every code step has complete code. No `TODO`, `TBD`, or "similar to" placeholders. Every `git commit` has an explicit message. All `pnpm` commands are concrete.
3. **Type consistency:** `useScheduleListQuery`, `useScheduleSaveMutation`, `useScheduleDeleteMutation`, `useScheduleToggleMutation`, `useScheduleTriggerMutation`, `useValidDataSetMapQuery`, `useDeliveryConfigMapQuery` names match across Tasks 5/6/7/9. `queryKeys.reportSchedule.all/list/detail/executions` match across Tasks 3/5/9. `QueryBoundary` props (`query`, `skeletonRows`, `keepStaleOnError`, `errorTitle`) consistent Task 8. `extractHttpStatus` + `shouldRetry` signatures match Task 2 test and implementation.
