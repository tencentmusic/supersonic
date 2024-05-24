export enum MetricSettingKey {
  BASIC = 'BASIC',
  SQL_CONFIG = 'SQLCONFIG',
  DIMENSION_CONFIG = 'DIMENSION_CONFIG',
}

export const MetricSettingWording = {
  [MetricSettingKey.BASIC]: '基本信息',
  [MetricSettingKey.SQL_CONFIG]: '表达式',
  [MetricSettingKey.DIMENSION_CONFIG]: '下钻维度配置',
};
