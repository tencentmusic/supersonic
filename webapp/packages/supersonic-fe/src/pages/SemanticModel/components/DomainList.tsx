import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { Input, message, Popconfirm, Tooltip, Row, Col, Button, Menu, MenuProps } from 'antd';
import type { DataNode } from 'antd/lib/tree';
import { useEffect, useState } from 'react';
import type { FC } from 'react';
import { useModel, history } from '@umijs/max';
import { createDomain, updateDomain, deleteDomain } from '../service';
import DomainInfoForm from './DomainInfoForm';
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
  const [projectInfoModalVisible, setProjectInfoModalVisible] = useState<boolean>(false);
  const [domainInfoParams, setDomainInfoParams] = useState<any>({});
  const [filterValue, setFliterValue] = useState<string>('');
  const [classList, setClassList] = useState<ISemantic.IDomainItem[]>([]);
  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId, domainList } = domainModel;

  useEffect(() => {
    setClassList(domainList);
  }, [domainList]);

  const onSearch = (value: any) => {
    setFliterValue(value);
  };

  const handleSelect = (selectedKeys: string) => {
    if (`${selectedKeys}` === `${selectDomainId}`) {
      return;
    }
    const targetNodeData = classList.filter((item: any) => {
      return `${item.id}` === `${selectedKeys}`;
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
      window.location.reload();
    } else {
      message.error(res.msg);
    }
  };

  const domainSubmit = async (values: any) => {
    if (values.modelType === 'add') {
      const { code, data, msg } = await createDomain(values);
      if (code === 200) {
        history.push(`/model/domain/${data.id}`);
        window.location.reload();
      } else {
        message.error(msg);
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
      window.location.reload();
    } else {
      message.error(res.msg);
    }
  };

  const titleRender = (node: any) => {
    const { id, name, path, hasEditPermission, parentId, hasModel } = node as any;
    const type = parentId === 0 ? 'top' : 'normal';
    return (
      <div className={styles.projectItem}>
        <span className={styles.projectItemTitle}>{name}</span>
        {createDomainBtnVisible && hasEditPermission && (
          <span className={`${styles.operation}  ${styles.rowHover} `}>
            {Array.isArray(path) && path.length < 2 && !hasModel && (
              <PlusOutlined
                className={styles.icon}
                onClick={(e) => {
                  setDomainInfoParams({
                    modelType: 'add',
                    type: 'normal',
                    parentId: id,
                    parentName: name,
                  });
                  setProjectInfoModalVisible(true);
                  e.stopPropagation();
                }}
              />
            )}

            <EditOutlined
              className={styles.icon}
              onClick={(e) => {
                setDomainInfoParams({
                  modelType: 'edit',
                  type,
                  ...node,
                });
                setProjectInfoModalVisible(true);
                e.stopPropagation();
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
              <DeleteOutlined
                className={styles.icon}
                onClick={(e) => {
                  e.stopPropagation();
                }}
              />
            </Popconfirm>
          </span>
        )}
      </div>
    );
  };

  const items = domainList
    .filter((domain) => {
      if (filterValue) {
        return domain.parentId === 0 && domain.name.includes(filterValue);
      } else {
        return domain.parentId === 0;
      }
    })
    .map((domain: ISemantic.IDomainItem) => {
      return {
        key: domain.id,
        label: titleRender(domain),
      };
    });

  const getLevelKeys = (items1: any[]) => {
    const key: Record<string, number> = {};
    const func = (items2: any[], level = 1) => {
      items2.forEach((item) => {
        if (item.key) {
          key[item.key] = level;
        }
        if (item.children) {
          func(item.children, level + 1);
        }
      });
    };
    func(items1);
    return key;
  };
  const levelKeys = getLevelKeys(items as any[]);
  const [stateOpenKeys, setStateOpenKeys] = useState(['2', '23']);

  const onOpenChange: MenuProps['onOpenChange'] = (openKeys) => {
    const currentOpenKey = openKeys.find((key) => stateOpenKeys.indexOf(key) === -1);
    // open
    if (currentOpenKey !== undefined) {
      const repeatIndex = openKeys
        .filter((key) => key !== currentOpenKey)
        .findIndex((key) => levelKeys[key] === levelKeys[currentOpenKey]);

      setStateOpenKeys(
        openKeys
          // remove repeat key
          .filter((_, index) => index !== repeatIndex)
          // remove current level all child
          .filter((key) => levelKeys[key] <= levelKeys[currentOpenKey]),
      );
    } else {
      // close
      setStateOpenKeys(openKeys);
    }
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
              onChange={(e) => {
                const value = e.target.value;
                if (!value) {
                  setFliterValue(value);
                }
              }}
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
      {selectDomainId && (
        <Menu
          mode="inline"
          defaultSelectedKeys={[`${selectDomainId}`]}
          openKeys={stateOpenKeys}
          onOpenChange={onOpenChange}
          style={{ width: 256 }}
          items={items}
          onClick={(info) => {
            handleSelect(info.key);
          }}
        />
      )}
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
