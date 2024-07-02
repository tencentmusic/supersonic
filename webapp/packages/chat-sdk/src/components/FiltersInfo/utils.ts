import { ChatContextType, DateInfoType, FilterItemType } from '../../common/type';
import { uuid } from '../../utils/utils';
import type {
  IPill,
  IStringFilterCondition,
  IStringColumnCondition,
  ITopNCondition,
  IAggregationPill,
  IDateFilterPill,
  IGroupPill,
  INumberFilterPill,
  ITextFilterPill,
  ITopNPill,
} from './types';

export async function getDropdownHtmlElement(container: HTMLElement): Promise<HTMLElement> {
  const dropdown = container.querySelector('.ant-select-dropdown');
  if (!dropdown) {
    return new Promise<HTMLElement>(resolve => {
      const observer = new MutationObserver(() => {
        const dropdown = container.querySelector('.ant-select-dropdown');
        if (dropdown) {
          observer.disconnect();
          resolve(dropdown as HTMLElement);
        }
      });
      observer.observe(container, { childList: true, subtree: true });
    });
  }
  return Promise.resolve(dropdown as HTMLElement);
}

const numberOperatorMap = {
  '>': {
    getText: (v1: number, v2: number) => '大于' + v1,
    label: '大于',
    typeMeaning: 'least',
    includeMin: false,
    includeMax: false,
  },
  '>=': {
    getText: (v1: number, v2: number) => '大于等于' + v1,
    label: '大于等于',
    typeMeaning: 'least',
    includeMin: true,
    includeMax: false,
  },
  '<': {
    getText: (v1: number, v2: number) => '小于' + v1,
    label: '小于',
    typeMeaning: 'most',
    includeMin: false,
    includeMax: false,
  },
  '<=': {
    getText: (v1: number, v2: number) => '小于等于' + v1,
    label: '小于等于',
    typeMeaning: 'most',
    includeMin: false,
    includeMax: true,
  },
  '=': {
    getText: (v1: number, v2: number) => '等于' + v1,
    label: '等于',
  },
  // gtlt: {
  //   getText: (v1: number, v2: number) => '大于' + v1 + '且小于' + v2,
  //   typeMeaning: 'between',
  //   includeMin: false,
  //   includeMax: false,
  // },
  // gtelte: {
  //   getText: (v1: number, v2: number) => '大于等于' + v1 + '且小于等于' + v2,
  //   typeMeaning: 'between',
  //   includeMin: true,
  //   includeMax: true,
  // },
  // gtlte: {
  //   getText: (v1: number, v2: number) => '大于' + v1 + '且小于等于' + v2,
  //   typeMeaning: 'between',
  //   includeMin: false,
  //   includeMax: true,
  // },
  // gtelt: {
  //   getText: (v1: number, v2: number) => '大于等于' + v1 + '且小于' + v2,
  //   typeMeaning: 'between',
  //   includeMin: true,
  //   includeMax: false,
  // },
};

function getNumberOperatorOptions() {
  return Object.keys(numberOperatorMap).map(key => ({
    value: key,
    label: numberOperatorMap[key].label,
  }));
}

export function getOperatorConfig(operator: string) {
  return numberOperatorMap[operator];
}

/**
 *
 * @param value
 * @param operator 仅支持大于、大于等于、小于、小于等于、大于xx且小于yy、大于等于xx且小于等于yy、大于等于xx且小于yy、大于xx且小于等于yy
 * @returns
 */
export function getNumberFilterShowText(value: number | number[] | null, operator: string) {
  if (!numberOperatorMap[operator]) {
    const _v = Array.isArray(value) ? value : value === null ? [] : [value];
    return operator + _v.join('、');
  }

  const { getText } = numberOperatorMap[operator];

  if (Array.isArray(value)) {
    return getText(value[0] ?? '', value[1] ?? '');
  }

  return getText(value ?? '', '');
}

