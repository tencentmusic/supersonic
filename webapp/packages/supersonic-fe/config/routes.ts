export const ROUTE_AUTH_CODES = {};

const ENV_KEY = {
  CHAT: 'chat',
  SEMANTIC: 'semantic',
};

const { APP_TARGET } = process.env;

const ROUTES = [
  ...(APP_TARGET !== 'inner'
    ? [
        {
          path: '/chat',
          name: 'chat',
          component: './Chat',
          envEnableList: [ENV_KEY.CHAT],
        },
      ]
    : []),
  {
    path: '/chatSetting/:modelId?/:menuKey?',
    name: 'chatSetting',
    component: './SemanticModel/ChatSetting',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/semanticModel/:modelId?/:menuKey?',
    name: 'semanticModel',
    component: './SemanticModel/ProjectManager',
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
    redirect: APP_TARGET === 'inner' ? '/semanticModel' : '/chat',
    envRedirect: {
      [ENV_KEY.CHAT]: '/chat',
      [ENV_KEY.SEMANTIC]: '/semanticModel',
    },
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
