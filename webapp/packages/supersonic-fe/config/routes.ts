// 权限码常量，与后端 s2_permission 表的 code 字段对应
export const ROUTE_AUTH_CODES = {
  // ========== 通用权限 ==========
  SYSTEM_ADMIN: 'SYSTEM_ADMIN', // 系统超级管理员

  // ========== 业务菜单权限 ==========
  MENU_CHAT: 'MENU_CHAT',
  MENU_MODEL: 'MENU_MODEL',
  MENU_METRIC: 'MENU_METRIC',
  MENU_AGENT: 'MENU_AGENT',
  MENU_PLUGIN: 'MENU_PLUGIN',
  MENU_DATABASE: 'MENU_DATABASE',
  MENU_LLM: 'MENU_LLM',
  MENU_SEMANTIC_TEMPLATE: 'MENU_SEMANTIC_TEMPLATE',

  // ========== 语义模板操作权限 ==========
  API_TEMPLATE_VIEW: 'API_TEMPLATE_VIEW',       // 查看模板
  API_TEMPLATE_CREATE: 'API_TEMPLATE_CREATE',   // 创建模板
  API_TEMPLATE_UPDATE: 'API_TEMPLATE_UPDATE',   // 编辑模板
  API_TEMPLATE_DELETE: 'API_TEMPLATE_DELETE',   // 删除模板
  API_TEMPLATE_DEPLOY: 'API_TEMPLATE_DEPLOY',   // 部署模板

  // ========== 平台级权限 (Platform RBAC) ==========
  PLATFORM_ADMIN: 'PLATFORM_ADMIN',                     // 平台管理员
  PLATFORM_TENANT_MANAGE: 'PLATFORM_TENANT_MANAGE',     // 租户管理
  PLATFORM_SUBSCRIPTION: 'PLATFORM_SUBSCRIPTION',       // 订阅计划管理
  PLATFORM_ROLE_MANAGE: 'PLATFORM_ROLE_MANAGE',         // 平台角色管理
  PLATFORM_PERMISSION: 'PLATFORM_PERMISSION',           // 平台权限管理
  PLATFORM_SETTINGS: 'PLATFORM_SETTINGS',               // 系统设置

  // ========== 租户级权限 (Tenant RBAC) ==========
  TENANT_ADMIN: 'TENANT_ADMIN',                         // 租户管理员
  TENANT_ORG_MANAGE: 'TENANT_ORG_MANAGE',               // 组织架构管理
  TENANT_MEMBER_MANAGE: 'TENANT_MEMBER_MANAGE',         // 成员管理
  TENANT_ROLE_MANAGE: 'TENANT_ROLE_MANAGE',             // 租户角色管理
  TENANT_PERMISSION: 'TENANT_PERMISSION',               // 租户权限管理
  TENANT_SETTINGS: 'TENANT_SETTINGS',                   // 租户设置
  TENANT_USAGE_VIEW: 'TENANT_USAGE_VIEW',               // 用量查看
};

const ENV_KEY = {
  CHAT: 'chat',
  SEMANTIC: 'semantic',
};