export function getNumberValue(value: number | number[] | null, operator: string) {
  if (typeof value === 'number') {
    if (operator === '>') return [value, null];
    if (operator === '>=') return [value, null];
    if (operator === '<') return [null, value];
    if (operator === '<=') return [null, value];
    return [null, null];
  }

  if (Array.isArray(value)) {
    return value;
  }

  return [null, null];
}

export function getNumberFilterOperator(
  typeMeaning: string,
  includeMin: boolean,
  includeMax: boolean
) {
  if (typeMeaning === 'between') {
    if (includeMin && includeMax) {
      return 'gtelte';
    } else if (includeMax) {
      return 'gtlte';
    } else if (includeMin) {
      return 'gtelt';
    }
    return 'gtlt';
  } else if (typeMeaning === 'least') {
    return includeMin ? 'gte' : 'gt';
  } else if (typeMeaning === 'most') {
    return includeMax ? 'lte' : 'lt';
  }

  return '';
}

export function getNumberFilterValue(numbers: [number | null, number | null], operator: string) {
  if (operator === 'gt') return numbers[0];
  if (operator === 'gte') return numbers[0];
  if (operator === 'lt') return numbers[1];
  if (operator === 'lte') return numbers[1];
  if (operator === 'gtlt') return numbers;
  if (operator === 'gtelte') return numbers;
  if (operator === 'gtlte') return numbers;
  if (operator === 'gtelt') return numbers;
  return null;
}

const textOperatorMap = {
  IN: {
    getText: (v1: string[] | string) => {
      const _v1 = Array.isArray(v1) ? v1 : [v1];
      return '属于' + _v1.join('、');
    },
    label: '属于',
  },
  NOT_IN: {
    getText: (v1: string[] | string) => {
      const _v1 = Array.isArray(v1) ? v1 : [v1];
      return '不属于' + _v1.join('、');
    },
    label: '不属于',
  },
  IS_NULL: {
    getText: () => '为空',
    label: '为空',
  },
  IS_NOT_MULL: {
    getText: () => '不为空',
    label: '不为空',
  },
  LIKE: {
    getText: (v1: string[] | string) => {
      const _v1 = Array.isArray(v1) ? v1[0] : v1;
      return '包含 ' + _v1;
    },
    label: '包含',
  },
  '=': {
    getText: (v1: string[] | string) => {
      const _v1 = Array.isArray(v1) ? v1[0] : v1;
      return '等于' + _v1;
    },
    label: '等于',
  },
};

function getTextOperatorOptions() {
  return Object.keys(textOperatorMap).map(key => ({
    value: key,
    label: textOperatorMap[key].label,
  }));
}

export function getStringFilterShowText(value: string | string[] | null, operator: string) {
  if (!textOperatorMap[operator]) {
    const _v = Array.isArray(value) ? value : [value];
    return operator + _v.join('、');
  }

  const { getText } = textOperatorMap[operator];

  return getText(value ?? '');
}

const aggregationMap = {
  COUNT: {
    getText: (name: string) => name + '计数',
    label: '计数',
  },
  COUNT_DISTINCT: {
    getText: (name: string) => name + '去重计数',
    label: '去重计数',
  },
  SUM: {
    getText: (name: string) => name + '求和',
    label: '求和',
  },
  AVG: {
    getText: (name: string) => name + '平均值',
    label: '平均值',
  },
  MAX: {
    getText: (name: string) => name + '最大值',
    label: '最大值',
  },
  MIN: {
    getText: (name: string) => name + '最小值',
    label: '最小值',
  },

  PERCENTILE: {
    getText: (name: string) => name + '百分位数',
    label: '百分位数',
  },
  RATIO_ROLL: {
    getText: (name: string) => name + '环比',
    label: '环比',
  },
  RATIO_OVER: {
    getText: (name: string) => name + '同比',
    label: '同比',
  },
};

function getAggregationOptions() {
  return Object.keys(aggregationMap).map(key => ({
    value: key,
    label: aggregationMap[key].label,
  }));
}

export function getOptions(type: 'string' | 'number' | 'aggregation') {
  if (type === 'string') return getTextOperatorOptions();
  if (type === 'number') return getNumberOperatorOptions();
  if (type === 'aggregation') return getAggregationOptions();
  return [];
}

