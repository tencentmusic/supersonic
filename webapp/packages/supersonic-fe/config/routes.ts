export const ROUTE_AUTH_CODES = {};

const ENV_KEY = {
  CHAT: 'chat',
  SEMANTIC: 'semantic',
};

const { APP_TARGET } = process.env;

const ROUTES = [
  {
    path: '/chat',
    name: 'chat',
    component: './Chat',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/chatSetting/model/:domainId?/:modelId?/:menuKey?',
    component: './SemanticModel/ChatSetting/ChatSetting',
    name: 'chatSetting',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/chatPlugin',
    name: 'chatPlugin',
    component: './ChatPlugin',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/agent',
    name: 'agent',
    component: './Agent',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/semanticModel/model/:domainId?/:modelId?/:menuKey?',
    component: './SemanticModel/DomainManager',
    name: 'semanticModel',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/Metric',
    name: 'metric',
    component: './SemanticModel/Metric',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/login',
    name: 'login',
    layout: false,
    hideInMenu: true,
    component: './Login',
  },
  {
    path: '/',
    redirect: APP_TARGET === 'inner' ? '/semanticModel/model/' : '/chat',
    envRedirect: {
      [ENV_KEY.CHAT]: '/chat',
      [ENV_KEY.SEMANTIC]: '/semanticModel/model',
    },
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