const ROUTES = [
  // ──────────────────────────────────────────────────────────
  // layout:false routes — bypass Pro Layout entirely.
  // MUST be declared before any group parent nodes.
  // ──────────────────────────────────────────────────────────
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

  // ========== 分析中心 ==========
  {
    name: 'analysisCenter',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
      {
        path: '/operations-cockpit',
        name: 'operationsCockpit',
        component: './OperationsCockpit',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/business-topics',
        name: 'businessTopics',
        component: './BusinessTopics',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/business-topics/workspace/:topicId',
        name: 'topicWorkspace',
        component: './BusinessTopics/TopicWorkspace',
        envEnableList: [ENV_KEY.SEMANTIC],
        hideInMenu: true,
      },
      {
        path: '/reports',
        name: 'reports',
        component: './Reports',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/task-center',
        name: 'taskCenter',
        component: './TaskCenter',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/responsibility-ledger',
        name: 'responsibilityLedger',
        component: './ResponsibilityLedger',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/delivery-config',
        name: 'deliveryConfig',
        component: './DeliveryConfig',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/feishu-bot',
        name: 'feishuBot',
        component: './FeishuBot',
        envEnableList: [ENV_KEY.SEMANTIC],
      },
      {
        path: '/report-schedule',
        name: 'reportSchedule',
        component: './ReportSchedule',
        envEnableList: [ENV_KEY.SEMANTIC],
        hideInMenu: true,
      },
    ],
  },

  // ========== 智能问答 ==========
  {
    name: 'aiQuery',
    envEnableList: [ENV_KEY.CHAT],
    routes: [
      {
        path: '/chat',
        name: 'chat',
        component: './ChatPage',
        envEnableList: [ENV_KEY.CHAT],
        access: ROUTE_AUTH_CODES.MENU_CHAT,
      },
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
    ],
  },

  // ========== 数据建模 ==========
  {
    name: 'dataModeling',
    envEnableList: [ENV_KEY.SEMANTIC],
    routes: [
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
        access: ROUTE_AUTH_CODES.MENU_MODEL,
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
          },
          {
            path: '/model/dimension/:domainId/:modelId/:dimensionId',
            component: './SemanticModel/Dimension/Detail',
            envEnableList: [ENV_KEY.SEMANTIC],
          },
        ],
      },
      {
        path: '/metric',
        name: 'metric',
        component: './SemanticModel/Metric',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_METRIC,
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
        path: '/database',
        name: 'database',
        component: './SemanticModel/components/Database/DatabaseTable',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_DATABASE,
      },
      {
        path: '/llm',
        name: 'llm',
        component: './SemanticModel/components/LLM/LlmTable',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_LLM,
      },
      {
        path: '/semantic-template',
        name: 'semanticTemplate',
        component: './SemanticTemplate',
        envEnableList: [ENV_KEY.SEMANTIC],
        access: ROUTE_AUTH_CODES.MENU_SEMANTIC_TEMPLATE,
      },
    ],
  },

  // ========== 系统管理 ==========
  {
    name: 'systemAdmin',
    routes: [
      {
        path: '/platform',
        name: 'platform',
        access: ROUTE_AUTH_CODES.PLATFORM_ADMIN,
        routes: [
          {
            path: '/platform',
            redirect: '/platform/tenants',
          },
          {
            path: '/platform/tenants',
            name: 'tenants',
            component: './AdminTenant',
            access: ROUTE_AUTH_CODES.PLATFORM_TENANT_MANAGE,
          },
          {
            path: '/platform/subscriptions',
            name: 'subscriptions',
            component: './Platform/SubscriptionManagement',
            access: ROUTE_AUTH_CODES.PLATFORM_SUBSCRIPTION,
          },
          {
            path: '/platform/roles',
            name: 'platformRoles',
            component: './Platform/RoleManagement',
            access: ROUTE_AUTH_CODES.PLATFORM_ROLE_MANAGE,
          },
          {
            path: '/platform/permissions',
            name: 'platformPermissions',
            component: './Platform/PermissionManagement',
            access: ROUTE_AUTH_CODES.PLATFORM_PERMISSION,
          },
          {
            path: '/platform/settings',
            name: 'platformSettings',
            component: './System',
            access: ROUTE_AUTH_CODES.PLATFORM_SETTINGS,
          },
        ],
      },
      {
        path: '/tenant',
        name: 'tenant',
        access: ROUTE_AUTH_CODES.TENANT_ADMIN,
        routes: [
          {
            path: '/tenant',
            redirect: '/tenant/organization',
          },
          {
            path: '/tenant/organization',
            name: 'organization',
            component: './System/OrganizationManagement',
            access: ROUTE_AUTH_CODES.TENANT_ORG_MANAGE,
          },
          {
            path: '/tenant/members',
            name: 'members',
            component: './Tenant/MemberManagement',
            access: ROUTE_AUTH_CODES.TENANT_MEMBER_MANAGE,
          },
          {
            path: '/tenant/roles',
            name: 'tenantRoles',
            component: './Tenant/RoleManagement',
            access: ROUTE_AUTH_CODES.TENANT_ROLE_MANAGE,
          },
          {
            path: '/tenant/permissions',
            name: 'tenantPermissions',
            component: './Tenant/PermissionManagement',
            access: ROUTE_AUTH_CODES.TENANT_PERMISSION,
          },
          {
            path: '/tenant/settings',
            name: 'tenantSettings',
            component: './TenantSettings',
            access: ROUTE_AUTH_CODES.TENANT_SETTINGS,
          },
          {
            path: '/tenant/usage',
            name: 'usage',
            component: './UsageDashboard',
            access: ROUTE_AUTH_CODES.TENANT_USAGE_VIEW,
          },
        ],
      },
    ],
  },

  // ========== 系统路由（不出现在菜单中） ==========
  {
    path: '/login',
    name: 'login',
    layout: false,
    hideInMenu: true,
    component: './Login',
  },
  {
    path: '/login/callback',
    name: 'oauthCallback',
    layout: false,
    hideInMenu: true,
    component: './Login/OAuthCallback',
  },
  {
    path: '/',
    redirect: '/operations-cockpit',
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