export function getAggregationShowText(name: string, operator: string) {
  if (!aggregationMap[operator]) return name + operator;

  const { getText } = aggregationMap[operator];

  return getText(name);
}

export function getDateFilterShowText(value: [string | null, string | null] | null) {
  return `${value?.[0]} ~ ${value?.[1]}`;
}

export function getPillTitle(data: IPill) {
  switch (data.type) {
    case 'date-filter':
      return '时间：' + getDateFilterShowText(data.value);
    case 'text-filter':
      return data.fieldName + getStringFilterShowText(data.value, data.operator);
    case 'number-filter':
      return data.fieldName + getNumberFilterShowText(data.value, data.operator);
    case 'group':
      return `分组：${data.fields.map(field => field.fieldName ?? field.field).join('、 ')}`;
    case 'aggregation':
      return `计算：${data.fields
        .map(field => getAggregationShowText(field.fieldName, field.operator))
        .join('、 ')}`;
    case 'top-n':
      return `前${data.value}条`;
  }
}

type FilterPill = ITextFilterPill | IDateFilterPill | INumberFilterPill;

type Filters = {
  type: 'filters';
  data: FilterPill[];
};

type Groups = {
  type: 'groups';
  data: IGroupPill;
};

type Aggregations = {
  type: 'aggregations';
  data: IAggregationPill;
};

type TopNs = {
  type: 'topNs';
  data: ITopNPill;
};

export function getPillExplainTitle(data: Filters | Groups | Aggregations | TopNs) {
  switch (data.type) {
    case 'filters':
      return (
        '按照' +
        data.data
          .map(filter => {
            switch (filter.type) {
              case 'date-filter':
                return '时间为' + getDateFilterShowText(filter.value);
              case 'text-filter':
                return filter.fieldName + getStringFilterShowText(filter.value, filter.operator);
              case 'number-filter':
                return filter.fieldName + getNumberFilterShowText(filter.value, filter.operator);
              default:
                return '';
            }
          })
          .join('、') +
        '过滤'
      );

    case 'groups':
      return (
        '按照' + data.data.fields.map(field => field.fieldName ?? field.field).join('、 ') + '分组'
      );
    case 'aggregations':
      return `计算${data.data.fields
        .map(field => getAggregationShowText(field.fieldName, field.operator))
        .join('、 ')}`;
    case 'topNs':
      return `返回前${data.data.value}条数据`;
  }
}

// 按照类型分组Pill
export function groupPillsByType(pills: IPill[]) {
  let filters: FilterPill[] = [];
  let groups: IGroupPill | null = null;
  let aggregations: IAggregationPill | null = null;
  let topNs: ITopNPill | null = null;

  pills.forEach(pill => {
    switch (pill.type) {
      case 'text-filter':
      case 'number-filter':
      case 'date-filter':
        filters.push(pill);
        break;
      case 'group':
        groups = pill;
        break;
      case 'aggregation':
        aggregations = pill;
        break;
      case 'top-n':
        topNs = pill;
        break;
    }
  });

  return {
    filters,
    groups,
    aggregations,
    topNs,
  };
}

export function translate2ExplainText(mode: 'detail' | 'metric', data: IPill[]): string {
  let orderIndicators: string[] = [];
  const { filters, groups, aggregations, topNs } = groupPillsByType(data);
  let count = 0;
  const finnalPillsArray: (Filters | Groups | Aggregations | TopNs)[] = [];
  if (filters.length) {
    finnalPillsArray.push({
      type: 'filters',
      data: filters,
    });
    count++;
  }

  if (groups) {
    finnalPillsArray.push({
      type: 'groups',
      data: groups,
    });
    count++;
  }

  if (aggregations) {
    finnalPillsArray.push({
      type: 'aggregations',
      data: aggregations,
    });
    count++;
  }

  if (topNs) {
    finnalPillsArray.push({
      type: 'topNs',
      data: topNs,
    });
    count++;
  }

  if (count === 2) {
    orderIndicators = ['先', '再'];
  } else if (count > 2) {
    orderIndicators = ['先', '再', '然后', '接着', '最后'].slice(0, count - 1).concat(['最后']);
  }

  return finnalPillsArray
    .map((item, index) => (orderIndicators[index] ?? '') + getPillExplainTitle(item))
    .join('，');
}

