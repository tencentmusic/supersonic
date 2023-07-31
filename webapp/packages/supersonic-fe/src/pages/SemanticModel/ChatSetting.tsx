import { Tabs, Popover, message } from 'antd';
import React, { useEffect, useState } from 'react';
import { connect, Helmet, useParams, history } from 'umi';
import ProjectListTree from './components/ProjectList';
import styles from './components/style.less';
import type { StateType } from './model';
import { DownOutlined } from '@ant-design/icons';
import EntitySection from './components/Entity/EntitySection';
import RecommendedQuestionsSection from './components/Entity/RecommendedQuestionsSection';
import { ISemantic } from './data';
import { getDomainList } from './service';
import OverView from './components/OverView';
import { findLeafNodesFromDomainList } from './utils';
import { ChatConfigType } from './enum';
import type { Dispatch } from 'umi';

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const ChatSetting: React.FC<Props> = ({ domainManger, dispatch }) => {
  window.RUNNING_ENV = 'chat';
  const defaultTabKey = 'metric';
  const params: any = useParams();
  const menuKey = params.menuKey ? params.menuKey : defaultTabKey;
  const modelId = params.modelId;
  const { selectDomainId, selectDomainName, domainList } = domainManger;
  const [modelList, setModelList] = useState<ISemantic.IDomainItem[]>([]);
  const [open, setOpen] = useState(false);
  const [isModel, setIsModel] = useState<boolean>(false);
  const [activeKey, setActiveKey] = useState<string>(menuKey);

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
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
      pushUrlMenu(selectDomainId, menuKey);
    }
  }, [selectDomainId]);

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

  const pushUrlMenu = (domainId: number, menuKey: string) => {
    history.push(`/chatSetting/${domainId}/${menuKey}`);
  };

  const tabItem = [
    {
      label: '子主题域',
      key: 'overview',
      children: <OverView modelList={modelList} />,
    },
  ];

  const isModelItem = [
    {
      label: '指标模式',
      key: 'metric',
      children: <EntitySection chatConfigType={ChatConfigType.AGG} />,
    },
    {
      label: '实体模式',
      key: 'dimenstion',
      children: <EntitySection chatConfigType={ChatConfigType.DETAIL} />,
    },
    {
      label: '推荐问题',
      key: 'recommendedQuestions',
      children: <RecommendedQuestionsSection />,
    },
  ];

  return (
    <div className={styles.projectBody}>
      <Helmet title={'问答设置-超音数'} />
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
                createDomainBtnVisible={false}
                onTreeSelected={() => {
                  setOpen(false);
                }}
              />
            }
            trigger="click"
            open={open}
            onOpenChange={handleOpenChange}
          >
            <div className={styles.domainSelector}>
              <span className={styles.domainTitle}>
                {selectDomainName ? `选择的主题域：${selectDomainName}` : '主题域信息'}
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
              activeKey={activeKey}
              destroyInactiveTabPane
              onChange={(menuKey: string) => {
                setActiveKey(menuKey);
                pushUrlMenu(selectDomainId, menuKey);
              }}
              items={!isModel ? tabItem : isModelItem}
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
}))(ChatSetting);
