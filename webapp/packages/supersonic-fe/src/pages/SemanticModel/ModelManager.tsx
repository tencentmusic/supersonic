import React, { useEffect, useState } from 'react';
import { history, useParams, useModel } from '@umijs/max';
import ModelManagerTab from './components/ModelManagerTab';
import { toModelList } from '@/pages/SemanticModel/utils';

type Props = {};

const ModelManager: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const modelId = params.modelId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const dimensionModel = useModel('SemanticModel.dimensionData');
  const metricModel = useModel('SemanticModel.metricData');
  const { selectDomainId } = domainModel;
  const { selectModelId, modelList } = modelModel;
  const { MrefreshDimensionList } = dimensionModel;
  const { MrefreshMetricList } = metricModel;
  const menuKey = params.menuKey ? params.menuKey : !Number(modelId) ? defaultTabKey : '';
  const [activeKey, setActiveKey] = useState<string>(menuKey);

  const initModelConfig = () => {
    const currentMenuKey = menuKey === defaultTabKey ? '' : menuKey;
    toModelList(selectDomainId, selectModelId!, currentMenuKey);
    setActiveKey(currentMenuKey);
  };

  useEffect(() => {
    if (!selectModelId || `${selectModelId}` === `${modelId}`) {
      return;
    }
    initModelConfig();
    MrefreshDimensionList({ modelId: selectModelId });
    MrefreshMetricList({ modelId: selectModelId });
  }, [selectModelId]);

  return (
    <ModelManagerTab
      activeKey={activeKey}
      modelList={modelList}
      onMenuChange={(menuKey) => {
        setActiveKey(menuKey);
        toModelList(selectDomainId, selectModelId!, menuKey);
      }}
    />
  );
};

export default ModelManager;
