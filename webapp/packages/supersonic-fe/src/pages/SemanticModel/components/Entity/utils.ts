import { IChatConfig, ISemantic } from '../../data';

export const exChangeRichEntityListToIds = (entityData: IChatConfig.IEntity) => {
  const entityList = entityData.entityIds || [];
  const detailData: {
    dimensionIds: number[];
    metricIds: number[];
  } = { dimensionIds: [], metricIds: [] };
  const { dimensionList, metricList } = entityData.entityInternalDetailDesc || {};
  if (Array.isArray(dimensionList)) {
    detailData.dimensionIds = dimensionList.map((item: ISemantic.IDimensionItem) => {
      return item.id;
    });
  }
  if (Array.isArray(metricList)) {
    detailData.metricIds = metricList.map((item: ISemantic.IMetricItem) => {
      return item.id;
    });
  }
  const entityIds = entityList.map((item) => {
    return item.id;
  });
  return {
    ...entityData,
    entityIds,
    detailData,
  };
};
