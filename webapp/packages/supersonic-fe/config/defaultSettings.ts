import { ProLayoutProps } from '@ant-design/pro-components';

/** 全站品牌主色唯一来源（Less、`configProviderTheme`、Logo 等均应对齐此值） */
export const BRAND_PRIMARY = '#296DF3';

export type DefaultSetting = ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
};
const Settings: DefaultSetting = {
  navTheme: 'light',
  colorPrimary: BRAND_PRIMARY,
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  colorWeak: false,
  title: '',
  pwa: false,
  iconfontUrl: '//at.alicdn.com/t/c/font_4120566_x5c4www9bqm.js',
  splitMenus: false,
};
export const publicPath = '/webapp/';
export const basePath = '/webapp/';

export default Settings;
