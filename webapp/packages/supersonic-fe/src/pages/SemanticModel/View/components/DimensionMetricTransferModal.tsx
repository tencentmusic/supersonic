import React, { useState, useEffect } from 'react';

import { ISemantic } from '../../data';

import { TransType } from '../../enum';
import DimensionMetricVisibleTransfer from '../../components/Entity/DimensionMetricVisibleTransfer';
import { wrapperTransTypeAndId } from '../../utils';

export type ModelCreateFormModalProps = {
  dimensionList?: ISemantic.IDimensionItem[];
  metricList?: ISemantic.IMetricItem[];
  modelId?: number;
  selectedTransferKeys: React.Key[];
  toolbarSolt?: React.ReactNode;
  onCancel: () => void;
  onSubmit: (values: any, selectedKeys: React.Key[]) => void;
};

const DimensionMetricTransferModal: React.FC<ModelCreateFormModalProps> = ({
  modelId,
  toolbarSolt,
  selectedTransferKeys,
  metricList,
  dimensionList,
  onSubmit,
}) => {
  const [sourceList, setSourceList] = useState<any[]>([]);
  const [selectedItemList, setSelectedItemList] = useState<any[]>([]);

  const addItemKey = (item: any, transType: TransType) => {
    const { id } = item;
    const key = wrapperTransTypeAndId(transType, id);
    return {
      ...item,
      transType,
      key,
    };
  };

  useEffect(() => {
    if (!dimensionList || !metricList) {
      return;
    }
    const sourceDimensionList = dimensionList.reduce((mergeList: any[], item) => {
      mergeList.push(addItemKey(item, TransType.DIMENSION));
      return mergeList;
    }, []);

    const hasDimensionList = selectedItemList
      .filter((item) => {
        return item.typeEnum === TransType.DIMENSION;
      })
      .reduce((modelDimensionList: ISemantic.IDimensionItem[], item) => {
        const hasItem = sourceDimensionList.find((dataListItem: ISemantic.IDimensionItem) => {
          return dataListItem.id === item.id;
        });
        if (!hasItem) {
          modelDimensionList.push(addItemKey(item, TransType.DIMENSION));
        }
        return modelDimensionList;
      }, []);

    const sourceMetricList = metricList.reduce((mergeList: any[], item) => {
      mergeList.push(addItemKey(item, TransType.METRIC));
      return mergeList;
    }, []);

    const hasMetricList = selectedItemList
      .filter((item) => {
        return item.typeEnum === TransType.METRIC;
      })
      .reduce((modelMetricList: ISemantic.IMetricItem[], item) => {
        const hasItem = sourceMetricList.find((dataListItem: ISemantic.IMetricItem) => {
          return dataListItem.id === item.id;
        });
        if (!hasItem) {
          modelMetricList.push(addItemKey(item, TransType.METRIC));
        }
        return modelMetricList;
      }, []);

    setSourceList([
      ...sourceDimensionList,
      ...sourceMetricList,
      ...hasDimensionList,
      ...hasMetricList,
    ]);
  }, [dimensionList, metricList]);

  return (
    <DimensionMetricVisibleTransfer
      titles={[<>{toolbarSolt}</>, '已加入维度/指标']}
      listStyle={{
        width: 520,
        height: 600,
      }}
      targetList={selectedTransferKeys}
      sourceList={sourceList}
      onChange={(newTargetKeys: string[]) => {
        const removeDimensionList: ISemantic.IDimensionItem[] = [];
        const removeMetricList: ISemantic.IMetricItem[] = [];
        const dimensionItemChangeList = Array.isArray(dimensionList)
          ? dimensionList.reduce((dimensionChangeList: any[], item: any) => {
              if (newTargetKeys.includes(wrapperTransTypeAndId(TransType.DIMENSION, item.id))) {
                dimensionChangeList.push(item);
              } else {
                removeDimensionList.push(item.id);
              }
              return dimensionChangeList;
            }, [])
          : [];

        const metricItemChangeList = Array.isArray(metricList)
          ? metricList.reduce((metricChangeList: any[], item: any) => {
              if (newTargetKeys.includes(wrapperTransTypeAndId(TransType.METRIC, item.id))) {
                metricChangeList.push(item);
              } else {
                removeMetricList.push(item.id);
              }
              return metricChangeList;
            }, [])
          : [];

        setSelectedItemList([...dimensionItemChangeList, ...metricItemChangeList]);

        // 如果不是当前选中model中的指标或者维度，则先从本地数据中删除，避免后续请求数据更新时产生视觉上的界面闪烁
        const preUpdateSourceData = sourceList.filter((item) => {
          const { typeEnum, id } = item;
          if (typeEnum === TransType.DIMENSION) {
            if (modelId !== item.modelId && removeDimensionList.includes(id)) {
              return false;
            }
          }
          if (typeEnum === TransType.METRIC) {
            if (modelId !== item.modelId && removeMetricList.includes(id)) {
              return false;
            }
          }
          return true;
        });
        setSourceList([...preUpdateSourceData]);

        const dataSetModelConfigs = [...dimensionItemChangeList, ...metricItemChangeList].reduce(
          (config, item) => {
            const { modelId, id, typeEnum } = item;
            if (config[modelId]) {
              if (typeEnum === TransType.DIMENSION) {
                config[modelId].dimensions.push(id);
              }
              if (typeEnum === TransType.METRIC) {
                config[modelId].metrics.push(id);
              }
            } else {
              config[modelId] = {
                id: modelId,
                metrics: typeEnum === TransType.METRIC ? [id] : [],
                dimensions: typeEnum === TransType.DIMENSION ? [id] : [],
              };
            }
            return config;
          },
          {},
        );

        onSubmit?.(dataSetModelConfigs, newTargetKeys);
      }}
    />
  );
};

export default DimensionMetricTransferModal;
