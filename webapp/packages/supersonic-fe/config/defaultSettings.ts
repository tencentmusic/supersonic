import { ProLayoutProps } from '@ant-design/pro-components';

const Settings: ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
} = {
  navTheme: 'light',
  colorPrimary: '#296DF3',
  layout: 'top',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  colorWeak: false,
  title: '',
  pwa: false,
  iconfontUrl: '//at.alicdn.com/t/c/font_4120566_x5c4www9bqm.js',
  // splitMenus: true,
  // menu: {
  //   autoClose: false,
  //   ignoreFlatMenu: true,
  // },
};
export const publicPath = '/webapp/';
export const basePath = '/webapp/';

export default Settings;
