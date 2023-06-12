import type { Settings as LayoutSettings } from '@ant-design/pro-layout';
import { Spin, Space } from 'antd';
import ScaleLoader from 'react-spinners/ScaleLoader';
import { history } from 'umi';
import type { RunTimeLayoutConfig } from 'umi';
import RightContent from '@/components/RightContent';
import S2Icon, { ICON } from '@/components/S2Icon';
import qs from 'qs';
import { queryCurrentUser } from './services/user';
import { queryToken } from './services/login';
import defaultSettings from '../config/defaultSettings';
import settings from '../config/themeSettings';
import { deleteUrlQuery } from './utils/utils';
import { AUTH_TOKEN_KEY, FROM_URL_KEY } from '@/common/constants';
import 'supersonic-chat-sdk/dist/index.css';
import { setToken as setChatSdkToken } from 'supersonic-chat-sdk';
export { request } from './services/request';
import { ROUTE_AUTH_CODES } from '../config/routes';

const TOKEN_KEY = AUTH_TOKEN_KEY;

const replaceRoute = '/';

const getRuningEnv = async () => {
  try {
    // const response = await fetch(`supersonic.config.json`);
    // const config = await response.json();
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

const getToken = async () => {
  let { search } = window.location;
  if (search.length > 0) {
    search = search.slice(1);
  }
  const data = qs.parse(search);
  if (data.code) {
    try {
      const fromUrl = localStorage.getItem(FROM_URL_KEY);
      const res = await queryToken(data.code as string);
      localStorage.setItem(TOKEN_KEY, res.data.authToken);
      const newUrl = deleteUrlQuery(window.location.href, 'code');
      window.location.href = fromUrl || newUrl;
    } catch (err) {
      console.log(err);
    }
  }
};

const getAuthCodes = () => {
  const { RUN_TYPE, APP_TARGET } = process.env;
  if (RUN_TYPE === 'local') {
    return location.host.includes('9080')
      ? [ROUTE_AUTH_CODES.CHAT, ROUTE_AUTH_CODES.CHAT_SETTING]
      : [ROUTE_AUTH_CODES.SEMANTIC];
  }
  if (APP_TARGET === 'inner') {
    return [ROUTE_AUTH_CODES.CHAT_SETTING, ROUTE_AUTH_CODES.SEMANTIC];
  }
  return [ROUTE_AUTH_CODES.CHAT, ROUTE_AUTH_CODES.CHAT_SETTING, ROUTE_AUTH_CODES.SEMANTIC];
};

export async function getInitialState(): Promise<{
  settings?: LayoutSettings;
  currentUser?: API.CurrentUser;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
  codeList?: string[];
  authCodes?: string[];
}> {
  await getRuningEnv();
  const fetchUserInfo = async () => {
    try {
      const { code, data } = await queryCurrentUser();
      if (code === 200) {
        return { ...data, staffName: data.staffName || data.name };
      }
    } catch (error) {}
    return undefined;
  };
  const { query } = history.location as any;
  const currentToken = query[TOKEN_KEY] || localStorage.getItem(TOKEN_KEY);

  if (window.location.host.includes('tmeoa') && !currentToken) {
    await getToken();
  }

  setChatSdkToken(localStorage.getItem(AUTH_TOKEN_KEY) || '');

  const currentUser = await fetchUserInfo();

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
    onPageChange: (location: any) => {
      const { pathname } = location;
      const { RUN_TYPE, APP_TARGET } = process.env;
      if (
        (RUN_TYPE === 'local' && !window.location.host.includes('9080') && pathname === '/chat') ||
        (APP_TARGET === 'inner' && pathname === '/chat')
      ) {
        history.push('/semanticModel');
      }
    },
    menuHeaderRender: undefined,
    childrenRender: (dom) => {
      return dom;
    },
    openKeys: false,
    ...initialState?.settings,
  };
};
