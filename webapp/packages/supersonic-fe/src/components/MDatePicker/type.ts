export enum DynamicAdvancedConfigType {
  LATEST = 'latest',
  // CUSTOM = 'custom',
  LAST = 'last',
  HISTORY = 'history',
  FROM_DATE_PERIOD = 'fromDatePeriod',
  FROM_DATE = 'fromDate',
}

export enum DatePeriodType {
  DAY = 'DAYS',
  WEEK = 'WEEK',
  MONTH = 'MONTH',
  YEAR = 'YEAR',
}

export enum DateSettingType {
  STATIC = 'STATIC',
  DYNAMIC = 'DYNAMIC',
}

export enum PerDatePeriodType {
  PERDAY = 'PERDAY',
  PERWEEK = 'PERWEEK',
  PERMONTH = 'PERMONTH',
  PERYEAR = 'PERYEAR',
}

export type DateRangeParams = {
  dateSettingType: DateSettingType;
  dynamicParams: any;
  staticParams: any;
  latestDateMap?: { maxPartition: string };
};

export enum StaticDateSelectMode {
  DATE_RANGE = 'dateRange',
  DATE_MODE = 'dateMode',
}

export enum DateRangeType {
  DAY = 'DAY',
  WEEK = 'WEEK',
  MONTH = 'MONTH',
}

export enum DateRangePicker {
  DATE = 'date',
  WEEK = 'week',
  MONTH = 'month',
}

export const DateRangeTypeToPickerMap = {
  [DateRangeType.DAY]: DateRangePicker.DATE,
  [DateRangeType.WEEK]: DateRangePicker.WEEK,
  [DateRangeType.MONTH]: DateRangePicker.MONTH,
};

export type LatestDateMap = {
  maxPartition: string;
};

export enum DateMode {
  RANGE = 1,
  LIST = 2,
  ES = 4,
}
