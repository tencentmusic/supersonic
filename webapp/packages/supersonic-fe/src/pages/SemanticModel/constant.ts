export const SENSITIVE_LEVEL_OPTIONS = [
  {
    label: '低',
    value: 0,
  },
  {
    label: '中',
    value: 1,
  },
  {
    label: '高',
    value: 2,
  },
];

export const SENSITIVE_LEVEL_ENUM = SENSITIVE_LEVEL_OPTIONS.reduce(
  (sensitiveEnum: any, item: any) => {
    const { label, value } = item;
    sensitiveEnum[value] = label;
    return sensitiveEnum;
  },
  {},
);
