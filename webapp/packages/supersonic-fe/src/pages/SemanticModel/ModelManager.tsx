import React, { useEffect, useState } from 'react';
import { history, useParams, useModel } from '@umijs/max';
import ModelManagerTab from './components/ModelManagerTab';

type Props = {};

const OverviewContainer: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const domainId = params.domainId;
  const modelId = params.modelId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const dimensionModel = useModel('SemanticModel.dimensionData');
  const metricModel = useModel('SemanticModel.metricData');
  const databaseModel = useModel('SemanticModel.databaseData');
  const { selectDomainId, domainList, setSelectDomain, setDomainList } = domainModel;
  const {
    selectModelId,
    modelList,
    MrefreshModelList,
    setSelectModel,
    setModelTableHistoryParams,
  } = modelModel;
  const { MrefreshDimensionList } = dimensionModel;
  const { MrefreshMetricList } = metricModel;
  const { MrefreshDatabaseList } = databaseModel;
  const menuKey = params.menuKey ? params.menuKey : !Number(modelId) ? defaultTabKey : '';
  const [activeKey, setActiveKey] = useState<string>(menuKey);

  const initModelConfig = () => {
    const currentMenuKey = menuKey === defaultTabKey ? '' : menuKey;
    pushUrlMenu(selectDomainId, selectModelId, currentMenuKey);
    setActiveKey(currentMenuKey);
  };

  useEffect(() => {
    if (!selectModelId) {
      return;
    }
    initModelConfig();
    MrefreshDimensionList({ modelId: selectModelId });
    MrefreshMetricList({ modelId: selectModelId });
  }, [selectModelId]);

  const pushUrlMenu = (domainId: number, modelId: number, menuKey: string) => {
    history.push(`/model/manager/${domainId}/${modelId}/${menuKey}`);
  };

  const cleanModelInfo = (domainId) => {
    setActiveKey(defaultTabKey);
    pushUrlMenu(domainId, 0, defaultTabKey);
    setSelectModel(undefined);
  };

  return (
    <ModelManagerTab
      activeKey={activeKey}
      modelList={modelList}
      onBackDomainBtnClick={() => {
        cleanModelInfo(selectDomainId);
      }}
      onMenuChange={(menuKey) => {
        setActiveKey(menuKey);
        pushUrlMenu(selectDomainId, selectModelId, menuKey);
      }}
    />
  );
};

export default OverviewContainer;
