import { message } from 'antd';
// import { InfoCircleOutlined } from '@ant-design/icons';
import React, { forwardRef, useImperativeHandle, useState, useEffect } from 'react';
import type { Ref } from 'react';
import DimensionMetricTransferModal from './DimensionMetricTransferModal';
// import styles from '../../components/style.less';
import { TransType } from '../../enum';
import { getDimensionList, queryMetric } from '../../service';
import { wrapperTransTypeAndId } from '../../components/Entity/utils';
import { ISemantic } from '../../data';
import { isArrayOfValues } from '@/utils/utils';

type Props = {
  // modelList: ISemantic.IModelItem[];
  viewItem: ISemantic.IViewItem;
  modelItem?: ISemantic.IModelItem;
  [key: string]: any;
};
const ViewModelConfigTransfer: React.FC<Props> = forwardRef(
  ({ viewItem, modelItem }: Props, ref: Ref<any>) => {
    // const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const [selectedTransferKeys, setSelectedTransferKeys] = useState<React.Key[]>([]);

    const [viewModelConfigsMap, setViewModelConfigsMap] = useState({});

    const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>([]);
    const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>([]);

    useImperativeHandle(ref, () => ({
      getViewModelConfigs: () => {
        return viewModelConfigsMap;
      },
    }));

    const queryDimensionListByIds = async (ids: number[]) => {
      if (!isArrayOfValues(ids)) {
        queryDimensionList([]);
        return;
      }
      const { code, data, msg } = await getDimensionList({ ids });
      if (code === 200 && Array.isArray(data?.list)) {
        queryDimensionList(data.list);
      } else {
        message.error(msg);
      }
    };

    const queryMetricListByIds = async (ids: number[]) => {
      if (!isArrayOfValues(ids)) {
        queryMetricList([]);
        return;
      }
      const { code, data, msg } = await queryMetric({ ids });
      if (code === 200 && Array.isArray(data?.list)) {
        queryMetricList(data.list);
      } else {
        message.error(msg);
      }
    };

    const queryDimensionList = async (selectedDimensionList: ISemantic.IDimensionItem[]) => {
      const { code, data, msg } = await getDimensionList({ modelId: modelItem?.id });
      if (code === 200 && Array.isArray(data?.list)) {
        const mergeList = selectedDimensionList.reduce(
          (modelDimensionList: ISemantic.IDimensionItem[], item) => {
            const hasItem = data.list.find((dataListItem: ISemantic.IDimensionItem) => {
              return dataListItem.id === item.id;
            });
            if (!hasItem) {
              return [item, ...modelDimensionList];
            }
            return modelDimensionList;
          },
          data.list,
        );
        setDimensionList(mergeList);
      } else {
        message.error(msg);
      }
    };

    const queryMetricList = async (selectedMetricList: ISemantic.IMetricItem[]) => {
      const { code, data, msg } = await queryMetric({ modelId: modelItem?.id });
      if (code === 200 && Array.isArray(data?.list)) {
        const mergeList = selectedMetricList.reduce(
          (modelMetricList: ISemantic.IMetricItem[], item) => {
            const hasItem = data.list.find((dataListItem: ISemantic.IMetricItem) => {
              return dataListItem.id === item.id;
            });
            if (!hasItem) {
              return [item, ...modelMetricList];
            }
            return modelMetricList;
          },
          data.list,
        );
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
        // setSelectedRowKeys(idList);
        setViewModelConfigsMap(viewConfigMap);
      }
    }, []);

    useEffect(() => {
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
        queryDimensionList([]);
        queryMetricList([]);
      }
    }, [modelItem]);

    return (
      <>
        <DimensionMetricTransferModal
          modelId={modelItem?.id}
          dimensionList={dimensionList}
          metricList={metricList}
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
