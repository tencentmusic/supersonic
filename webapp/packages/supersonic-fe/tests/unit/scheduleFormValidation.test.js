const {
  resolveScheduleDateField,
  validateScheduleDateInfo,
} = require('../../.tmp-unit/pages/ReportSchedule/utils/scheduleFormValidation.js');

describe('resolveScheduleDateField', () => {
  it('uses the explicit form dateField first', () => {
    expect(
      resolveScheduleDateField({
        formDateField: 'form_workday',
        partitionDimension: 'partition_workday',
        recordDateField: 'record_workday',
      }),
    ).toBe('form_workday');
  });

  it('falls back to dataset partitionDimension when hidden form dateField is empty', () => {
    expect(
      resolveScheduleDateField({
        partitionDimension: 'workday',
        recordDateField: 'record_workday',
      }),
    ).toBe('workday');
  });

  it('falls back to the original queryConfig dateField when dataset metadata has no partition', () => {
    expect(
      resolveScheduleDateField({
        recordDateField: 'record_workday',
      }),
    ).toBe('record_workday');
  });

  it('returns undefined when no source has a dateField', () => {
    expect(resolveScheduleDateField({})).toBeUndefined();
  });

  it('treats empty string formDateField as missing and falls back', () => {
    expect(
      resolveScheduleDateField({
        formDateField: '',
        partitionDimension: 'workday',
      }),
    ).toBe('workday');
  });
});

describe('validateScheduleDateInfo', () => {
  describe('BETWEEN mode', () => {
    it('passes when dateField and both ends are present', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'BETWEEN',
        dateField: 'workday',
        dateRange: ['2026-01-01', '2026-01-07'],
      });
      expect(result.ok).toBe(true);
    });

    it('reports missing dateField separately from range', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'BETWEEN',
        dateRange: ['2026-01-01', '2026-01-07'],
      });
      expect(result.ok).toBe(false);
      expect(result.error).toBe('请选择或填写日期字段');
    });

    it('reports missing range when only one end is set', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'BETWEEN',
        dateField: 'workday',
        dateRange: ['2026-01-01', undefined],
      });
      expect(result.ok).toBe(false);
      expect(result.error).toBe('请选择日期范围');
    });

    it('reports missing range when dateRange is absent', () => {
      // Original P0 — legacy ALL silently coerced to BETWEEN with no range.
      const result = validateScheduleDateInfo({
        dateMode: 'BETWEEN',
        dateField: 'workday',
      });
      expect(result.ok).toBe(false);
      expect(result.error).toBe('请选择日期范围');
    });

    it('defaults to BETWEEN and reports missing dateField first', () => {
      const result = validateScheduleDateInfo({});
      expect(result.ok).toBe(false);
      expect(result.error).toBe('请选择或填写日期字段');
    });
  });

  describe('RECENT mode', () => {
    it('passes with dateField and recentUnit', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'RECENT',
        dateField: 'workday',
        recentUnit: 7,
      });
      expect(result.ok).toBe(true);
    });

    it('reports missing recentUnit with a targeted message', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'RECENT',
        dateField: 'workday',
        recentUnit: 0,
      });
      expect(result.ok).toBe(false);
      expect(result.error).toBe('请输入最近 N 天的天数');
    });

    it('reports missing dateField with a targeted message', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'RECENT',
        recentUnit: 7,
      });
      expect(result.ok).toBe(false);
      expect(result.error).toBe('请选择或填写日期字段');
    });
  });

  describe('ALL mode', () => {
    it('passes for AGGREGATE queries', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'ALL',
        queryType: 'AGGREGATE',
      });
      expect(result.ok).toBe(true);
    });

    it('rejects DETAIL queries', () => {
      const result = validateScheduleDateInfo({
        dateMode: 'ALL',
        queryType: 'DETAIL',
      });
      expect(result.ok).toBe(false);
      expect(result.error).toBe('明细调度不支持全量模式，请选择固定区间或最近 N 天');
    });
  });
});
