import type { API } from '@/services/API';
import { ISemantic } from './data';
import type { DataNode } from 'antd/lib/tree';

export const changeTreeData = (treeData: API.DomainList, auth?: boolean): DataNode[] => {
  return treeData.map((item: any) => {
    const newItem: DataNode = {
      ...item,
      key: item.id,
      disabled: auth,
      children: item.children ? changeTreeData(item.children, auth) : [],
    };
    return newItem;
  });
};

export const addPathInTreeData = (treeData: API.DomainList, loopPath: any[] = []): any => {
  return treeData.map((item: any) => {
    const { children, parentId = [] } = item;
    const path = loopPath.slice();
    path.push(parentId);
    if (children) {
      return {
        ...item,
        path,
        children: addPathInTreeData(children, path),
      };
    }
    return {
      ...item,
      path,
    };
  });
};

export const constructorClassTreeFromList = (list: any[], parentId: number = 0) => {
  const tree = list.reduce((nodeList, nodeItem) => {
    if (nodeItem.parentId == parentId) {
      const children = constructorClassTreeFromList(list, nodeItem.id);
      if (children.length) {
        nodeItem.children = children;
      }
      nodeItem.key = nodeItem.id;
      nodeItem.value = nodeItem.id;
      nodeItem.title = nodeItem.name;
      nodeList.push(nodeItem);
    }
    return nodeList;
  }, []);
  return tree;
};

export const treeParentKeyLists = (treeData: API.DomainList): string[] => {
  let keys: string[] = [];
  treeData.forEach((item: any) => {
    if (item.children && item.children.length > 0) {
      keys.push(item.id);
      keys = keys.concat(treeParentKeyLists(item.children));
    }
  });
  return keys;
};

// bfs 查询树结构
export const findDepartmentTree: any = (treeData: any[], projectId: string) => {
  if (treeData.length === 0) {
    return [];
  }
  let newStepList: any[] = [];
  const departmentData = treeData.find((item) => {
    if (item.subOrganizations) {
      newStepList = newStepList.concat(item.subOrganizations);
    }
    return item.id === projectId;
  });
  if (departmentData) {
    return departmentData;
  }
  return findDepartmentTree(newStepList, projectId);
};

const isDescendant = (
  node: ISemantic.IDomainItem,
  parentId: number,
  nodes: ISemantic.IDomainItem[],
): boolean => {
  // 如果当前节点的 parentId 与指定的 parentId 相同，则说明它是指定节点的子节点
  if (node.parentId === parentId) {
    return true;
  }

  // 递归检查当前节点的父节点是否是指定节点的子节点
  const parentNode = nodes.find((n) => n.id === node.parentId);
  if (parentNode) {
    return isDescendant(parentNode, parentId, nodes);
  }

  // 如果找不到父节点，则说明当前节点不是指定节点的子孙节点
  return false;
};

export const findLeafNodesFromDomainList = (
  nodes: ISemantic.IDomainItem[],
  id: number | null = null,
): ISemantic.IDomainItem[] => {
  const leafNodes: ISemantic.IDomainItem[] = [];

  // 遍历所有节点
  for (const node of nodes) {
    let isLeaf = true;

    // 检查当前节点是否有子节点
    for (const childNode of nodes) {
      if (childNode.parentId === node.id) {
        isLeaf = false;
        break;
      }
    }

    // 如果当前节点是叶子节点，并且满足指定的 id 条件，则将其添加到结果数组中
    if (isLeaf && (id === null || isDescendant(node, id, nodes))) {
      leafNodes.push(node);
    }
  }

  return leafNodes;
};
