import RightContent from '@/components/RightContent';
import S2Icon, { ICON } from '@/components/S2Icon';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import { Space, Spin, ConfigProvider, message } from 'antd';
import ScaleLoader from 'react-spinners/ScaleLoader';
import { history, RunTimeLayoutConfig } from '@umijs/max';
import defaultSettings from '../config/defaultSettings';
import settings from '../config/themeSettings';
import { getUserPermissions, queryCurrentUser } from './services/user';
import { deleteUrlQuery, isMobile, getToken } from '@/utils/utils';
import { publicPath } from '../config/defaultSettings';
import { Copilot } from 'supersonic-chat-sdk';
import { configProviderTheme } from '../config/themeSettings';
export { request } from './services/request';
import { ROUTE_AUTH_CODES } from '../config/routes';
import AppPage from './pages/index';
import type { API } from './services/API';
import logoSrc from '@/assets/logo.png';

const replaceRoute = '/';

const getRunningEnv = async () => {
  try {
    const response = await fetch(`${publicPath}supersonic.config.json`);
    const config = await response.json();
    return config;
  } catch (error) {
    console.warn('无法获取配置文件: 运行时环境将以semantic启动');
  }
};

Spin.setDefaultIndicator(
  <ScaleLoader color={settings['primary-color']} height={25} width={2} radius={2} margin={2} />,
);

const getAuthCodes = async (params: any) => {
  const { currentUser } = params;
  try {
    const { data: codes } = await getUserPermissions();
    if (currentUser?.superAdmin) {
      codes.push(ROUTE_AUTH_CODES.SYSTEM_ADMIN);
    }
    return codes;
  } catch (error) {
    message.error('权限接口调用失败');
    return [];
  }
};

export async function getInitialState(): Promise<{
  settings?: LayoutSettings;
  currentUser?: API.CurrentUser;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
  codeList?: string[];
  authCodes?: string[];
}> {
  const fetchUserInfo = async () => {
    try {
      const { code, data } = await queryCurrentUser();
      if (code === 200) {
        return { ...data, staffName: data.staffName || data.name };
      }
    } catch (error) {}
    return undefined;
  };

  let currentUser: any;
  if (!window.location.pathname.includes('login')) {
    currentUser = await fetchUserInfo();
  }

  if (currentUser) {
    localStorage.setItem('user', currentUser.staffName);
    if (currentUser.orgName) {
      localStorage.setItem('organization', currentUser.orgName);
    }
  }

  const authCodes = await getAuthCodes({
    currentUser,
  });

  return {
    fetchUserInfo,
    currentUser,
    settings: defaultSettings,
    authCodes,
  };
}

// export async function patchRoutes({ routes }) {
//   const config = await getRunningEnv();
//   if (config && config.env) {
//     window.RUNNING_ENV = config.env;
//     const { env } = config;
//     const target = routes[0].routes;
//     if (env) {
//       const envRoutes = traverseRoutes(target, env);
//       // 清空原本route;
//       target.splice(0, 99);
//       // 写入根据环境转换过的的route
//       target.push(...envRoutes);
//     }
//   } else {
//     const target = routes[0].routes;
//     // start-standalone模式不存在env，在此模式下不显示chatSetting
//     const envRoutes = target.filter((item: any) => {
//       return !['chatSetting'].includes(item.name);
//     });
//     target.splice(0, 99);
//     target.push(...envRoutes);
//   }
// }

export function onRouteChange() {
  // const title = window.document.title.split('-SuperSonic')[0];
  // if (!title.includes('SuperSonic')) {
  //   window.document.title = `${title}-SuperSonic`;
  // } else {
  //   window.document.title = 'SuperSonic';
  // }
}

export const layout: RunTimeLayoutConfig = (params) => {
  const { initialState } = params as any;

  console.log('🚀 ~ initialState?.currentUser?.superAdmin:', initialState?.currentUser?.superAdmin);

  return {
    onMenuHeaderClick: (e) => {
      e.preventDefault();
      history.push(replaceRoute);
    },
    logo: (
      <Space>
        {/* <S2Icon
          icon={ICON.iconlogobiaoshi}
          size={30}
          color="#1672fa"
          style={{ display: 'inline-block', marginTop: 8 }}
        /> */}
        <img src={logoSrc} alt="logo" style={{ height: 34 }} />
        <div className="logo" style={{ position: 'relative', top: '-2px' }}>
          Chatdata
        </div>
      </Space>
    ),
    contentStyle: { ...(initialState?.contentStyle || {}) },
    rightContentRender: () => <RightContent />,
    disableContentMargin: true,
    // menuHeaderRender: undefined,
    childrenRender: (dom) => {
      return (
        <ConfigProvider theme={configProviderTheme}>
          <div
            style={{
              height: location.pathname.includes('chat') ? 'calc(100vh - 56px)' : undefined,
            }}
          >
            <AppPage dom={dom} />
            {/* {dom} */}
            {history.location.pathname !== '/chat' && !isMobile && (
              <Copilot
                token={getToken() || ''}
                isDeveloper={
                  process.env.NODE_ENV === 'development' || initialState?.currentUser?.superAdmin
                }
              />
            )}
          </div>
        </ConfigProvider>
      );
    },
    ...initialState?.settings,
  };
};
