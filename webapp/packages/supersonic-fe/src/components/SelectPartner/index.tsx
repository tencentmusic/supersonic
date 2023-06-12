import { Avatar, TreeSelect, Tag } from 'antd';
import React, { useEffect, useState } from 'react';
import { getDepartmentTree, getUserByDeptid } from './service';
import TMEAvatar from '@/components/TMEAvatar';

type Props = {
  type: 'selectedPerson' | 'selectedDepartment';
  value?: any;
  onChange?: (value: boolean) => void;
  treeSelectProps?: Record<string, any>;
};

const isDisableCheckbox = (name: string, type: string) => {
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
export function changeTreeData(treeData: any = [], type: string) {
  return treeData.map((item: any) => {
    return {
      title: item.name,
      value: item.key,
      key: item.key,
      isLeaf: !!item.emplid,
      children: item?.subDepartments ? changeTreeData(item.subDepartments, type) : [],
      disableCheckbox: isDisableCheckbox(item.name, type),
      checkable: !isDisableCheckbox(item.name, type),
      icon: item.name.includes('(') && (
        <Avatar size={18} shape="square" src={`${item.avatarImg}`} alt="avatar" />
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
    const res = await getDepartmentTree();
    const data = changeTreeData(res.data, type);
    setTreeData(data);
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
      const childData = await getUserByDeptid(key);
      if (childData.data.length === 0) {
        return;
      }
      setTimeout(() => {
        setTreeData((origin) => updateTreeData(origin, key, changeTreeData(childData.data, type)));
      }, 300);
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
