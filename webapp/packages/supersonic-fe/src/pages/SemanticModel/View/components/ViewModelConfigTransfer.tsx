import { message } from 'antd';
import React, { forwardRef, useImperativeHandle, useState, useEffect } from 'react';
import type { ReactNode, Ref } from 'react';
import DimensionMetricTransferModal from './DimensionMetricTransferModal';
import { TransType } from '../../enum';
import { getDimensionList, queryMetric } from '../../service';
import { wrapperTransTypeAndId } from '../../utils';
import { ISemantic } from '../../data';
import { isArrayOfValues } from '@/utils/utils';

type Props = {
  viewItem: ISemantic.IViewItem;
  modelItem?: ISemantic.IModelItem;
  dimensionList?: ISemantic.IDimensionItem[];
  metricList?: ISemantic.IMetricItem[];
  toolbarSolt?: ReactNode;
};
const ViewModelConfigTransfer: React.FC<Props> = forwardRef(
  ({ viewItem, modelItem, dimensionList, metricList, toolbarSolt }: Props, ref: Ref<any>) => {
    const [selectedTransferKeys, setSelectedTransferKeys] = useState<React.Key[]>([]);

    const [viewModelConfigsMap, setViewModelConfigsMap] = useState({});

    const [mergeDimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>();
    const [mergeMetricList, setMetricList] = useState<ISemantic.IMetricItem[]>();

    useImperativeHandle(ref, () => ({
      getViewModelConfigs: () => {
        return viewModelConfigsMap;
      },
    }));

    const queryDimensionListByIds = async (ids: number[]) => {
      if (!isArrayOfValues(ids)) {
        setDimensionList(dimensionList);
        return;
      }
      const { code, data, msg } = await getDimensionList({ ids });
      if (code === 200 && Array.isArray(data?.list)) {
        const mergeList = data?.list.reduce(
          (modelDimensionList: ISemantic.IDimensionItem[], item) => {
            const hasItem = Array.isArray(dimensionList)
              ? dimensionList.find((dataListItem: ISemantic.IDimensionItem) => {
                  return dataListItem.id === item.id;
                })
              : [];
            if (!hasItem) {
              return [item, ...modelDimensionList];
            }
            return modelDimensionList;
          },
          dimensionList,
        );
        setDimensionList(mergeList);
      } else {
        message.error(msg);
      }
    };

    const queryMetricListByIds = async (ids: number[]) => {
      if (!isArrayOfValues(ids)) {
        setMetricList(metricList);
        return;
      }
      const { code, data, msg } = await queryMetric({ ids });
      if (code === 200 && Array.isArray(data?.list)) {
        const mergeList = data.list.reduce((modelMetricList: ISemantic.IMetricItem[], item) => {
          const hasItem = Array.isArray(metricList)
            ? metricList.find((dataListItem: ISemantic.IMetricItem) => {
                return dataListItem.id === item.id;
              })
            : [];
          if (!hasItem) {
            return [item, ...modelMetricList];
          }
          return modelMetricList;
        }, metricList);
        setMetricList(mergeList);
      } else {
        message.error(msg);
      }
    };

    useEffect(() => {
      const viewModelConfigs = viewItem?.viewDetail?.viewModelConfigs;
      if (Array.isArray(viewModelConfigs)) {
        const idList: number[] = [];
        const transferKeys: React.Key[] = [];
        const viewConfigMap = {};
        const allMetrics: number[] = [];
        const allDimensions: number[] = [];
        viewModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
          const { id, metrics, dimensions } = item;
          idList.push(id);
          allMetrics.push(...metrics);
          allDimensions.push(...dimensions);
          viewConfigMap[id] = { ...item };
          if (Array.isArray(metrics)) {
            metrics.forEach((metricId: number) => {
              transferKeys.push(wrapperTransTypeAndId(TransType.METRIC, metricId));
            });
          }
          if (Array.isArray(dimensions)) {
            dimensions.forEach((dimensionId: number) => {
              transferKeys.push(wrapperTransTypeAndId(TransType.DIMENSION, dimensionId));
            });
          }
        });
        setSelectedTransferKeys(transferKeys);
        setViewModelConfigsMap(viewConfigMap);
      }
    }, []);

    useEffect(() => {
      if (!dimensionList || !metricList) {
        return;
      }
      const viewModelConfigs = isArrayOfValues(Object.values(viewModelConfigsMap))
        ? (Object.values(viewModelConfigsMap) as ISemantic.IViewModelConfigItem[])
        : viewItem?.viewDetail?.viewModelConfigs;
      if (isArrayOfValues(viewModelConfigs)) {
        const allMetrics: number[] = [];
        const allDimensions: number[] = [];
        viewModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
          const { metrics, dimensions } = item;
          allMetrics.push(...metrics);
          allDimensions.push(...dimensions);
        });
        queryDimensionListByIds(allDimensions);
        queryMetricListByIds(allMetrics);
      } else {
        setDimensionList(dimensionList);
        setMetricList(metricList);
      }
    }, [modelItem, dimensionList, metricList]);

    return (
      <>
        <DimensionMetricTransferModal
          toolbarSolt={toolbarSolt}
          modelId={modelItem?.id}
          dimensionList={mergeDimensionList}
          metricList={mergeMetricList}
          selectedTransferKeys={selectedTransferKeys}
          onSubmit={(
            submitData: Record<string, ISemantic.IViewModelConfigItem>,
            selectedKeys: React.Key[],
          ) => {
            const viewModelConfigs = Object.values(submitData) as ISemantic.IViewModelConfigItem[];

            if (isArrayOfValues(viewModelConfigs)) {
              const allMetrics: number[] = [];
              const allDimensions: number[] = [];
              viewModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
                const { metrics, dimensions } = item;
                allMetrics.push(...metrics);
                allDimensions.push(...dimensions);
              });
              queryDimensionListByIds(allDimensions);
              queryMetricListByIds(allMetrics);
            }
            setViewModelConfigsMap(submitData);
            setSelectedTransferKeys(selectedKeys);
          }}
          onCancel={() => {}}
        />
      </>
    );
  },
);

export default ViewModelConfigTransfer;
