export interface MenuNode {
  name?: string;
  path?: string;
  hideInMenu?: boolean;
  children?: MenuNode[];
  [key: string]: unknown;
}

type MenuGroup = {
  name: string;
  path: string;
  childPaths: string[];
};

const MENU_GROUPS: MenuGroup[] = [
  {
    name: 'analysisCenter',
    path: '/analysis-center',
    childPaths: [
      '/operations-cockpit',
      '/business-topics',
      '/reports',
      '/task-center',
      '/responsibility-ledger',
      '/delivery-config',
      '/feishu-bot',
    ],
  },
  {
    name: 'aiQuery',
    path: '/ai-query',
    childPaths: ['/chat', '/agent', '/plugin'],
  },
  {
    name: 'dataModeling',
    path: '/data-modeling',
    childPaths: ['/model/', '/metric', '/tag', '/database', '/llm', '/semantic-template'],
  },
  {
    name: 'systemAdmin',
    path: '/system-admin',
    childPaths: ['/platform', '/tenant'],
  },
];

/**
 * Recursively filter menu nodes.
 * A node is kept if:
 *   1. Its own hideInMenu is falsy, AND
 *   2. It has no children, OR its children array is non-empty after filtering.
 *
 * Used as menuDataRender in app.tsx to hide parent groups whose
 * children are all hidden (by hideInMenu or env/access filtering).
 */
export function filterEmptyGroups(items: MenuNode[]): MenuNode[] {
  return items
    .map((item) => ({
      ...item,
      children: item.children ? filterEmptyGroups(item.children) : undefined,
    }))
    .filter((item) => {
      const hasVisibleChildren =
        item.children !== undefined && item.children !== null && item.children.length > 0;

      if (item.hideInMenu) {
        return false;
      }

      // Umi/Pro 会把无权限菜单节点变成“保留 path、移除 name”的占位项。
      // 这里需要和其默认菜单过滤保持一致，避免把不可见父级重新拼回菜单树。
      if (!item.name && !hasVisibleChildren) {
        return false;
      }

      return item.children === undefined || item.children === null || hasVisibleChildren;
    });
}

export function buildGroupedMenu(items: MenuNode[]): MenuNode[] {
  const visibleItems = filterEmptyGroups(items);
  const itemByPath = new Map<string, MenuNode>();

  visibleItems.forEach((item) => {
    if (item.path) {
      itemByPath.set(item.path, item);
    }
  });

  const groupedPaths = new Set<string>();
  const groupedMenus = MENU_GROUPS.map((group) => {
    const children = group.childPaths
      .map((path) => itemByPath.get(path))
      .filter((item): item is MenuNode => Boolean(item));

    children.forEach((item) => {
      if (item.path) {
        groupedPaths.add(item.path);
      }
    });

    return {
      name: group.name,
      path: group.path,
      children,
    };
  });

  const ungroupedItems = visibleItems.filter((item) => !item.path || !groupedPaths.has(item.path));

  return filterEmptyGroups([...groupedMenus, ...ungroupedItems]);
}
