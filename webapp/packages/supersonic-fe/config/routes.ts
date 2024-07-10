export const ROUTE_AUTH_CODES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  MENU_CHAT: 'menu:chat',
  MENU_AGENT: 'menu:agent',
  MENU_PLUGIN: 'menu:plugin',
  MENU_MODEL: 'menu:model',
  MENU_METRIC_MARKET: 'menu:metric:market',
  MENU_TAG_MARKET: 'menu:tag:market',
  MENU_DATABASE: 'menu:database',
  MENU_SYSTEM: 'menu:system',
};

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
    access: ROUTE_AUTH_CODES.MENU_AGENT,
  },
  {
    path: '/plugin',
    name: 'plugin',
    component: './ChatPlugin',
    envEnableList: [ENV_KEY.CHAT],
    access: ROUTE_AUTH_CODES.MENU_PLUGIN,
  },
  {
    path: '/model/metric/edit/:metricId',
    name: 'metricEdit',
    hideInMenu: true,
    component: './SemanticModel/Metric/Edit',
    envEnableList: [ENV_KEY.SEMANTIC],
    access: ROUTE_AUTH_CODES.MENU_MODEL,
  },
  {
    path: '/model/',
    component: './SemanticModel/DomainManager',
    name: 'semanticModel',
    envEnableList: [ENV_KEY.SEMANTIC],
    access: ROUTE_AUTH_CODES.MENU_MODEL,
    routes: [
      {
        path: '/model/:domainId/:modelId',
        component: './SemanticModel/DomainManager',
        // name: 'semanticModel',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_MODEL,
      },
      {
        path: '/model/:domainId/:modelId/:menuKey',
        component: './SemanticModel/DomainManager',
        // name: 'semanticModel',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_MODEL,
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
    access: ROUTE_AUTH_CODES.MENU_METRIC_MARKET,
    routes: [
      {
        path: '/metric',
        redirect: '/metric/market',
        access: ROUTE_AUTH_CODES.MENU_METRIC_MARKET,
      },
      {
        path: '/metric/market',
        component: './SemanticModel/Metric/Market',
        hideInMenu: true,
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_METRIC_MARKET,
      },
      {
        path: '/metric/detail/:metricId',
        name: 'metricDetail',
        hideInMenu: true,
        component: './SemanticModel/Metric/Detail',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_METRIC_MARKET,
      },
      {
        path: '/metric/detail/edit/:metricId',
        name: 'metricDetail',
        hideInMenu: true,
        component: './SemanticModel/Metric/Edit',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_METRIC_MARKET,
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
        access: ROUTE_AUTH_CODES.MENU_TAG_MARKET,
      },
      {
        path: '/tag/market',
        component: './SemanticModel/Insights/Market',
        hideInMenu: true,
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_TAG_MARKET,
      },
      {
        path: '/tag/detail/:tagId',
        name: 'tagDetail',
        hideInMenu: true,
        component: './SemanticModel/Insights/Detail',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_TAG_MARKET,
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
    access: ROUTE_AUTH_CODES.MENU_DATABASE,
  },
  {
    path: '/system',
    name: 'system',
    component: './System',
    access: ROUTE_AUTH_CODES.MENU_SYSTEM,
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
