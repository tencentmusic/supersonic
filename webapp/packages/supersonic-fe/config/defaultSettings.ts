import { Settings as LayoutSettings } from '@ant-design/pro-layout';

const Settings: LayoutSettings & {
  pwa?: boolean;
  logo?: string;
} = {
  navTheme: 'light',
  primaryColor: '#296DF3',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  colorWeak: false,
  title: '',
  pwa: false,
  iconfontUrl: '//at.alicdn.com/t/c/font_3201979_drwu4z3kkbi.js',
  splitMenus: true,
  menu: {
    defaultOpenAll: true,
    autoClose: false,
    ignoreFlatMenu: true,
  },
};

export default Settings;