export function getPillTitleForTooltip(data: IPill) {
  let tip = ['text-filter', 'date-filter'].includes(data.type) ? '列表过滤：' : '';
  return tip + getPillTitle(data);
}

export function createFilterConditionData(): IStringFilterCondition {
  return {
    uuid: uuid(),
    type: 'filter',
    fieldType: 'string',
    value: '',
  };
}

export function createColumnConditionData(): IStringColumnCondition {
  return {
    uuid: uuid(),
    type: 'column',
    fieldType: 'string',
    value: '',
  };
}

export function createTopNConditionData(): ITopNCondition {
  return {
    uuid: uuid(),
    type: 'topN',
    value: null,
  };
}

function createTextFilterPill(filterItem: FilterItemType): ITextFilterPill {
  return {
    type: 'text-filter',
    field: filterItem.bizName,
    fieldName: filterItem.name,
    value: filterItem.value,
    operator: filterItem.operator!,
    id: filterItem.bizName,
    fieldId: filterItem.elementID,
  };
}

function createNumberFilterPill(filterItem: FilterItemType): INumberFilterPill {
  return {
    type: 'number-filter',
    field: filterItem.bizName,
    fieldName: filterItem.name,
    value: filterItem.value,
    operator: filterItem.operator!,
    id: filterItem.bizName,
  };
}

function createDateFilterPill(dateInfo: DateInfoType): IDateFilterPill {
  return {
    type: 'date-filter',
    field: 'date',
    fieldName: '日期',
    value: [dateInfo.startDate, dateInfo.endDate],
    id: 'date',
  };
}

function createTopNPill(limit: number): ITopNPill {
  return {
    type: 'top-n',
    value: limit,
    id: 'topN',
  };
}

function createGroupPill(dimensions: ChatContextType['dimensions']): IGroupPill {
  return {
    type: 'group',
    fields: dimensions.map(dimension => ({
      field: dimension.bizName,
      fieldName: dimension.name,
    })),
    id: 'group',
  };
}

function createAggregationPill(metrics: ChatContextType['metrics']): IAggregationPill {
  return {
    type: 'aggregation',
    fields: metrics.map(metric => ({
      field: metric.bizName,
      fieldName: metric.name,
      operator: metric.aggregator ?? metric.defaultAgg,
    })),
    id: 'aggregation',
  };
}

export function getPillsByParseInfo(
  parseInfo: ChatContextType | null,
  getTypeByBizName: (bizName: string) => 'string' | 'number' | 'date'
): IPill[] {
  if (!parseInfo || !parseInfo.id) return [];
  const { dimensionFilters = [], dimensions = [], metrics = [], dateInfo, limit } = parseInfo;

  const filterPills: IPill[] = [];
  // dimensionFilters 根绝值类型来判断是 text-filter 还是 number-filter，分别转换成多个 filter
  dimensionFilters.forEach(filter => {
    const bizNameType = getTypeByBizName(filter.bizName);
    if (bizNameType === 'string') {
      filterPills.push(createTextFilterPill(filter));
    } else if (bizNameType === 'number') {
      filterPills.push(createNumberFilterPill(filter));
    }
  });
  // dateInfo 转化为一个 date-filter
  dateInfo && filterPills.push(createDateFilterPill(dateInfo));
  // dimensions 转化为一个 group filter
  dimensions.length && filterPills.push(createGroupPill(dimensions));
  // metrics 转化为一个 aggregation filter
  metrics.length && filterPills.push(createAggregationPill(metrics));
  // limit 转化为一个 topN filter
  typeof limit === 'number' && filterPills.push(createTopNPill(limit));

  return filterPills;
}
