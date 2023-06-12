import type { API } from '@/services/API';

import type { DataNode } from 'antd/lib/tree';

export const changeTreeData = (treeData: API.ProjectList, auth?: boolean): DataNode[] => {
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

export const addPathInTreeData = (treeData: API.ProjectList, loopPath: any[] = []): any => {
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
      nodeItem.title = nodeItem.name;
      nodeList.push(nodeItem);
    }
    return nodeList;
  }, []);
  return tree;
};

export const treeParentKeyLists = (treeData: API.ProjectList): string[] => {
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
    if (item.subDepartments) {
      newStepList = newStepList.concat(item.subDepartments);
    }
    return item.key === projectId;
  });
  if (departmentData) {
    return departmentData;
  }
  return findDepartmentTree(newStepList, projectId);
};
