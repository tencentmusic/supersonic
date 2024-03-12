import React, { useState, useEffect } from 'react';

import { ISemantic } from '../../data';

import { TransType } from '../../enum';
import DimensionMetricVisibleTransfer from '../../components/Entity/DimensionMetricVisibleTransfer';
import { wrapperTransTypeAndId } from '../../utils';

export type ModelCreateFormModalProps = {
  tagList?: ISemantic.ITagItem[];
  modelId?: number;
  selectedTransferKeys: React.Key[];
  toolbarSolt?: React.ReactNode;
  onCancel: () => void;
  onSubmit: (values: any, selectedKeys: React.Key[]) => void;
};

const TagTransferModal: React.FC<ModelCreateFormModalProps> = ({
  modelId,
  toolbarSolt,
  selectedTransferKeys,
  tagList,
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
    if (!tagList) {
      return;
    }
    const sourceTagList = tagList.reduce((mergeList: any[], item) => {
      mergeList.push(addItemKey(item, TransType.TAG));
      return mergeList;
    }, []);

    const hasTagList = selectedItemList.reduce((modelTagList: ISemantic.ITagItem[], item) => {
      const hasItem = sourceTagList.find((dataListItem: ISemantic.ITagItem) => {
        return dataListItem.id === item.id;
      });
      if (!hasItem) {
        modelTagList.push(addItemKey(item, TransType.TAG));
      }
      return modelTagList;
    }, []);

    setSourceList([...sourceTagList, ...hasTagList]);
  }, [tagList]);

  return (
    <DimensionMetricVisibleTransfer
      titles={[<>{toolbarSolt}</>, '已加入标签']}
      listStyle={{
        width: 520,
        height: 600,
      }}
      targetList={selectedTransferKeys}
      sourceList={sourceList}
      onChange={(newTargetKeys: string[]) => {
        const removeTagList: ISemantic.ITagItem[] = [];
        const tagItemChangeList = Array.isArray(tagList)
          ? tagList.reduce((tagChangeList: any[], item: any) => {
              if (newTargetKeys.includes(wrapperTransTypeAndId(TransType.TAG, item.id))) {
                tagChangeList.push(item);
              } else {
                removeTagList.push(item.id);
              }
              return tagChangeList;
            }, [])
          : [];

        setSelectedItemList([...tagItemChangeList]);

        // 如果不是当前选中model中的指标或者维度，则先从本地数据中删除，避免后续请求数据更新时产生视觉上的界面闪烁
        const preUpdateSourceData = sourceList.filter((item) => {
          const { id } = item;

          if (modelId !== item.modelId && removeTagList.includes(id)) {
            return false;
          }

          return true;
        });
        setSourceList([...preUpdateSourceData]);
        const dataSetModelConfigs = [...tagItemChangeList].reduce((config, item) => {
          const { modelId, id } = item;
          if (config[modelId]) {
            config[modelId].tagIds.push(id);
          } else {
            config[modelId] = {
              id: modelId,
              tagIds: [id],
            };
          }
          return config;
        }, {});

        onSubmit?.(dataSetModelConfigs, newTargetKeys);
      }}
    />
  );
};

export default TagTransferModal;
