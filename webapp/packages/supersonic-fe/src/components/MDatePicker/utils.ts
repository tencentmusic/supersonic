import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { DateMode, DateRangeType, DateRangePicker, DateRangeTypeToPickerMap } from './type';
import {
  DatePeriodType,
  PerDatePeriodType,
  DateSettingType,
  DynamicAdvancedConfigType,
  DateRangeParams,
  shortCutIdType,
} from './type';
import { LatestDateMap } from './type';

export const dateRangeTypeExchangeDatePeriodTypeMap = {
  [DateRangeType.DAY]: DatePeriodType.DAY,
  [DateRangeType.WEEK]: DatePeriodType.WEEK,
  [DateRangeType.MONTH]: DatePeriodType.MONTH,
};

export const datePeriodTypeMap = {
  [DatePeriodType.DAY]: '天',
  [DatePeriodType.WEEK]: '周',
  [DatePeriodType.MONTH]: '月',
  [DatePeriodType.YEAR]: '年',
};
export const datePeriodTypeWordingMap = {
  [DatePeriodType.DAY]: '当天',
  [DatePeriodType.WEEK]: '本周',
  [DatePeriodType.MONTH]: '本月',
  [DatePeriodType.YEAR]: '今年',
};

export const perDatePeriodTypeMap = {
  [PerDatePeriodType.PERDAY]: '每日',
  [PerDatePeriodType.PERWEEK]: '每周(周一)',
  [PerDatePeriodType.PERMONTH]: '每月(01日)',
  [PerDatePeriodType.PERYEAR]: '每年(01月01日)',
};

export const DATE_RANGE_TYPE_ITEM_LIST = [
  {
    label: '天',
    value: DateRangeType.DAY,
    toolTips: '展示每天数据',
  },
  {
    label: '周',
    value: DateRangeType.WEEK,
    toolTips: '仅展示每周日数据',
  },
  {
    label: '月',
    value: DateRangeType.MONTH,
    toolTips: '仅展示每月最后一天数据',
  },
];

export const LATEST_TEXT = '最近1天';

export const getDatePickerDynamicInitialValues = (number: number, dateRangeType: DateRangeType) => {
  return {
    dateSettingType: 'DYNAMIC',
    dynamicParams: {
      includesCurrentPeriod: true,
      dateRangeType: dateRangeType,
      dynamicAdvancedConfigType: 'last',
      dateSettingType: DateSettingType.DYNAMIC,
      shortCutId: `last${number}${shortCutIdType[dateRangeType]}`,
      number,
      periodType: dateRangeType,
    },
  };
};

