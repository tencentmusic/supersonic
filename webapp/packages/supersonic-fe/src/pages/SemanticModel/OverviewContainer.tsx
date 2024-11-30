import React, { useEffect, useState } from 'react';
import { history, useParams, useModel, Outlet } from '@umijs/max';
import DomainListTree from './components/DomainList';
import styles from './components/style.less';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { ISemantic } from './data';

type Props = {};

const OverviewContainer: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const domainId = params.domainId;
  const modelId = params.modelId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const databaseModel = useModel('SemanticModel.databaseData');
  const { setSelectDomain, setDomainList, selectDomainId } = domainModel;
  const { setSelectModel, setModelTableHistoryParams, MrefreshModelList } = modelModel;
  const { MrefreshDatabaseList } = databaseModel;
  const menuKey = params.menuKey ? params.menuKey : !Number(modelId) ? defaultTabKey : '';
  const [collapsedState, setCollapsedState] = useState(true);

  useEffect(() => {
    if (!selectDomainId || `${domainId}` === `${selectDomainId}`) {
      return;
    }
    pushUrlMenu(selectDomainId, menuKey);
  }, [selectDomainId]);

  // const initSelectedDomain = (domainList: ISemantic.IDomainItem[]) => {
  //   const targetNode = domainList.filter((item: any) => {
  //     return `${item.id}` === domainId;
  //   })[0];
  //   if (!targetNode) {
  //     const firstRootNode = domainList.filter((item: any) => {
  //       return item.parentId === 0;
  //     })[0];
  //     if (firstRootNode) {
  //       const { id } = firstRootNode;
  //       setSelectDomain(firstRootNode);
  //       pushUrlMenu(id, menuKey);
  //     }
  //   } else {
  //     setSelectDomain(targetNode);
  //   }
  // };

  // const initProjectTree = async () => {
  //   const { code, data, msg } = await getDomainList();
  //   if (code === 200) {
  //     initSelectedDomain(data);
  //     setDomainList(data);
  //   } else {
  //     message.error(msg);
  //   }
  // };

  // useEffect(() => {
  //   initProjectTree();
  //   MrefreshDatabaseList();
  //   return () => {
  //     setSelectDomain(undefined);
  //   };
  // }, []);

  const pushUrlMenu = (domainId: number, menuKey: string) => {
    history.push(`/model/domain/${domainId}/${menuKey}`);
  };

  const cleanModelInfo = (domainId) => {
    pushUrlMenu(domainId, defaultTabKey);
    setSelectModel(undefined);
  };

  const handleCollapsedBtn = () => {
    setCollapsedState(!collapsedState);
  };

  useEffect(() => {
    if (!selectDomainId) {
      return;
    }
    queryModelList();
  }, [selectDomainId]);

  const queryModelList = async () => {
    await MrefreshModelList(selectDomainId);
  };

  return (
    <div className={styles.projectBody}>
      <div className={styles.projectManger}>
        <div className={`${styles.sider} ${!collapsedState ? styles.siderCollapsed : ''}`}>
          <div className={styles.treeContainer}>
            <DomainListTree
              onTreeSelected={(domainData: ISemantic.IDomainItem) => {
                const { id } = domainData;
                cleanModelInfo(id);
                setSelectDomain(domainData);
                setModelTableHistoryParams({
                  [id]: {},
                });
              }}
              // onTreeDataUpdate={() => {
              //   // initProjectTree();
              // }}
            />
          </div>

          <div
            className={styles.siderCollapsedButton}
            onClick={() => {
              handleCollapsedBtn();
            }}
          >
            {collapsedState ? <LeftOutlined /> : <RightOutlined />}
          </div>
        </div>
        <div className={styles.content}>
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default OverviewContainer;
