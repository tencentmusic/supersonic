import { DownOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { Input, message, Tree, Popconfirm, Space, Tooltip } from 'antd';
import type { DataNode } from 'antd/lib/tree';
import React, { useEffect, useState } from 'react';
import type { FC, Key } from 'react';
import { connect } from 'umi';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';
import { getDomainList, createDomain, updateDomain, deleteDomain } from '../service';
import { treeParentKeyLists } from '../utils';
import ProjectInfoFormProps from './ProjectInfoForm';
import { constructorClassTreeFromList, addPathInTreeData } from '../utils';
import { PlusCircleOutlined } from '@ant-design/icons';

import styles from './style.less';

const { Search } = Input;

type ProjectListProps = {
  selectDomainId: string;
  selectDomainName: string;
  createDomainBtnVisible?: boolean;
  dispatch: Dispatch;
};

const projectTreeFlat = (projectTree: DataNode[], filterValue: string): DataNode[] => {
  let newProjectTree: DataNode[] = [];
  projectTree.map((item) => {
    const { children, ...rest } = item;
    if (String(item.title).includes(filterValue)) {
      newProjectTree.push({ ...rest });
    }
    if (children && children.length > 0) {
      newProjectTree = newProjectTree.concat(projectTreeFlat(children, filterValue));
    }
    return item;
  });
  return newProjectTree;
};

const ProjectListTree: FC<ProjectListProps> = ({
  selectDomainId,
  createDomainBtnVisible = true,
  dispatch,
}) => {
  const [projectTree, setProjectTree] = useState<DataNode[]>([]);
  const [projectInfoModalVisible, setProjectInfoModalVisible] = useState<boolean>(false);
  const [projectInfoParams, setProjectInfoParams] = useState<any>({});
  const [filterValue, setFliterValue] = useState<string>('');
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [classList, setClassList] = useState<any[]>([]);

  const onSearch = (value: any) => {
    setFliterValue(value);
  };

  const initProjectTree = async () => {
    const { code, data, msg } = await getDomainList();
    if (code === 200) {
      const treeData = addPathInTreeData(constructorClassTreeFromList(data));
      setProjectTree(treeData);
      setClassList(data);
      setExpandedKeys(treeParentKeyLists(treeData));
      const firstRootNode = data.filter((item: any) => {
        return item.parentId === 0;
      })[0];
      if (firstRootNode) {
        const { id, name } = firstRootNode;
        dispatch({
          type: 'domainManger/setSelectDomain',
          selectDomainId: id,
          selectDomainName: name,
          domainData: firstRootNode,
        });
      }
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    initProjectTree();
  }, []);

  const handleSelect = (selectedKeys: string, projectName: string) => {
    if (selectedKeys === selectDomainId) {
      return;
    }
    const targetNodeData = classList.filter((item: any) => {
      return item.id === selectedKeys;
    })[0];
    dispatch({
      type: 'domainManger/setSelectDomain',
      selectDomainId: selectedKeys,
      selectDomainName: projectName,
      domainData: targetNodeData,
    });
  };

  const editProject = async (values: any) => {
    const params = { ...values };
    const res = await updateDomain(params);
    if (res.code === 200) {
      message.success('编辑分类成功');
      setProjectInfoModalVisible(false);
      initProjectTree();
    } else {
      message.error(res.msg);
    }
  };

  const projectSubmit = async (values: any) => {
    if (values.modelType === 'add') {
      await createDomain(values);
    } else if (values.modelType === 'edit') {
      await editProject(values);
    }
    initProjectTree();
    setProjectInfoModalVisible(false);
  };

  // 删除项目
  const confirmDelete = async (projectId: string) => {
    const res = await deleteDomain(projectId);
    if (res.code === 200) {
      message.success('编辑项目成功');
      setProjectInfoModalVisible(false);
      initProjectTree();
    } else {
      message.error(res.msg);
    }
  };

  const titleRender = (node: any) => {
    const { id, name, path } = node as any;
    return (
      <div className={styles.projectItem}>
        <span
          className={styles.title}
          onClick={() => {
            handleSelect(id, name);
          }}
        >
          {name}
        </span>
        {createDomainBtnVisible && (
          <span className={styles.operation}>
            {Array.isArray(path) && path.length < 3 && (
              <PlusOutlined
                className={styles.icon}
                onClick={() => {
                  setProjectInfoParams({
                    modelType: 'add',
                    type: 'normal',
                    parentId: id,
                    parentName: name,
                  });
                  setProjectInfoModalVisible(true);
                }}
              />
            )}

            <EditOutlined
              className={styles.icon}
              onClick={() => {
                setProjectInfoParams({
                  modelType: 'edit',
                  type: 'normal',
                  ...node,
                });
                setProjectInfoModalVisible(true);
              }}
            />
            <Popconfirm
              key="popconfirm"
              title={'确认删除吗?'}
              onConfirm={() => {
                confirmDelete(id);
              }}
              okText="是"
              cancelText="否"
            >
              <DeleteOutlined className={styles.icon} />
            </Popconfirm>
          </span>
        )}
      </div>
    );
  };

  const projectRenderTree = filterValue ? projectTreeFlat(projectTree, filterValue) : projectTree;

  const handleExpand = (_expandedKeys: Key[]) => {
    setExpandedKeys(_expandedKeys as string[]);
  };

  return (
    <div className={styles.projectList}>
      <h2 className={styles.treeTitle}>
        <span className={styles.title}>主题域</span>
        <Space>
          {createDomainBtnVisible && (
            <Tooltip title="新增顶级域">
              <PlusCircleOutlined
                onClick={() => {
                  setProjectInfoParams({ type: 'top', modelType: 'add' });
                  setProjectInfoModalVisible(true);
                }}
                className={styles.addBtn}
              />
            </Tooltip>
          )}
        </Space>
      </h2>
      <Search
        allowClear
        className={styles.search}
        placeholder="请输入主题域名称进行查询"
        onSearch={onSearch}
      />
      <Tree
        expandedKeys={expandedKeys}
        onExpand={handleExpand}
        className={styles.tree}
        selectedKeys={[selectDomainId]}
        blockNode={true}
        switcherIcon={<DownOutlined />}
        defaultExpandAll={true}
        treeData={projectRenderTree}
        titleRender={titleRender}
      />
      {projectInfoModalVisible && (
        <ProjectInfoFormProps
          basicInfo={projectInfoParams}
          onSubmit={projectSubmit}
          onCancel={() => {
            setProjectInfoModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default connect(
  ({ domainManger: { selectDomainId, selectDomainName } }: { domainManger: StateType }) => ({
    selectDomainId,
    selectDomainName,
  }),
)(ProjectListTree);