export const SHORT_CUT_ITEM_LIST = [
  {
    id: 'last7Days',
    text: '最近7天',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.DAY,
    initData: {
      shortCutId: 'last7Days',
      number: 7,
      periodType: DatePeriodType.DAY,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last15Days',
    text: '最近15天',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.DAY,
    initData: {
      shortCutId: 'last15Days',
      number: 15,
      periodType: DatePeriodType.DAY,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last30Days',
    text: '最近30天',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.DAY,
    initData: {
      shortCutId: 'last30Days',
      number: 30,
      periodType: DatePeriodType.DAY,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last4Weeks',
    text: '最近4周',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.WEEK,
    initData: {
      shortCutId: 'last4Weeks',
      number: 4,
      periodType: DatePeriodType.WEEK,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last8Weeks',
    text: '最近8周',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.WEEK,
    initData: {
      shortCutId: 'last8Weeks',
      number: 8,
      periodType: DatePeriodType.WEEK,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last12Weeks',
    text: '最近12周',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.WEEK,
    initData: {
      shortCutId: 'last12Weeks',
      number: 12,
      periodType: DatePeriodType.WEEK,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last6Months',
    text: '最近6个月',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.MONTH,
    initData: {
      shortCutId: 'last6Months',
      number: 6,
      periodType: DatePeriodType.MONTH,
      includesCurrentPeriod: false,
    },
  },
  {
    id: 'last12Months',
    text: '最近12个月',
    advancedConfigType: DynamicAdvancedConfigType.LAST,
    dateRangeType: DateRangeType.MONTH,
    initData: {
      shortCutId: 'last12Months',
      number: 12,
      periodType: DatePeriodType.MONTH,
      includesCurrentPeriod: false,
    },
  },
];

const computedSubtractDateRange = (number: number, includesCurrentPeriod: boolean) => {
  const startDateNumber = includesCurrentPeriod ? number - 1 : number;
  const endDateNumber = includesCurrentPeriod ? 0 : 1;
  return { startDateNumber, endDateNumber };
};

const getLastTypeDateRange = ({
  number,
  periodType,
  latestDateMap,
  includesCurrentPeriod = false, // 是否包含当前周期 今天/本周/本月/今年
}: {
  number: number;
  periodType: DatePeriodType;
  latestDateMap: LatestDateMap;
  includesCurrentPeriod?: boolean;
}) => {
  const { startDateNumber, endDateNumber } = computedSubtractDateRange(
    number,
    includesCurrentPeriod,
  );

  switch (periodType) {
    case DatePeriodType.DAY: {
      // 如果选取了包含今天，则放弃使用标签最新更新时间；
      const periodTypeMoment = includesCurrentPeriod
        ? undefined
        : latestDateMap?.maxPartition || undefined;
      let subtractData = {
        startDateNumber,
        endDateNumber,
      };
      if (periodTypeMoment) {
        // 在使用标签最新更新时间时，需包含最新的时间日期，并重置计算时间区段
        subtractData = computedSubtractDateRange(number, true);
      }
      return [
        dayjs(periodTypeMoment).subtract(subtractData.startDateNumber, 'days').startOf('days'),
        dayjs(periodTypeMoment).subtract(subtractData.endDateNumber, 'days').endOf('days'),
      ];
    }
    case DatePeriodType.WEEK: {
      // 如果选取了包含今天，则放弃使用标签最新更新时间；
      const periodTypeMoment = includesCurrentPeriod
        ? undefined
        : latestDateMap?.maxPartition || undefined;
      let subtractData = {
        startDateNumber,
        endDateNumber,
      };
      if (periodTypeMoment) {
        // 在使用标签最新更新时间时，需包含最新的时间日期，并重置计算时间区段
        subtractData = computedSubtractDateRange(number, true);
      }
      return [
        dayjs(periodTypeMoment).subtract(subtractData.startDateNumber, 'week').startOf('week'),
        dayjs(periodTypeMoment).subtract(subtractData.endDateNumber, 'week').endOf('week'),
      ];
    }
    case DatePeriodType.MONTH: {
      // 如果选取了包含今天，则放弃使用标签最新更新时间；
      const periodTypeMoment = includesCurrentPeriod
        ? undefined
        : latestDateMap?.maxPartition || undefined;
      let subtractData = {
        startDateNumber,
        endDateNumber,
      };
      if (periodTypeMoment) {
        // 在使用标签最新更新时间时，需包含最新的时间日期，并重置计算时间区段
        subtractData = computedSubtractDateRange(number, true);
      }
      return [
        dayjs(periodTypeMoment).subtract(subtractData.startDateNumber, 'month').startOf('month'),
        dayjs(periodTypeMoment).subtract(subtractData.endDateNumber, 'month').endOf('month'),
      ];
    }
    case DatePeriodType.YEAR:
      return [
        dayjs().subtract(startDateNumber, 'year').startOf('year'),
        dayjs().subtract(endDateNumber, 'year').endOf('year'),
      ];
    default:
      return [];
  }
};

const getHistoryTypeDateRange = ({
  number,
  periodType,
}: {
  number: number;
  periodType: DatePeriodType;
}) => {
  const dateNumber = number;
  switch (periodType) {
    case DatePeriodType.DAY:
      return [
        dayjs().subtract(dateNumber, 'days').startOf('days'),
        dayjs().subtract(dateNumber, 'days').endOf('days'),
      ];
    case DatePeriodType.WEEK:
      return [
        dayjs().subtract(dateNumber, 'week').startOf('week'),
        dayjs().subtract(dateNumber, 'week').endOf('week'),
      ];
    case DatePeriodType.MONTH:
      return [
        dayjs().subtract(dateNumber, 'month').startOf('month'),
        dayjs().subtract(dateNumber, 'month').endOf('month'),
      ];
    case DatePeriodType.YEAR:
      return [
        dayjs().subtract(dateNumber, 'year').startOf('year'),
        dayjs().subtract(dateNumber, 'year').endOf('year'),
      ];
    default:
      return [];
  }
};
const getFromDatePeriodTypeDateRange = ({
  perPeriodType,
}: {
  perPeriodType: PerDatePeriodType;
}) => {
  switch (perPeriodType) {
    case PerDatePeriodType.PERDAY:
      return [dayjs().startOf('days'), dayjs()];
    case PerDatePeriodType.PERWEEK:
      return [dayjs().startOf('week'), dayjs()];
    case PerDatePeriodType.PERMONTH:
      return [dayjs().startOf('month'), dayjs()];
    case PerDatePeriodType.PERYEAR:
      return [dayjs().startOf('year'), dayjs()];
    default:
      return [];
  }
};
const getFromDateTypeDateRange = ({ date }: { date: string }) => {
  return [dayjs(date), dayjs()];
};

export const getLastestTypeDateRange = (latestDate: string) => {
  if (latestDate) {
    return [dayjs(latestDate), dayjs(latestDate)];
  }
  console.warn('最新标签更新日期不存在');
  return [dayjs().subtract(1, 'week'), dayjs().subtract(1, 'week')];
};

export const shortCutDateRangeMap = {
  // latest: (latestDate?: string) => {
  //   if (latestDate) {
  //     return [dayjs(latestDate), dayjs(latestDate)];
  //   }
  //   return [dayjs(), dayjs()];
  // },
  // yesterday: () => {
  //   return [dayjs().subtract(1, 'days'), dayjs().subtract(1, 'days')];
  // },
  // last3Days: () => {
  //   return [dayjs().subtract(3, 'days'), dayjs()];
  // },
  // last7Days: () => {
  //   return [dayjs().subtract(7, 'days'), dayjs()];
  // },
  // last30Days: () => {
  //   return [dayjs().subtract(30, 'days'), dayjs()];
  // },
  // today: () => {
  //   return [dayjs(), dayjs()];
  // },
  // thisWeek: () => {
  //   return [dayjs().startOf('week'), dayjs().endOf('week')];
  // },
  // thisMonth: () => {
  //   return [dayjs().startOf('month'), dayjs().endOf('month')];
  // },
  // thisYear: () => {
  //   return [dayjs().startOf('year'), dayjs().endOf('year')];
  // },
  [DynamicAdvancedConfigType.LATEST]: getLastestTypeDateRange,
  [DynamicAdvancedConfigType.LAST]: getLastTypeDateRange,
  [DynamicAdvancedConfigType.HISTORY]: getHistoryTypeDateRange,
  [DynamicAdvancedConfigType.FROM_DATE_PERIOD]: getFromDatePeriodTypeDateRange,
  [DynamicAdvancedConfigType.FROM_DATE]: getFromDateTypeDateRange,
};

export const formatDateRangeString = (dateRange: Dayjs[]) => {
  if (dateRange && Array.isArray(dateRange)) {
    return dateRange.map((item) => {
      return item?.format?.('YYYY-MM-DD') || '';
    });
  }
  return dateRange;
};

// 动态时间配置转换为静态时间参数进行请求
export const parseDynamicDateParamsToStaticDateParams = (
  dateParams: any,
  latestDateMap: LatestDateMap,
) => {
  const {
    dynamicAdvancedConfigType,
    number,
    periodType,
    includesCurrentPeriod,
    perPeriodType,
    date,
    dateRangeType,
  } = dateParams;
  const staticParams = {
    dateMode: DateMode.RANGE,
    dateRangeType: dateRangeType || DateRangeType.DAY,
  };
  let dateMomentRange: Dayjs[] = [];
  switch (dynamicAdvancedConfigType) {
    case DynamicAdvancedConfigType.LATEST: {
      const latestDate = latestDateMap?.maxPartition;
      dateMomentRange = getLastestTypeDateRange(latestDate);
      break;
    }
    case DynamicAdvancedConfigType.LAST: {
      dateMomentRange = getLastTypeDateRange({
        number,
        periodType,
        latestDateMap,
        includesCurrentPeriod,
      });
      break;
    }
    case DynamicAdvancedConfigType.HISTORY: {
      dateMomentRange = getHistoryTypeDateRange({ number, periodType });
      break;
    }
    case DynamicAdvancedConfigType.FROM_DATE_PERIOD:
      dateMomentRange = getFromDatePeriodTypeDateRange({ perPeriodType });
      break;
    case DynamicAdvancedConfigType.FROM_DATE:
      dateMomentRange = getFromDateTypeDateRange({ date });
      break;
    default:
      break;
  }
  return {
    ...staticParams,
    dateRange: formatDateRangeString(dateMomentRange),
  };
};

export const generatorDateRangesParams = (dateRangeParams: DateRangeParams) => {
  const {
    dateSettingType,
    latestDateMap = {},
    dynamicParams = {},
    staticParams = {},
  } = dateRangeParams;
  if (dateSettingType === DateSettingType.DYNAMIC) {
    return parseDynamicDateParamsToStaticDateParams(dynamicParams, latestDateMap as any);
  }
  return staticParams;
};

export const getDynamicDateRangeStringByParams = (
  params: any,
  type: DynamicAdvancedConfigType,
  latestDateMap: LatestDateMap,
) => {
  const { number, periodType, includesCurrentPeriod, perPeriodType, date } = params;
  let dateRangeMoment: any[] = [];
  let dateRangeStringDesc = '';
  switch (type) {
    case DynamicAdvancedConfigType.LATEST: {
      const latestDate = latestDateMap.maxPartition;
      dateRangeStringDesc = LATEST_TEXT;
      dateRangeMoment = shortCutDateRangeMap[DynamicAdvancedConfigType.LATEST](latestDate);
      break;
    }

    case DynamicAdvancedConfigType.LAST:
      dateRangeMoment = shortCutDateRangeMap[DynamicAdvancedConfigType.LAST]({
        number,
        periodType,
        latestDateMap,
        includesCurrentPeriod,
      });
      dateRangeStringDesc =
        `最近${number}${datePeriodTypeMap[periodType]}` +
        `${includesCurrentPeriod ? `(包含${datePeriodTypeWordingMap[periodType]})` : ''}`;
      break;
    case DynamicAdvancedConfigType.HISTORY:
      dateRangeMoment = shortCutDateRangeMap[DynamicAdvancedConfigType.HISTORY]({
        number,
        periodType,
      });
      dateRangeStringDesc = `过去第${number}${datePeriodTypeMap[periodType]}`;
      break;
    case DynamicAdvancedConfigType.FROM_DATE_PERIOD:
      dateRangeMoment = shortCutDateRangeMap[DynamicAdvancedConfigType.FROM_DATE_PERIOD]({
        perPeriodType,
      });
      dateRangeStringDesc = `自从${perDatePeriodTypeMap[perPeriodType]}00:00:00至此刻`;
      break;
    case DynamicAdvancedConfigType.FROM_DATE:
      dateRangeMoment = shortCutDateRangeMap[DynamicAdvancedConfigType.FROM_DATE]({
        date,
      });
      dateRangeStringDesc = `${date}至此刻`;
      break;
    default:
      dateRangeMoment = [];
      dateRangeStringDesc = '';
  }
  return {
    dateRangeString: formatDateRangeString(dateRangeMoment),
    dateRangeStringDesc,
  };
};

export function getDateStrings({
  dates,
  dateRangeType,
  latestDateMap,
  isDateRangeChange,
}: {
  dates: any;
  dateRangeType: DateRangeType;
  latestDateMap?: LatestDateMap;
  isDateRangeChange?: boolean;
}) {
  let dateRange = dates;
  if (!Array.isArray(dateRange)) {
    dateRange = [dateRange];
  }
  const picker = DateRangeTypeToPickerMap[dateRangeType];
  const dateStrings = dateRange.map((date: Dayjs, index: number) => {
    switch (picker) {
      case DateRangePicker.DATE:
        if (latestDateMap?.maxPartition && !isDateRangeChange) {
          return latestDateMap.maxPartition;
        }
        return date.format('YYYY-MM-DD');
      case DateRangePicker.WEEK:
        if (index === 0) {
          // 仅当dateRangeType进行切换时，即天/周/月被转换时对时间进行当前时间周期-1操作
          return date
            .startOf('week')
            .subtract(!isDateRangeChange ? 1 : 0, 'week')
            .format('YYYY-MM-DD');
        }
        return date
          .endOf('week')
          .subtract(!isDateRangeChange ? 1 : 0, 'week')
          .format('YYYY-MM-DD');
      case DateRangePicker.MONTH:
        if (index === 0) {
          return date
            .startOf('month')
            .subtract(!isDateRangeChange ? 1 : 0, 'month')
            .format('YYYY-MM-DD');
        }
        return date
          .endOf('month')
          .subtract(!isDateRangeChange ? 1 : 0, 'month')
          .format('YYYY-MM-DD');
      default:
        if (latestDateMap?.maxPartition && !isDateRangeChange) {
          return latestDateMap?.maxPartition;
        }
        return date.format('YYYY-MM-DD');
    }
  });
  return dateStrings;
}

export const getWeekDateRangeString = (startTime: string, endTime: string) => {
  const startTimeWeekNumber = dayjs(startTime).format('w');
  const endTimeWeekNumber = dayjs(endTime).format('w');
  return `${startTime}(${startTimeWeekNumber}周)至${endTime}(${endTimeWeekNumber}周)`;
};

export const getMonthDateRangeString = (startTime: string, endTime: string) => {
  const startTimeMonth = dayjs(startTime).format('YYYY-MM');
  const endTimeMonth = dayjs(endTime).format('YYYY-MM');
  return `${startTimeMonth}至${endTimeMonth}`;
};
