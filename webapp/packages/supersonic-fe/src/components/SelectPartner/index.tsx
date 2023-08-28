import { TreeSelect, Tag } from 'antd';
import React, { useEffect, useState } from 'react';
import { getUserByDeptid, getOrganizationTree } from './service';
import TMEAvatar from '@/components/TMEAvatar';

type Props = {
  type: 'selectedPerson' | 'selectedDepartment';
  value?: any;
  onChange?: (value: boolean) => void;
  treeSelectProps?: Record<string, any>;
};

const isDisableCheckbox = (name: string, type: string) => {
  if (!name) {
    return false;
  }
  const isPersonNode = name.includes('(');
  if (type === 'selectedPerson') {
    return !isPersonNode;
  }
  if (type === 'selectedDepartment') {
    if (isPersonNode) {
      return true;
    }
    return false;
  }
  return true;
};

// 转化树结构
export function changeTreeData(treeData: any = [], type: string, keyName = 'id') {
  return treeData.map((item: any) => {
    return {
      title: item.name,
      value: item[keyName],
      key: item[keyName],
      isLeaf: !item.subOrganizations,
      children: item.subOrganizations ? changeTreeData(item.subOrganizations, type, keyName) : [],
      disableCheckbox: isDisableCheckbox(item.displayName, type),
      checkable: !isDisableCheckbox(item.displayName, type),
      icon: (item.displayName || '').includes('(') && (
        <TMEAvatar size="small" staffName={item.name} />
      ),
    };
  });
}

const SelectPartner: React.FC<Props> = ({
  type = 'selectedPerson',
  value,
  onChange,
  treeSelectProps = {},
}) => {
  const [treeData, setTreeData] = useState([]);

  const getDetpartment = async () => {
    const { code, data } = await getOrganizationTree();
    if (code === 200) {
      const changeData = changeTreeData(data, type);
      setTreeData(changeData);
      return;
    }
  };

  useEffect(() => {
    getDetpartment();
  }, []);

  const updateTreeData = (list: any, key: any, children: any) => {
    return list.map((node: any) => {
      if (node.key === key) {
        let childrenData = node.children;
        if (node.children && !node.children.find((item: any) => item?.key === children[0]?.key)) {
          childrenData = [...children, ...node.children];
        }
        return { ...node, children: childrenData };
      }
      if (node.children.length !== 0) {
        return { ...node, children: updateTreeData(node.children, key, children) };
      }
      return node;
    });
  };
  const onLoadData = (target: any) => {
    const { key } = target;
    const loadData = async () => {
      const { code, data } = await getUserByDeptid(key);
      if (code === 200) {
        const list = data.reduce((userList: any[], item: any) => {
          const { name, displayName } = item;
          if (name && displayName) {
            userList.push({ key: `${key}-${item.id}`, ...item });
          }
          return userList;
        }, []);
        setTimeout(() => {
          setTreeData((origin) => {
            return updateTreeData(origin, key, changeTreeData(list, type, 'key'));
          });
        }, 300);
      }
    };
    return new Promise<void>((resolve) => {
      loadData().then(() => {
        resolve();
      });
    });
  };

  const handleChange = (newValue: any) => {
    onChange?.(newValue);
  };
  const tagRender = (props: any) => {
    const { label } = props;
    const onPreventMouseDown = (event: React.MouseEvent<HTMLSpanElement>) => {
      event.preventDefault();
      event.stopPropagation();
    };
    const enEname = label.split('(')[0];
    return (
      <Tag
        onMouseDown={onPreventMouseDown}
        closable={true}
        onClose={() => {
          const { value: propsValue } = props;
          const newValue = value.filter((code: string) => {
            return code !== propsValue;
          });
          onChange?.(newValue);
        }}
        style={{ marginRight: 3, marginBottom: 3 }}
      >
        {type === 'selectedPerson' && <TMEAvatar size="small" staffName={enEname} />}
        <span
          style={{
            position: 'relative',
            top: '2px',
            left: '5px',
          }}
        >
          {label}
        </span>
      </Tag>
    );
  };
  return (
    <>
      <TreeSelect
        showSearch
        style={{ width: '100%' }}
        value={value}
        loadData={onLoadData}
        dropdownStyle={{ maxHeight: 800, overflow: 'auto' }}
        allowClear
        multiple
        onChange={handleChange}
        treeCheckable={true}
        treeIcon={true}
        treeData={treeData}
        tagRender={tagRender}
        treeNodeFilterProp={'title'}
        listHeight={500}
        showCheckedStrategy={TreeSelect.SHOW_PARENT}
        {...treeSelectProps}
      />
    </>
  );
};

export default SelectPartner;
