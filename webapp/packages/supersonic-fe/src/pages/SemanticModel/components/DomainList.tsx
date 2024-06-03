import { DownOutlined, PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { Input, message, Tree, Popconfirm, Tooltip, Row, Col, Button } from 'antd';
import type { DataNode } from 'antd/lib/tree';
import { useEffect, useState } from 'react';
import type { FC, Key } from 'react';
import { useModel } from '@umijs/max';
import { createDomain, updateDomain, deleteDomain } from '../service';
import { treeParentKeyLists } from '../utils';
import DomainInfoForm from './DomainInfoForm';
import { constructorClassTreeFromList, addPathInTreeData } from '../utils';

import styles from './style.less';
import { ISemantic } from '../data';

const { Search } = Input;

type DomainListProps = {
  createDomainBtnVisible?: boolean;
  onCreateDomainBtnClick?: () => void;
  onTreeSelected?: (targetNodeData: ISemantic.IDomainItem) => void;
  onTreeDataUpdate?: () => void;
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

const DomainListTree: FC<DomainListProps> = ({
  createDomainBtnVisible = true,
  onCreateDomainBtnClick,
  onTreeSelected,
  onTreeDataUpdate,
}) => {
  const [projectTree, setProjectTree] = useState<DataNode[]>([]);
  const [projectInfoModalVisible, setProjectInfoModalVisible] = useState<boolean>(false);
  const [domainInfoParams, setDomainInfoParams] = useState<any>({});
  const [filterValue, setFliterValue] = useState<string>('');
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [classList, setClassList] = useState<ISemantic.IDomainItem[]>([]);
  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId, domainList } = domainModel;

  useEffect(() => {
    const treeData = addPathInTreeData(constructorClassTreeFromList(domainList));
    setProjectTree(treeData);
    setClassList(domainList);
    setExpandedKeys(treeParentKeyLists(treeData));
  }, [domainList]);

  const onSearch = (value: any) => {
    setFliterValue(value);
  };

  const handleSelect = (selectedKeys: string) => {
    if (`${selectedKeys}` === `${selectDomainId}`) {
      return;
    }
    const targetNodeData = classList.filter((item: any) => {
      return item.id === selectedKeys;
    })[0];
    onTreeSelected?.(targetNodeData);
  };

  const editProject = async (values: any) => {
    const params = { ...values };
    const res = await updateDomain(params);
    if (res.code === 200) {
      message.success('编辑分类成功');
      setProjectInfoModalVisible(false);
      onTreeDataUpdate?.();
    } else {
      message.error(res.msg);
    }
  };

  const createDefaultModelSet = async (domainId: number) => {
    const { code, msg } = await createDomain({
      modelType: 'add',
      type: 'normal',
      parentId: domainId,
      name: '默认模型集',
      bizName: `defaultModelSet_${(Math.random() * 1000000).toFixed(0)}`,
      isUnique: 1,
    });
    if (code !== 200) {
      message.error(msg);
    }
  };

  const domainSubmit = async (values: any) => {
    if (values.modelType === 'add') {
      const { code, data } = await createDomain(values);
      if (code === 200 && values.type === 'top') {
        await createDefaultModelSet(data.id);
      }
    } else if (values.modelType === 'edit') {
      await editProject(values);
    }
    onTreeDataUpdate?.();
    setProjectInfoModalVisible(false);
  };

  const confirmDelete = async (domainId: string) => {
    const res = await deleteDomain(domainId);
    if (res.code === 200) {
      message.success('删除成功');
      setProjectInfoModalVisible(false);
      onTreeDataUpdate?.();
    } else {
      message.error(res.msg);
    }
  };

  const titleRender = (node: any) => {
    const { id, name, path, hasEditPermission, parentId, hasModel } = node as any;
    const type = parentId === 0 ? 'top' : 'normal';
    return (
      <div className={styles.projectItem}>
        <span
          className={styles.projectItemTitle}
          onClick={() => {
            handleSelect(id);
          }}
        >
          {name}
        </span>
        {createDomainBtnVisible && hasEditPermission && (
          <span className={`${styles.operation} ${parentId ? styles.rowHover : ''}`}>
            {Array.isArray(path) && path.length < 2 && !hasModel && (
              <PlusOutlined
                className={styles.icon}
                onClick={() => {
                  setDomainInfoParams({
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
                setDomainInfoParams({
                  modelType: 'edit',
                  type,
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
    <div className={styles.domainList}>
      <div className={styles.searchContainer}>
        <Row style={{ gap: 10 }}>
          <Col flex="1 1 150px">
            <Search
              allowClear
              className={styles.search}
              placeholder="请输入名称搜索"
              onSearch={onSearch}
            />
          </Col>
          {createDomainBtnVisible && (
            <Col flex="0 0 45px" style={{ display: 'flex', alignItems: 'center' }}>
              <Tooltip title="新增主题域">
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  size="small"
                  onClick={() => {
                    setDomainInfoParams({ type: 'top', modelType: 'add' });
                    setProjectInfoModalVisible(true);
                    onCreateDomainBtnClick?.();
                  }}
                />
              </Tooltip>
            </Col>
          )}
        </Row>
      </div>
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
        <DomainInfoForm
          basicInfo={domainInfoParams}
          onSubmit={domainSubmit}
          onCancel={() => {
            setProjectInfoModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default DomainListTree;
