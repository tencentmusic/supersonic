// https://umijs.org/config/
import { defineConfig } from 'umi';
import defaultSettings, { publicPath } from './defaultSettings';
import themeSettings from './themeSettings';
import proxy from './proxy';
import routes from './routes';
import moment from 'moment';
import ENV_CONFIG from './envConfig';

const { REACT_APP_ENV, RUN_TYPE } = process.env;

export default defineConfig({
  webpack5: {},
  mfsu: false,
  define: {
    // 添加这个自定义的环境变量
    // 'process.env.REACT_APP_ENV': process.env.REACT_APP_ENV, // * REACT_APP_ENV 本地开发环境：dev，测试服：test，正式服：prod
    'process.env': {
      ...process.env,
      API_BASE_URL: '/api/semantic/', // 直接在define中挂载裸露的全局变量还需要配置eslint，ts相关配置才能导致在使用中不会飘红，冗余较高，这里挂在进程环境下
      CHAT_API_BASE_URL: '/api/chat/',
      AUTH_API_BASE_URL: '/api/auth/',
      ...ENV_CONFIG,
    },
  },
  metas: [
    {
      name: 'app_version',
      content: moment().format('YYYY-MM-DD HH:mm:ss'),
    },
  ],
  devServer: { port: 8002 },
  hash: true,
  // history: { type: 'hash' },
  antd: {},
  dva: {
    hmr: true,
  },
  layout: {
    name: '',
    locale: true,
    siderWidth: 208,
    ...defaultSettings,
  },
  locale: {
    // default zh-CN
    default: 'zh-CN',
    antd: true,
    // default true, when it is true, will use `navigator.language` overwrite default
    baseNavigator: false,
  },
  // dynamicImport: {
  //   loading: '@ant-design/pro-layout/es/PageLoading',
  // },
  targets: {
    ie: 11,
  },
  // umi routes: https://umijs.org/docs/routing
  routes,
  // Theme for antd: https://ant.design/docs/react/customize-theme-cn
  theme: {
    ...themeSettings,
  },
  esbuild: {},
  title: false,
  ignoreMomentLocale: true,
  proxy: proxy[REACT_APP_ENV || 'dev'],
  manifest: {
    basePath: '/',
  },
  base: publicPath,
  publicPath,
  outputPath: RUN_TYPE === 'local' ? 'supersonic-webapp' : 'dist',
  resolve: {
    includes: ['src/components'],
  },
});
