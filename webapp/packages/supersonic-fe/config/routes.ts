export const ROUTE_AUTH_CODES = { SYSTEM_ADMIN: 'SYSTEM_ADMIN' };

const ENV_KEY = {
  CHAT: 'chat',
  SEMANTIC: 'semantic',
};

const { APP_TARGET } = process.env;

const ROUTES = [
  {
    path: '/chat/mobile',
    name: 'chat',
    component: './ChatPage',
    hideInMenu: true,
    layout: false,
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/chat',
    name: 'chat',
    component: './ChatPage',
    envEnableList: [ENV_KEY.CHAT],
  },
  // {
  //   path: '/chatSetting/model/:domainId?/:modelId?/:menuKey?',
  //   component: './SemanticModel/ChatSetting/ChatSetting',
  //   name: 'chatSetting',
  //   envEnableList: [ENV_KEY.CHAT],
  // },
  {
    path: '/agent',
    name: 'agent',
    component: './Agent',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/plugin',
    name: 'plugin',
    component: './ChatPlugin',
    envEnableList: [ENV_KEY.CHAT],
  },
  {
    path: '/model/metric/edit/:metricId',
    name: 'metricEdit',
    hideInMenu: true,
    component: './SemanticModel/Metric/Edit',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/model/',
    component: './SemanticModel/DomainManager',
    name: 'semanticModel',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/model/:domainId/:modelId',
        component: './SemanticModel/DomainManager',
        // name: 'semanticModel',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/model/:domainId/:modelId/:menuKey',
        component: './SemanticModel/DomainManager',
        // name: 'semanticModel',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
    ],
  },

  // {
  //   path: '/model/:domainId/:modelId/:menuKey',
  //   component: './SemanticModel/DomainManager',
  //   name: 'semanticModel',
  //   envEnableList: [ENV_KEY.SEMANTIC],
  // },

  {
    path: '/metric',
    name: 'metric',
    component: './SemanticModel/Metric',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/metric',
        redirect: '/metric/market',
      },
      {
        path: '/metric/market',
        component: './SemanticModel/Metric/Market',
        hideInMenu: true,
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/metric/detail/:metricId',
        name: 'metricDetail',
        hideInMenu: true,
        component: './SemanticModel/Metric/Detail',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/metric/detail/edit/:metricId',
        name: 'metricDetail',
        hideInMenu: true,
        component: './SemanticModel/Metric/Edit',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
    ],
  },

  {
    path: '/tag',
    name: 'tag',
    component: './SemanticModel/Insights',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/tag',
        redirect: '/tag/market',
      },
      {
        path: '/tag/market',
        component: './SemanticModel/Insights/Market',
        hideInMenu: true,
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/tag/detail/:tagId',
        name: 'tagDetail',
        hideInMenu: true,
        component: './SemanticModel/Insights/Detail',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
    ],
  },

  {
    path: '/login',
    name: 'login',
    layout: false,
    hideInMenu: true,
    component: './Login',
  },
  {
    path: '/database',
    name: 'database',
    // hideInMenu: true,
    component: './SemanticModel/components/Database/DatabaseTable',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/system',
    name: 'system',
    component: './System',
    access: ROUTE_AUTH_CODES.SYSTEM_ADMIN,
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
