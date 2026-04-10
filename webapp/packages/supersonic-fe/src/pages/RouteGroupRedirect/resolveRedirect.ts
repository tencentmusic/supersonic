export type RedirectCandidate = {
  path: string;
  access?: string;
  enabled?: boolean;
};

const ROUTE_AUTH_CODES = {
  MENU_CHAT: 'MENU_CHAT',
  MENU_MODEL: 'MENU_MODEL',
  MENU_METRIC: 'MENU_METRIC',
  MENU_AGENT: 'MENU_AGENT',
  MENU_PLUGIN: 'MENU_PLUGIN',
  MENU_DATABASE: 'MENU_DATABASE',
  MENU_LLM: 'MENU_LLM',
  MENU_SEMANTIC_TEMPLATE: 'MENU_SEMANTIC_TEMPLATE',
  PLATFORM_ADMIN: 'PLATFORM_ADMIN',
  TENANT_ADMIN: 'TENANT_ADMIN',
};

export const GROUP_REDIRECTS: Record<string, RedirectCandidate[]> = {
  '/analysis-center': [{ path: '/operations-cockpit' }],
  '/ai-query': [
    { path: '/chat', access: ROUTE_AUTH_CODES.MENU_CHAT },
    { path: '/agent', access: ROUTE_AUTH_CODES.MENU_AGENT },
    { path: '/plugin', access: ROUTE_AUTH_CODES.MENU_PLUGIN },
  ],
  '/data-modeling': [
    { path: '/model/', access: ROUTE_AUTH_CODES.MENU_MODEL },
    { path: '/metric', access: ROUTE_AUTH_CODES.MENU_METRIC },
    { path: '/tag', enabled: !!process.env.SHOW_TAG },
    { path: '/database', access: ROUTE_AUTH_CODES.MENU_DATABASE },
    { path: '/llm', access: ROUTE_AUTH_CODES.MENU_LLM },
    { path: '/semantic-template', access: ROUTE_AUTH_CODES.MENU_SEMANTIC_TEMPLATE },
  ],
  '/system-admin': [
    { path: '/platform', access: ROUTE_AUTH_CODES.PLATFORM_ADMIN },
    { path: '/tenant', access: ROUTE_AUTH_CODES.TENANT_ADMIN },
  ],
};

export function resolveGroupRedirect(pathname: string, authCodes: string[] = []) {
  const candidates = GROUP_REDIRECTS[pathname];
  if (!candidates) {
    return null;
  }

  const target = candidates.find((candidate) => {
    if (candidate.enabled === false) {
      return false;
    }
    return !candidate.access || authCodes.includes(candidate.access);
  });

  return target?.path || '/401';
}
