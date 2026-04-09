import dayjs from 'dayjs';
import type { DataSetSchemaField } from '@/services/reportSchedule';
import type {
  QueryAggregatorFormItem,
  QueryDimensionFilterFormItem,
  QueryType,
} from '@/components/QueryConfigFormSection';

export const parseQueryConfig = (queryConfig?: string) => {
  if (!queryConfig) {
    return {};
  }
  try {
    return JSON.parse(queryConfig);
  } catch {
    return {};
  }
};

export const parseAggregators = (queryConfig?: string): QueryAggregatorFormItem[] => {
  const parsed: any = parseQueryConfig(queryConfig);
  if (!Array.isArray(parsed?.aggregators)) {
    return [];
  }
  return parsed.aggregators
    .map((item: any) => ({
      column: item?.column,
      func: item?.func || 'SUM',
    }))
    .filter((item: QueryAggregatorFormItem) => item.column);
};

export const parseDimensionFilters = (queryConfig?: string): QueryDimensionFilterFormItem[] => {
  const parsed: any = parseQueryConfig(queryConfig);
  if (!Array.isArray(parsed?.dimensionFilters)) {
    return [];
  }
  return parsed.dimensionFilters
    .map((item: any) => ({
      bizName: item?.bizName || item?.name,
      operator: item?.operator || 'EQUALS',
      value: Array.isArray(item?.value) ? item.value.join(', ') : item?.value,
    }))
    .filter((item: QueryDimensionFilterFormItem) => item.bizName || item.value);
};

export const parseQueryType = (
  queryConfig?: string,
  defaultType: QueryType = 'DETAIL',
): QueryType => {
  const parsed: any = parseQueryConfig(queryConfig);
  return parsed?.queryType === 'AGGREGATE' || parsed?.queryType === 'DETAIL'
    ? parsed.queryType
    : defaultType;
};

export const parseQueryGroups = (queryConfig?: string): string[] => {
  const parsed: any = parseQueryConfig(queryConfig);
  return Array.isArray(parsed?.groups) ? parsed.groups : [];
};

export const parseQueryLimit = (queryConfig: string | undefined, fallback: number): number => {
  const parsed: any = parseQueryConfig(queryConfig);
  return typeof parsed?.limit === 'number' && parsed.limit > 0 ? parsed.limit : fallback;
};

export const parseDateInfoFromQueryConfig = (queryConfig?: string) => {
  const parsed: any = parseQueryConfig(queryConfig);
  const dateInfo = parsed?.dateInfo;
  if (!dateInfo) return {};

  const result: Record<string, any> = {
    dateField: dateInfo.dateField,
  };
  if (dateInfo.dateMode === 'RECENT') {
    result.dateMode = 'RECENT';
    result.recentUnit = dateInfo.unit ?? 7;
    result.recentPeriod = dateInfo.period ?? 'DAY';
  } else if (dateInfo.dateMode === 'ALL') {
    result.dateMode = 'ALL';
  } else {
    result.dateMode = 'BETWEEN';
    if (dateInfo.startDate && dateInfo.endDate) {
      result.dateRange = [dayjs(dateInfo.startDate), dayjs(dateInfo.endDate)];
    }
  }
  return result;
};

export const stripDateInfoFromQueryConfig = (queryConfig?: string) => {
  const parsed: any = parseQueryConfig(queryConfig);
  const { dateInfo, ...rest } = parsed;
  return rest;
};

export const buildQueryDimensions = (
  baseConfig: any,
  currentDimensions: DataSetSchemaField[],
): Array<{ id: number; name: string; bizName: string }> => {
  if (Array.isArray(baseConfig?.dimensions) && baseConfig.dimensions.length > 0) {
    return baseConfig.dimensions;
  }
  return currentDimensions.map((field) => ({
    id: field.id,
    name: field.name,
    bizName: field.bizName,
  }));
};

export const buildQueryGroups = ({
  queryGroups,
  baseConfig,
  queryType,
  currentDimensions,
}: {
  queryGroups?: string[];
  baseConfig: any;
  queryType: QueryType;
  currentDimensions: DataSetSchemaField[];
}) => {
  if (Array.isArray(queryGroups) && queryGroups.length > 0) {
    return queryGroups;
  }
  if (Array.isArray(baseConfig?.groups) && baseConfig.groups.length > 0) {
    return baseConfig.groups;
  }
  if (queryType === 'AGGREGATE') {
    return [];
  }
  return currentDimensions.map((item) => item.bizName);
};

export const buildQueryAggregators = (queryAggregators?: QueryAggregatorFormItem[]) => {
  if (!Array.isArray(queryAggregators)) {
    return [];
  }
  return queryAggregators
    .filter((item) => item?.column)
    .map((item) => ({
      column: item.column,
      func: item.func || 'SUM',
    }));
};

export const buildQueryDimensionFilters = (
  queryDimensionFilters: QueryDimensionFilterFormItem[] | undefined,
  dimensions: Array<{ id?: number; name: string; bizName: string }>,
) => {
  if (!Array.isArray(queryDimensionFilters)) {
    return [];
  }
  return queryDimensionFilters
    .filter((item) => {
      if (!item?.bizName) return false;
      if (['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'EQUALS')) return true;
      return !!item?.value?.trim();
    })
    .map((item) => {
      const matchedDimension = dimensions.find((dimension) => dimension?.bizName === item.bizName);
      const isMultiValue = ['IN', 'NOT_IN'].includes(item.operator || 'EQUALS');
      const isNoValueOperator = ['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'EQUALS');
      const rawValue = item.value?.trim() || '';
      return {
        bizName: item.bizName,
        name: matchedDimension?.name || item.bizName,
        operator: item.operator || 'EQUALS',
        value: isNoValueOperator
          ? null
          : isMultiValue
            ? rawValue
                .split(',')
                .map((part: string) => part.trim())
                .filter(Boolean)
            : rawValue,
      };
    });
};
