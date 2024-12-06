import React, { useEffect, useState } from 'react';
import { useParams, useModel, Outlet } from '@umijs/max';
import DomainListTree from './components/DomainList';
import styles from './components/style.less';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { ISemantic } from './data';
import { toDomainList } from '@/pages/SemanticModel/utils';

type Props = {};

const OverviewContainer: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const domainId = params.domainId;
  const modelId = params.modelId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const { setSelectDomain, selectDomainId } = domainModel;
  const { setSelectModel, setModelTableHistoryParams, MrefreshModelList } = modelModel;
  const menuKey = params.menuKey ? params.menuKey : !Number(modelId) ? defaultTabKey : '';
  const [collapsedState, setCollapsedState] = useState(true);

  useEffect(() => {
    if (!selectDomainId || `${domainId}` === `${selectDomainId}`) {
      return;
    }
    toDomainList(selectDomainId, menuKey);
  }, [selectDomainId]);

  const cleanModelInfo = (domainId: number) => {
    toDomainList(domainId, defaultTabKey);
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
