import { message } from 'antd';
import React, { forwardRef, useImperativeHandle, useState, useEffect } from 'react';
import type { ReactNode, Ref } from 'react';
import DimensionMetricTransferModal from './DimensionMetricTransferModal';
import TagTransferModal from './TagTransferModal';
import { TransType } from '../../enum';
import { getDimensionList, queryMetric, getTagList } from '../../service';
import { wrapperTransTypeAndId } from '../../utils';
import { ISemantic } from '../../data';
import { isArrayOfValues } from '@/utils/utils';

type Props = {
  queryType?: string;
  viewItem: ISemantic.IViewItem;
  modelItem?: ISemantic.IModelItem;
  dimensionList?: ISemantic.IDimensionItem[];
  metricList?: ISemantic.IMetricItem[];
  tagList?: ISemantic.ITagItem[];
  toolbarSolt?: ReactNode;
};
const ViewModelConfigTransfer: React.FC<Props> = forwardRef(
  (
    {
      queryType = TransType.METRIC,
      viewItem,
      modelItem,
      dimensionList,
      metricList,
      tagList,
      toolbarSolt,
    }: Props,
    ref: Ref<any>,
  ) => {
    const [selectedTransferKeys, setSelectedTransferKeys] = useState<React.Key[]>([]);

    const [viewModelConfigsMap, setViewModelConfigsMap] = useState({});

    const [mergeDimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>();
    const [mergeMetricList, setMetricList] = useState<ISemantic.IMetricItem[]>();
    const [mergeTagList, setTagList] = useState<ISemantic.ITagItem[]>();

    useImperativeHandle(ref, () => ({
      getViewModelConfigs: () => {
        return viewModelConfigsMap;
      },
    }));

    const queryTagListByIds = async (ids: number[]) => {
      if (!isArrayOfValues(ids)) {
        setTagList(tagList);
        return;
      }
      const { code, data, msg } = await getTagList({ ids });
      if (code === 200 && Array.isArray(data?.list)) {
        const mergeList = data?.list.reduce(
          (modelTagList: ISemantic.ITagItem[], item: ISemantic.ITagItem) => {
            const hasItem = Array.isArray(tagList)
              ? tagList.find((dataListItem: ISemantic.ITagItem) => {
                  return dataListItem.id === item.id;
                })
              : [];
            if (!hasItem) {
              return [item, ...modelTagList];
            }
            return modelTagList;
          },
          tagList,
        );
        setTagList(mergeList);
      } else {
        message.error(msg);
      }
    };

    const queryDimensionListByIds = async (ids: number[]) => {
      if (!isArrayOfValues(ids)) {
        setDimensionList(dimensionList);
        return;
      }
      const { code, data, msg } = await getDimensionList({ ids });
      if (code === 200 && Array.isArray(data?.list)) {
        const mergeList = data?.list.reduce(
          (modelDimensionList: ISemantic.IDimensionItem[], item: ISemantic.IDimensionItem) => {
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
        const mergeList = data.list.reduce(
          (modelMetricList: ISemantic.IMetricItem[], item: ISemantic.IMetricItem) => {
            const hasItem = Array.isArray(metricList)
              ? metricList.find((dataListItem: ISemantic.IMetricItem) => {
                  return dataListItem.id === item.id;
                })
              : [];
            if (!hasItem) {
              return [item, ...modelMetricList];
            }
            return modelMetricList;
          },
          metricList,
        );
        setMetricList(mergeList);
      } else {
        message.error(msg);
      }
    };

    useEffect(() => {
      const dataSetModelConfigs = viewItem?.dataSetDetail?.dataSetModelConfigs;

      if (Array.isArray(dataSetModelConfigs)) {
        const idList: number[] = [];
        const transferKeys: React.Key[] = [];
        const viewConfigMap: any = {};
        dataSetModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
          const { id, metrics, dimensions, tagIds } = item;
          idList.push(id);
          viewConfigMap[id] = { ...item };

          if (queryType === TransType.METRIC) {
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
          }
          if (queryType === TransType.TAG) {
            if (Array.isArray(tagIds)) {
              tagIds.forEach((tagId: number) => {
                transferKeys.push(wrapperTransTypeAndId(TransType.TAG, tagId));
              });
            }
          }
        });
        setSelectedTransferKeys(transferKeys);
        setViewModelConfigsMap(viewConfigMap);
      }
    }, [queryType]);

    useEffect(() => {
      if (queryType !== TransType.METRIC) {
        return;
      }
      if (!dimensionList || !metricList) {
        return;
      }
      const dataSetModelConfigs = isArrayOfValues(Object.values(viewModelConfigsMap))
        ? (Object.values(viewModelConfigsMap) as ISemantic.IViewModelConfigItem[])
        : viewItem?.dataSetDetail?.dataSetModelConfigs;
      if (isArrayOfValues(dataSetModelConfigs)) {
        const allMetrics: number[] = [];
        const allDimensions: number[] = [];
        dataSetModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
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
    }, [queryType, modelItem, dimensionList, metricList]);

    useEffect(() => {
      if (queryType !== TransType.TAG) {
        return;
      }
      if (!tagList) {
        return;
      }
      const dataSetModelConfigs = isArrayOfValues(Object.values(viewModelConfigsMap))
        ? (Object.values(viewModelConfigsMap) as ISemantic.IViewModelConfigItem[])
        : viewItem?.dataSetDetail?.dataSetModelConfigs;
      if (isArrayOfValues(dataSetModelConfigs)) {
        const allTags: number[] = [];
        dataSetModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
          const { tagIds } = item;
          allTags.push(...tagIds);
        });
        queryTagListByIds(allTags);
      } else {
        setTagList(tagList);
      }
    }, [queryType, modelItem, tagList]);

    return (
      <>
        <div style={{ display: queryType === TransType.TAG ? 'none' : 'block' }}>
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
              const dataSetModelConfigs = Object.values(
                submitData,
              ) as ISemantic.IViewModelConfigItem[];

              if (isArrayOfValues(dataSetModelConfigs)) {
                const allMetrics: number[] = [];
                const allDimensions: number[] = [];
                dataSetModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
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
        </div>
        <div style={{ display: queryType !== TransType.TAG ? 'none' : 'block' }}>
          <TagTransferModal
            toolbarSolt={toolbarSolt}
            modelId={modelItem?.id}
            tagList={mergeTagList}
            selectedTransferKeys={selectedTransferKeys}
            onSubmit={(
              submitData: Record<string, ISemantic.IViewModelConfigItem>,
              selectedKeys: React.Key[],
            ) => {
              const dataSetModelConfigs = Object.values(
                submitData,
              ) as ISemantic.IViewModelConfigItem[];

              if (isArrayOfValues(dataSetModelConfigs)) {
                const allTags: number[] = [];
                dataSetModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
                  const { tagIds } = item;
                  allTags.push(...tagIds);
                });
                queryTagListByIds(allTags);
              }
              setViewModelConfigsMap(submitData);
              setSelectedTransferKeys(selectedKeys);
            }}
            onCancel={() => {}}
          />
        </div>
      </>
    );
  },
);

export default ViewModelConfigTransfer;
