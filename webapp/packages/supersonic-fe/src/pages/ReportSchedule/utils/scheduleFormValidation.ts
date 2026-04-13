export type ScheduleDateMode = 'BETWEEN' | 'RECENT' | 'ALL';
export type ScheduleQueryType = 'DETAIL' | 'AGGREGATE';

export type ScheduleDateInfoInput = {
  dateMode?: ScheduleDateMode;
  dateField?: string;
  dateRange?: [unknown, unknown] | unknown[];
  recentUnit?: number;
  queryType?: ScheduleQueryType;
};

export type ScheduleDateInfoValidation =
  | { ok: true }
  | { ok: false; error: string };

export type ScheduleDateFieldInput = {
  formDateField?: string;
  partitionDimension?: string;
  recordDateField?: string;
};

/**
 * dateField 取值链:表单值 → 数据集分区字段 → 原始 queryConfig 的 dateField。
 * 用于编辑态下 antd 表单 store 丢值时兜底;正常路径第一项就命中,后两项只在
 * 跨分步条件渲染 / 数据集无 partition_time 的边界场景触发。
 */
export function resolveScheduleDateField(input: ScheduleDateFieldInput): string | undefined {
  return input.formDateField || input.partitionDimension || input.recordDateField;
}

/**
 * Pure validator for the ReportSchedule form's dateInfo block. Mirrors the backend
 * invariants in ReportScheduleServiceImpl#validateQueryConfig so the user gets the
 * same error in the browser before a request is even sent.
 */
export function validateScheduleDateInfo(
  input: ScheduleDateInfoInput,
): ScheduleDateInfoValidation {
  const mode: ScheduleDateMode = input.dateMode || 'BETWEEN';
  if (mode === 'BETWEEN') {
    if (!input.dateField) {
      return { ok: false, error: '请选择或填写日期字段' };
    }
    const start = input.dateRange?.[0];
    const end = input.dateRange?.[1];
    if (!start || !end) {
      return { ok: false, error: '请选择日期范围' };
    }
    return { ok: true };
  }
  if (mode === 'RECENT') {
    if (!input.dateField) {
      return { ok: false, error: '请选择或填写日期字段' };
    }
    if (!input.recentUnit) {
      return { ok: false, error: '请输入最近 N 天的天数' };
    }
    return { ok: true };
  }
  if (mode === 'ALL' && input.queryType === 'DETAIL') {
    return {
      ok: false,
      error: '明细调度不支持全量模式，请选择固定区间或最近 N 天',
    };
  }
  return { ok: true };
}
