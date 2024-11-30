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
    path: '/chat/external',
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
    component: './SemanticModel/',
    name: 'semanticModel',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/model/',
        redirect: '/model/domain',
      },
      {
        path: '/model/domain/',
        component: './SemanticModel/OverviewContainer',
        routes: [
          {
            path: '/model/domain/:domainId',
            component: './SemanticModel/DomainManager',
            routes: [
              {
                path: '/model/domain/:domainId/:menuKey',
                component: './SemanticModel/DomainManager',
              },
            ],
          },
          {
            path: '/model/domain/manager/:domainId/:modelId',
            component: './SemanticModel/ModelManager',
            routes: [
              {
                path: '/model/domain/manager/:domainId/:modelId/:menuKey',
                component: './SemanticModel/ModelManager',
              },
            ],
          },
        ],
      },
      {
        path: '/model/dataset/:domainId/:datasetId',
        component: './SemanticModel/View/components/Detail',
        envEnableList: [ENV_KEY.SEMANTIC],
        routes: [
          {
            path: '/model/dataset/:domainId/:datasetId/:menuKey',
            component: './SemanticModel/View/components/Detail',
          },
        ],
      },
      {
        path: '/model/metric/:domainId/:modelId/:metricId',
        component: './SemanticModel/Metric/Edit',
        envEnableList: [ENV_KEY.SEMANTIC],
        // routes: [
        //   {
        //     path: '/model/manager/:domainId/:modelId/:menuKey',
        //     component: './SemanticModel/ModelManager',
        //   },
        // ],
      },
      {
        path: '/model/dimension/:domainId/:modelId/:dimensionId',
        component: './SemanticModel/Dimension/Detail',
        envEnableList: [ENV_KEY.SEMANTIC],
        // routes: [
        //   {
        //     path: '/model/manager/:domainId/:modelId/:menuKey',
        //     component: './SemanticModel/ModelManager',
        //   },
        // ],
      },
    ],
  },

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
    hideInMenu: process.env.SHOW_TAG ? false : true,
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
    component: './SemanticModel/components/Database/DatabaseTable',
    envEnableList: [ENV_KEY.SEMANTIC],
  },
  {
    path: '/llm',
    name: 'llm',
    component: './SemanticModel/components/LLM/LlmTable',
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
    redirect: '/model',
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
