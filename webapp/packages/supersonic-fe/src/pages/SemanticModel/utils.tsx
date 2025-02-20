import type { API } from '@/services/API';
import { ISemantic } from './data';
import type { DataNode } from 'antd/lib/tree';
import { Form, Input, InputNumber, Switch, Select, Slider } from 'antd';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import DisabledWheelNumberInput from '@/components/DisabledWheelNumberInput';
import { ConfigParametersItem } from '../System/types';
import { TransType } from './enum';
import { isString, isBoolean } from 'lodash';
import { ReactNode } from 'react';
import { history } from '@umijs/max';
import { openNewPage } from '@/utils/utils';

const FormItem = Form.Item;
const { TextArea } = Input;

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
  return treeData?.map((item: any) => {
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
  const tree = list?.reduce((nodeList, nodeItem) => {
    if (nodeItem.parentId == parentId) {
      const children = constructorClassTreeFromList(list, nodeItem.id);
      if (children.length) {
        nodeItem.children = children;
      }
      nodeItem.key = nodeItem.id;
      nodeItem.value = nodeItem.id;
      nodeItem.title = nodeItem.name || nodeItem.categoryName;
      nodeList.push(nodeItem);
    }
    return nodeList;
  }, []);
  return tree;
};

export const treeParentKeyLists = (treeData: API.DomainList): string[] => {
  let keys: string[] = [];
  treeData?.forEach((item: any) => {
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

export const genneratorFormItemList = (itemList: ConfigParametersItem[]) => {
  const list = itemList.reduce((itemList: ReactNode[], item) => {
    const { dataType, name, comment, placeholder, description, require, visible, sliderConfig } =
      item;
    if (visible === false) {
      return itemList;
    }
    let defaultItem = <Input />;
    switch (dataType) {
      case 'string':
        if (name === 'password') {
          defaultItem = <Input.Password placeholder={placeholder} />;
        } else {
          defaultItem = <Input placeholder={placeholder} />;
        }

        break;
      case 'password': {
        defaultItem = (
          <Input.Password placeholder={placeholder} visibilityToggle={name !== 'apiKey'} />
        );
        break;
      }
      case 'longText':
        defaultItem = <TextArea placeholder={placeholder} style={{ height: 100 }} />;
        break;
      case 'number':
        // defaultItem = <InputNumber placeholder={placeholder} style={{ width: '100%' }} />;
        defaultItem = (
          <DisabledWheelNumberInput placeholder={placeholder} style={{ width: '100%' }} />
        );
        break;
      case 'slider':
        defaultItem = (
          <Slider
            min={sliderConfig?.start?.value ? Number(sliderConfig?.start?.value) : 0}
            max={sliderConfig?.end?.value ? Number(sliderConfig?.end?.value) : 1}
            step={sliderConfig?.unit ? Number(sliderConfig?.unit) : 0.1}
            marks={
              // sliderConfig?.start?.text && sliderConfig?.end?.text?
              {
                0: sliderConfig?.start?.text || '精确',
                1: sliderConfig?.end?.text || '随机',
              }
              // : undefined
            }
          />
        );
        break;
      case 'bool':
        itemList.push(
          <FormItem
            name={name}
            label={comment}
            key={name}
            valuePropName="checked"
            getValueFromEvent={(value) => {
              return value === true ? 'true' : 'false';
            }}
            getValueProps={(value) => {
              return {
                checked: value === 'true',
              };
            }}
          >
            <Switch />
          </FormItem>,
        );
        return itemList;
      case 'list': {
        const { candidateValues = [] } = item;
        const options = candidateValues.map((item: string | { label: string; value: string }) => {
          if (isString(item)) {
            return { label: item, value: item };
          }
          if (item?.label) {
            return item;
          }
          return { label: item, value: item };
        });
        defaultItem = (
          <Select style={{ width: '100%' }} options={options} placeholder={placeholder} />
        );
        break;
      }
      default:
        defaultItem = <Input placeholder={placeholder} />;
        break;
    }

    itemList.push(
      <FormItem
        name={name}
        key={name}
        rules={[{ required: !!require, message: `请输入${comment}` }]}
        label={<FormItemTitle title={comment} subTitle={description} />}
      >
        {defaultItem}
      </FormItem>,
    );
    return itemList;
  }, []);
  return [...list];
};

export const wrapperTransTypeAndId = (exTransType: TransType, id: number) => {
  return `${exTransType}-${id}`;
};

export const toDomainList = (domainId: number, menuKey: string) => {
  history.push(`/model/domain/${domainId}/${menuKey}`);
};

export const toModelList = (domainId: number, modelId: number, menuKey?: string) => {
  history.push(`/model/domain/manager/${domainId}/${modelId}${menuKey ? `/${menuKey}` : ''}`);
};

export const toMetricEditPage = (
  domainId: number,
  modelId: number,
  metircId: number,
  menuKey?: string,
) => {
  history.push(`/model/metric/${domainId}/${modelId}/${metircId}${menuKey ? `/${menuKey}` : ''}`);
};

export const toDatasetEditPage = (domainId: number, datasetId: number, menuKey?: string) => {
  history.push(`/model/dataset/${domainId}/${datasetId}${menuKey ? `/${menuKey}` : ''}`);
};

export const toDimensionEditPage = (
  domainId: number,
  modelId: number,
  dimensionId: number,
  menuKey?: string,
) => {
  history.push(
    `/model/dimension/${domainId}/${modelId}/${dimensionId}${menuKey ? `/${menuKey}` : ''}`,
  );
};
