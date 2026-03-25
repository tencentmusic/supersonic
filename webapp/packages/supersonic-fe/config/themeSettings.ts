import type { ThemeConfig } from 'antd';
import { BRAND_PRIMARY } from './defaultSettings';

const constants = {
  black85: 'rgba(0,10,36,0.85)',
  black65: 'rgba(0,10,36,0.65)',
  black45: 'rgba(0,10,36,0.45)',
  black25: 'rgba(0,10,36,0.25)',
};

const settings = {
  // Colors（与 BRAND_PRIMARY 保持一致）
  'blue-6': BRAND_PRIMARY,
  'primary-color': BRAND_PRIMARY,
  'green-6': '#26C992',
  'success-color': '#26C992',
  'red-5': '#EF4872',
  'error-color': '#EF4872',
  'gold-6': '#FFB924',
  'warning-color': '#FFB924',

  'primary-1': '#E3ECFD',
  'primary-2': '#BED2FB',
  'primary-3': '#86ACF8',
  'primary-4': '#6193F6',
  'primary-5': '#4E86F5',
  'primary-6': BRAND_PRIMARY,
  'primary-7': '#0D57E8',
  'primary-8': '#0B49C3',
  'primary-9': '#093B9D',
  'primary-10': '#062666',

  'heading-color': constants.black85,
  'text-color': constants.black85,
  'text-color-secondary': constants.black65,
  'border-radius-base': '4px',

  'btn-padding-horizontal-sm': '8px',
  'btn-padding-horizontal-base': '16px',
  'btn-padding-horizontal-lg': '16px',
  'btn-default-color': constants.black65,
  'btn-default-border': 'rgba(0,0,0,0.15)',
  'btn-disable-color': constants.black25,
  'btn-disable-border': 'rgba(0,10,36,0.15)',
  'btn-disable-bg': 'rgba(0,10,36,0.04)',
};

/**
 * Ant Design 5 ConfigProvider 主题（与 docs/product/ui-commercial-saas-landing-plan.md 阶段一对齐）
 * Metabase 式：浅灰工作区底、白卡片、统一主色、表格表头轻对比。
 */
export const configProviderTheme: ThemeConfig = {
  token: {
    colorPrimary: BRAND_PRIMARY,
    borderRadius: 4,
    colorBgLayout: '#f5f7fa',
    colorBgContainer: '#ffffff',
    colorText: constants.black85,
    colorTextSecondary: constants.black65,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif",
    fontFamilyCode:
      "ui-monospace, 'SFMono-Regular', Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace",
  },
  components: {
    Layout: {
      bodyBg: '#f5f7fa',
    },
    Table: {
      headerBg: '#f2f4f7',
      headerColor: constants.black65,
      headerSplitColor: 'transparent',
    },
    Checkbox: {
      colorPrimary: BRAND_PRIMARY,
      borderRadiusSM: 4,
      checkboxSize: 18,
    },
    Radio: {
      colorPrimary: BRAND_PRIMARY,
    },
    Button: {
      colorPrimary: BRAND_PRIMARY,
    },
  },
};

export default settings;
