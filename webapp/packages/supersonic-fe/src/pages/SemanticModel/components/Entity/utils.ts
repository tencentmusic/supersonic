import { IChatConfig, ISemantic } from '../../data';
import { TransType } from '../../enum';

type FormatResult = IChatConfig.IChatRichConfig & {
  chatDefaultConfig: {
    dimensionIds: number[];
    metricIds: number[];
  };
};
export const formatRichEntityDataListToIds = (
  entityData: IChatConfig.IChatRichConfig,
): FormatResult => {
  if (!entityData?.chatDefaultConfig) {
    return {} as FormatResult;
  }
  const { chatDefaultConfig, entity } = entityData;

  const detailData: {
    dimensionIds: number[];
    metricIds: number[];
    ratioMetricIds: number[];
  } = { dimensionIds: [], metricIds: [], ratioMetricIds: [] };
  const { dimensions, metrics, ratioMetrics } = chatDefaultConfig || {};
  if (Array.isArray(dimensions)) {
    detailData.dimensionIds = dimensions.map((item: ISemantic.IDimensionItem) => {
      return item.id;
    });
  }
  if (Array.isArray(metrics)) {
    detailData.metricIds = metrics.map((item: ISemantic.IMetricItem) => {
      return item.id;
    });
  }
  if (Array.isArray(ratioMetrics)) {
    detailData.ratioMetricIds = ratioMetrics.map((item: ISemantic.IMetricItem) => {
      return item.id;
    });
  }

  let entitySetting = {};
  if (entity) {
    const entityItem = entity.dimItem;
    const entityId = entityItem?.id || null;
    const names = entity.names || [];
    entitySetting = {
      entity: {
        ...entity,
        entityId,
        name: names.join(','),
      },
    };
  }

  return {
    ...entityData,
    ...entitySetting,
    chatDefaultConfig: {
      ...chatDefaultConfig,
      ...detailData,
    },
  };
};

export const wrapperTransTypeAndId = (exTransType: TransType, id: number) => {
  return `${exTransType}-${id}`;
};

export const splitListToTransTypeId = (dataItemIds: string[]) => {
  const idListMap = dataItemIds.reduce(
    (
      idMap: {
        dimensionIds: number[];
        metricIds: number[];
      },
      item: string,
    ) => {
      const [transType, id] = item.split('-');
      if (id) {
        if (transType === TransType.DIMENSION) {
          idMap.dimensionIds.push(Number(id));
        }
        if (transType === TransType.METRIC) {
          idMap.metricIds.push(Number(id));
        }
      }
      return idMap;
    },
    {
      dimensionIds: [],
      metricIds: [],
    },
  );
  return idListMap;
};
