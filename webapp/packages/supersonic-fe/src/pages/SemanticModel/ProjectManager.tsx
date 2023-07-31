import { Tabs, Popover, message } from 'antd';
import React, { useEffect, useState } from 'react';
import { connect, Helmet, history, useParams } from 'umi';
import ProjectListTree from './components/ProjectList';
import ClassDataSourceTable from './components/ClassDataSourceTable';
import ClassDimensionTable from './components/ClassDimensionTable';
import ClassMetricTable from './components/ClassMetricTable';
import PermissionSection from './components/Permission/PermissionSection';
import DatabaseSection from './components/Database/DatabaseSection';
import EntitySettingSection from './components/Entity/EntitySettingSection';
import OverView from './components/OverView';
import styles from './components/style.less';
import type { StateType } from './model';
import { DownOutlined } from '@ant-design/icons';
import { ISemantic } from './data';
import { findLeafNodesFromDomainList } from './utils';
import SemanticGraphCanvas from './SemanticGraphCanvas';
import { getDomainList } from './service';
import type { Dispatch } from 'umi';

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const DomainManger: React.FC<Props> = ({ domainManger, dispatch }) => {
  window.RUNNING_ENV = 'semantic';
  const defaultTabKey = 'xflow';
  const params: any = useParams();
  const menuKey = params.menuKey ? params.menuKey : defaultTabKey;
  const modelId = params.modelId;
  const { selectDomainId, selectDomainName, domainList } = domainManger;
  const [modelList, setModelList] = useState<ISemantic.IDomainItem[]>([]);
  const [isModel, setIsModel] = useState<boolean>(false);
  const [open, setOpen] = useState(false);
  const [activeKey, setActiveKey] = useState<string>(menuKey);

  useEffect(() => {
    setActiveKey(menuKey);
  }, [menuKey]);

  const initSelectedDomain = (domainList: ISemantic.IDomainItem[]) => {
    const targetNode = domainList.filter((item: any) => {
      return `${item.id}` === modelId;
    })[0];
    if (!targetNode) {
      const firstRootNode = domainList.filter((item: any) => {
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
      const { id, name } = targetNode;
      dispatch({
        type: 'domainManger/setSelectDomain',
        selectDomainId: id,
        selectDomainName: name,
        domainData: targetNode,
      });
    }
  };

  const initProjectTree = async () => {
    const { code, data, msg } = await getDomainList();
    if (code === 200) {
      if (!selectDomainId) {
        initSelectedDomain(data);
      }
      dispatch({
        type: 'domainManger/setDomainList',
        payload: { domainList: data },
      });
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    initProjectTree();
  }, []);

  useEffect(() => {
    if (!selectDomainId) {
      return;
    }
    const list = findLeafNodesFromDomainList(domainList, selectDomainId);
    setModelList(list);
    if (Array.isArray(list) && list.length > 0) {
      setIsModel(false);
      pushUrlMenu(selectDomainId, 'overview');
      setActiveKey('overview');
    } else {
      setIsModel(true);
      const currentMenuKey = menuKey === 'overview' ? defaultTabKey : menuKey;
      pushUrlMenu(selectDomainId, currentMenuKey);
      setActiveKey(currentMenuKey);
    }
  }, [domainList, selectDomainId]);

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
  };

  const pushUrlMenu = (domainId: number, menuKey: string) => {
    history.push(`/semanticModel/${domainId}/${menuKey}`);
  };

  useEffect(() => {
    if (selectDomainId) {
      dispatch({
        type: 'domainManger/queryDimensionList',
        payload: {
          domainId: selectDomainId,
        },
      });
      dispatch({
        type: 'domainManger/queryMetricList',
        payload: {
          domainId: selectDomainId,
        },
      });
      dispatch({
        type: 'domainManger/queryDatabaseByDomainId',
        payload: {
          domainId: selectDomainId,
        },
      });
    }
  }, [selectDomainId]);

  const tabItem = [
    {
      label: '子主题域',
      key: 'overview',
      children: <OverView modelList={modelList} />,
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection />,
    },
  ];

  const isModelItem = [
    {
      label: '画布',
      key: 'xflow',
      children: (
        <div style={{ width: '100%', marginTop: -20 }}>
          <SemanticGraphCanvas />
        </div>
      ),
    },
    {
      label: '数据库',
      key: 'dataBase',
      children: <DatabaseSection />,
    },
    {
      label: '数据源',
      key: 'dataSource',
      children: <ClassDataSourceTable />,
    },
    {
      label: '维度',
      key: 'dimenstion',
      children: <ClassDimensionTable key={selectDomainId} />,
    },
    {
      label: '指标',
      key: 'metric',
      children: <ClassMetricTable />,
    },
    {
      label: '实体',
      key: 'entity',
      children: <EntitySettingSection />,
    },

    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection />,
    },
  ];

  return (
    <div className={styles.projectBody}>
      <Helmet title={'模型管理-超音数'} />
      <div className={styles.projectManger}>
        <h2 className={styles.title}>
          <Popover
            zIndex={1000}
            overlayInnerStyle={{
              overflow: 'scroll',
              maxHeight: '800px',
            }}
            content={
              <ProjectListTree
                onTreeSelected={() => {
                  setOpen(false);
                }}
                onTreeDataUpdate={() => {
                  initProjectTree();
                }}
              />
            }
            trigger="click"
            open={open}
            onOpenChange={handleOpenChange}
          >
            <div className={styles.domainSelector}>
              <span className={styles.domainTitle}>
                {selectDomainName ? `当前主题域：${selectDomainName}` : '主题域信息'}
              </span>
              <span className={styles.downIcon}>
                <DownOutlined />
              </span>
            </div>
          </Popover>
        </h2>
        {selectDomainId ? (
          <>
            <Tabs
              className={styles.tab}
              items={!isModel ? tabItem : isModelItem}
              activeKey={activeKey}
              destroyInactiveTabPane
              onChange={(menuKey: string) => {
                setActiveKey(menuKey);
                pushUrlMenu(selectDomainId, menuKey);
              }}
            />
          </>
        ) : (
          <h2 className={styles.mainTip}>请选择项目</h2>
        )}
      </div>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManger);
