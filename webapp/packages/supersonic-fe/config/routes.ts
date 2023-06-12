export const ROUTE_AUTH_CODES = {
  CHAT: 'chat',
  CHAT_SETTING: 'chatSetting',
  SEMANTIC: 'semantic',
};

const ROUTES = [
  {
    path: '/chat',
    name: 'chat',
    component: './Chat',
    access: ROUTE_AUTH_CODES.CHAT,
  },
  {
    path: '/chatSetting',
    name: 'chatSetting',
    component: './SemanticModel/ChatSetting',
    access: ROUTE_AUTH_CODES.CHAT_SETTING,
  },
  {
    path: '/semanticModel',
    name: 'semanticModel',
    component: './SemanticModel/ProjectManager',
    access: ROUTE_AUTH_CODES.SEMANTIC,
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
    redirect: '/chat',
  },
  {
    path: '/401',
    component: './401',
  },
];

export default ROUTES;
