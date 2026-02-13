import type { SemanticTemplateConfig } from '@/services/semanticTemplate';

export type ChangeCategory =
  | 'domain'
  | 'models'
  | 'dimensions'
  | 'measures'
  | 'dataSet'
  | 'agent'
  | 'terms'
  | 'configParams';

export type ChangeType = 'added' | 'removed' | 'modified';

export interface ConfigChange {
  category: ChangeCategory;
  type: ChangeType;
  name: string;
  detail?: string;
  parentName?: string;
}

/**
 * Compare two SemanticTemplateConfig objects and produce a structured change list.
 * Objects are matched by `bizName` (or `name`/`key` for singletons and params).
 */
export function diffTemplateConfig(
  oldConfig: SemanticTemplateConfig | undefined,
  newConfig: SemanticTemplateConfig | undefined,
): ConfigChange[] {
  const changes: ConfigChange[] = [];
  const old = oldConfig || {};
  const cur = newConfig || {};

  // --- Domain (singleton) ---
  diffSingleton(changes, 'domain', old.domain, cur.domain, ['name', 'bizName', 'description']);

  // --- Models ---
  const oldModels = old.models || [];
  const newModels = cur.models || [];
  const oldModelMap = toMap(oldModels, 'bizName');
  const newModelMap = toMap(newModels, 'bizName');

  for (const [key, newModel] of newModelMap) {
    const oldModel = oldModelMap.get(key);
    if (!oldModel) {
      changes.push({ category: 'models', type: 'added', name: newModel.name || key });
    } else {
      const diff = diffFields(oldModel, newModel, ['name', 'tableName', 'sqlQuery']);
      if (diff) {
        changes.push({ category: 'models', type: 'modified', name: newModel.name || key, detail: diff });
      }
      // Nested: dimensions
      diffNestedArray(
        changes,
        'dimensions',
        oldModel.dimensions || [],
        newModel.dimensions || [],
        'bizName',
        ['name', 'fieldName', 'type', 'expr'],
        newModel.name || key,
      );
      // Nested: measures
      diffNestedArray(
        changes,
        'measures',
        oldModel.measures || [],
        newModel.measures || [],
        'bizName',
        ['name', 'fieldName', 'aggOperator', 'expr'],
        newModel.name || key,
      );
    }
  }
  for (const [key, oldModel] of oldModelMap) {
    if (!newModelMap.has(key)) {
      changes.push({ category: 'models', type: 'removed', name: oldModel.name || key });
    }
  }

  // --- DataSet (singleton) ---
  diffSingleton(changes, 'dataSet', old.dataSet, cur.dataSet, ['name', 'description']);

  // --- Agent (singleton) ---
  diffSingleton(changes, 'agent', old.agent, cur.agent, ['name', 'description']);

  // --- Terms ---
  diffNamedArray(changes, 'terms', old.terms || [], cur.terms || [], 'name', [
    'description',
  ]);

  // --- ConfigParams ---
  diffNamedArray(changes, 'configParams', old.configParams || [], cur.configParams || [], 'key', [
    'name',
    'type',
    'defaultValue',
    'required',
  ]);

  return changes;
}

/**
 * Group changes by category for display.
 */
export function groupChanges(changes: ConfigChange[]): Record<ChangeCategory, ConfigChange[]> {
  const categories: ChangeCategory[] = [
    'domain',
    'models',
    'dimensions',
    'measures',
    'dataSet',
    'agent',
    'terms',
    'configParams',
  ];
  const grouped = {} as Record<ChangeCategory, ConfigChange[]>;
  for (const cat of categories) {
    grouped[cat] = [];
  }
  for (const c of changes) {
    grouped[c.category].push(c);
  }
  return grouped;
}

export const CATEGORY_LABELS: Record<ChangeCategory, string> = {
  domain: '主题域',
  models: '模型',
  dimensions: '维度',
  measures: '度量/指标',
  dataSet: '数据集',
  agent: 'Agent',
  terms: '术语',
  configParams: '配置参数',
};

// ---- internal helpers ----

function toMap<T>(arr: T[], key: string): Map<string, T> {
  const m = new Map<string, T>();
  for (const item of arr) {
    const k = (item as any)[key];
    if (k != null) m.set(String(k), item);
  }
  return m;
}

function diffFields(oldObj: any, newObj: any, fields: string[]): string | undefined {
  const diffs: string[] = [];
  for (const f of fields) {
    const ov = stringify(oldObj[f]);
    const nv = stringify(newObj[f]);
    if (ov !== nv) {
      diffs.push(`${f}: ${truncate(ov)} → ${truncate(nv)}`);
    }
  }
  return diffs.length > 0 ? diffs.join('; ') : undefined;
}

function diffSingleton(
  changes: ConfigChange[],
  category: ChangeCategory,
  oldObj: any,
  newObj: any,
  fields: string[],
) {
  if (!oldObj && !newObj) return;
  if (!oldObj && newObj) {
    changes.push({ category, type: 'added', name: newObj.name || category });
    return;
  }
  if (oldObj && !newObj) {
    changes.push({ category, type: 'removed', name: oldObj.name || category });
    return;
  }
  const diff = diffFields(oldObj, newObj, fields);
  if (diff) {
    changes.push({ category, type: 'modified', name: newObj.name || category, detail: diff });
  }
}

function diffNamedArray(
  changes: ConfigChange[],
  category: ChangeCategory,
  oldArr: any[],
  newArr: any[],
  matchKey: string,
  compareFields: string[],
) {
  const oldMap = toMap(oldArr, matchKey);
  const newMap = toMap(newArr, matchKey);

  for (const [key, newItem] of newMap) {
    const oldItem = oldMap.get(key);
    if (!oldItem) {
      changes.push({ category, type: 'added', name: (newItem as any).name || key });
    } else {
      const diff = diffFields(oldItem, newItem, compareFields);
      if (diff) {
        changes.push({
          category,
          type: 'modified',
          name: (newItem as any).name || key,
          detail: diff,
        });
      }
    }
  }
  for (const [key, oldItem] of oldMap) {
    if (!newMap.has(key)) {
      changes.push({ category, type: 'removed', name: (oldItem as any).name || key });
    }
  }
}

function diffNestedArray(
  changes: ConfigChange[],
  category: ChangeCategory,
  oldArr: any[],
  newArr: any[],
  matchKey: string,
  compareFields: string[],
  parentName: string,
) {
  const oldMap = toMap(oldArr, matchKey);
  const newMap = toMap(newArr, matchKey);

  for (const [key, newItem] of newMap) {
    const oldItem = oldMap.get(key);
    if (!oldItem) {
      changes.push({
        category,
        type: 'added',
        name: (newItem as any).name || key,
        parentName,
      });
    } else {
      const diff = diffFields(oldItem, newItem, compareFields);
      if (diff) {
        changes.push({
          category,
          type: 'modified',
          name: (newItem as any).name || key,
          detail: diff,
          parentName,
        });
      }
    }
  }
  for (const [key, oldItem] of oldMap) {
    if (!newMap.has(key)) {
      changes.push({
        category,
        type: 'removed',
        name: (oldItem as any).name || key,
        parentName,
      });
    }
  }
}

function stringify(val: any): string {
  if (val == null) return '';
  if (typeof val === 'object') return JSON.stringify(val);
  return String(val);
}

function truncate(s: string, max = 60): string {
  return s.length > max ? s.slice(0, max) + '...' : s;
}
