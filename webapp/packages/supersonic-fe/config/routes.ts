export const ROUTE_AUTH_CODES = {};

const ENV_KEY = {
  CHAT: 'chat',
  SEMANTIC: 'semantic',
};

const { APP_TARGET } = process.env;

const ROUTES = [
  {
    path: '/chat/mobile',
    name: 'chat',
    component: './Chat',
    hideInMenu: true,
    layout: false,
    envEnableList: [ENV_KEY.CHAT],
  },
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
    path: '/agent',
    name: 'agent',
    component: './Agent',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/model',
    name: 'semanticModel',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/model/:domainId?/:modelId?/:menuKey?',
        component: './SemanticModel/DomainManager',
        name: 'model',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/database',
        name: 'database',
        component: './SemanticModel/components/Database/DatabaseTable',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
    ],
  },

  {
    path: '/database',
    name: 'database',
    hideInMenu: true,
    component: './SemanticModel/components/Database/DatabaseTable',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/metric',
    name: 'metric',
    component: './SemanticModel/Metric',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/plugin',
    name: 'plugin',
    component: './ChatPlugin',
    envEnableList: [ENV_KEY.CHAT],
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
    redirect: APP_TARGET === 'inner' ? '/model' : '/chat',
    envRedirect: {
      [ENV_KEY.CHAT]: '/chat',
      [ENV_KEY.SEMANTIC]: '/model',
    },
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
