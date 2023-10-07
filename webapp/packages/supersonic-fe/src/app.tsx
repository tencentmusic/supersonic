import RightContent from '@/components/RightContent';
import S2Icon, { ICON } from '@/components/S2Icon';
import type { Settings as LayoutSettings } from '@ant-design/pro-layout';
import { Space, Spin } from 'antd';
import ScaleLoader from 'react-spinners/ScaleLoader';
import type { RunTimeLayoutConfig } from 'umi';
import { history } from 'umi';
import defaultSettings from '../config/defaultSettings';
import settings from '../config/themeSettings';
import { queryCurrentUser } from './services/user';
import { traverseRoutes, isMobile, getToken } from './utils/utils';
import { publicPath } from '../config/defaultSettings';
import { Copilot } from 'supersonic-chat-sdk';
export { request } from './services/request';

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

export const initialStateConfig = {
  loading: (
    <Spin wrapperClassName="initialLoading">
      <div className="loadingPlaceholder" />
    </Spin>
  ),
};

const getAuthCodes = () => {
  return [];
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

  const authCodes = getAuthCodes();

  return {
    fetchUserInfo,
    currentUser,
    settings: defaultSettings,
    authCodes,
  };
}

export async function patchRoutes({ routes }) {
  const config = await getRunningEnv();
  if (config && config.env) {
    window.RUNNING_ENV = config.env;
    const { env } = config;
    const target = routes[0].routes;
    if (env) {
      const envRoutes = traverseRoutes(target, env);
      // 清空原本route;
      target.splice(0, 99);
      // 写入根据环境转换过的的route
      target.push(...envRoutes);
    }
  } else {
    const target = routes[0].routes;
    // start-standalone模式不存在env，在此模式下不显示chatSetting
    const envRoutes = target.filter((item: any) => {
      return !['chatSetting'].includes(item.name);
    });
    target.splice(0, 99);
    target.push(...envRoutes);
  }
}

export const layout: RunTimeLayoutConfig = (params) => {
  const { initialState } = params as any;
  return {
    onMenuHeaderClick: (e) => {
      e.preventDefault();
      history.push(replaceRoute);
    },
    logo: (
      <Space>
        <S2Icon
          icon={ICON.iconlogobiaoshi}
          size={30}
          color="#fff"
          style={{ display: 'inline-block', marginTop: 8 }}
        />
        <div className="logo">超音数(SuperSonic)</div>
      </Space>
    ),
    contentStyle: { ...(initialState?.contentStyle || {}) },
    rightContentRender: () => <RightContent />,
    disableContentMargin: true,
    menuHeaderRender: undefined,
    childrenRender: (dom) => {
      return (
        <div
          style={{ height: location.pathname.includes('chat') ? 'calc(100vh - 48px)' : undefined }}
        >
          {dom}
          {history.location.pathname !== '/chat' && !isMobile && (
            <Copilot token={getToken() || ''} isDeveloper />
          )}
        </div>
      );
    },
    ...initialState?.settings,
  };
};
