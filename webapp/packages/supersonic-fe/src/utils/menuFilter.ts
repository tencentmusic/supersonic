export interface MenuNode {
  name?: string;
  path?: string;
  hideInMenu?: boolean;
  children?: MenuNode[];
  [key: string]: unknown;
}

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
    .filter(
      (item) =>
        !item.hideInMenu &&
        (item.children === undefined || item.children === null || item.children.length > 0),
    );
}
