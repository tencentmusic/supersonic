import React, { useEffect, useState, useRef } from 'react';
import { Modal, message, Tabs, Button } from 'antd';

import { addDomainExtend, editDomainExtend } from '../../service';
import DimensionMetricVisibleTransfer from './DimensionMetricVisibleTransfer';
import { IChatConfig } from '../../data';
import DimensionValueSettingForm from './DimensionValueSettingForm';
import { TransType } from '../../enum';
import { wrapperTransTypeAndId, formatRichEntityDataListToIds } from './utils';

type Props = {
  domainId: number;
  entityData: any;
  chatConfigKey: string;
  settingSourceList: any[];
  onCancel: () => void;
  visible: boolean;
  onSubmit: (params?: { isSilenceSubmit?: boolean }) => void;
};

const dimensionConfig = {
  blackIdListKey: 'blackDimIdList',
  visibleIdListKey: 'whiteDimIdList',
  modalTitle: '问答可见信息',
  titles: ['不可见维度/指标', '可见维度/指标'],
};

const DimensionAndMetricVisibleModal: React.FC<Props> = ({
  domainId,
  visible,
  entityData = {},
  chatConfigKey,
  settingSourceList,
  onCancel,
  onSubmit,
}) => {
  const [selectedKeyList, setSelectedKeyList] = useState<string[]>([]);
  const settingTypeConfig = dimensionConfig;
  const formatEntityData = formatRichEntityDataListToIds(entityData);
  const [knowledgeInfosMap, setKnowledgeInfosMap] = useState<IChatConfig.IKnowledgeInfosItemMap>(
    {},
  );

  const [activeKey, setActiveKey] = useState<string>('visibleSetting');
  const formRef = useRef<any>();

  const [globalKnowledgeConfigInitialValues, setGlobalKnowledgeConfigInitialValues] =
    useState<IChatConfig.IKnowledgeConfig>();

  useEffect(() => {
    if (entityData?.visibility && Array.isArray(settingSourceList)) {
      const { whiteDimIdList, whiteMetricIdList } = entityData.visibility;
      const dimensionIdString = whiteDimIdList.map((dimensionId: number) => {
        return wrapperTransTypeAndId(TransType.DIMENSION, dimensionId);
      });
      const metricIdString = whiteMetricIdList.map((metricId: number) => {
        return wrapperTransTypeAndId(TransType.METRIC, metricId);
      });
      setSelectedKeyList([...dimensionIdString, ...metricIdString]);
    }
    if (entityData?.globalKnowledgeConfig) {
      setGlobalKnowledgeConfigInitialValues(entityData.globalKnowledgeConfig);
    }
    if (Array.isArray(entityData?.knowledgeInfos)) {
      const infoMap = entityData.knowledgeInfos.reduce(
        (maps: IChatConfig.IKnowledgeInfosItemMap, item: IChatConfig.IKnowledgeInfosItem) => {
          const { bizName } = item;
          maps[bizName] = item;
          return maps;
        },
        {},
      );
      setKnowledgeInfosMap(infoMap);
    }
  }, [entityData, settingSourceList]);

  const saveEntity = async (submitData: any, isSilenceSubmit = false) => {
    const { selectedKeyList, knowledgeInfosMap } = submitData;
    const globalKnowledgeConfigFormFields = await formRef?.current?.getFormValidateFields?.();
    let globalKnowledgeConfig = entityData.globalKnowledgeConfig;
    if (globalKnowledgeConfigFormFields) {
      globalKnowledgeConfig = globalKnowledgeConfigFormFields;
    }
    const { id, modelId } = entityData;
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }

    const blackIdListMap = settingSourceList.reduce(
      (ids, item) => {
        const { id, transType } = item;
        if (!selectedKeyList.includes(wrapperTransTypeAndId(transType, id))) {
          if (transType === TransType.DIMENSION) {
            ids.blackDimIdList.push(id);
          }
          if (transType === TransType.METRIC) {
            ids.blackMetricIdList.push(id);
          }
        }
        return ids;
      },
      {
        blackDimIdList: [],
        blackMetricIdList: [],
      },
    );

    const knowledgeInfos = Object.keys(knowledgeInfosMap).reduce(
      (infoList: IChatConfig.IKnowledgeInfosItem[], key: string) => {
        const target = knowledgeInfosMap[key];
        if (target.searchEnable) {
          infoList.push(target);
        }
        return infoList;
      },
      [],
    );

    const params = {
      ...formatEntityData,
      visibility: blackIdListMap,
      knowledgeInfos,
      ...(globalKnowledgeConfig ? { globalKnowledgeConfig } : {}),
    };

    const { code, msg } = await saveDomainExtendQuery({
      [chatConfigKey]: params,
      id,
      modelId,
    });
    if (code === 200) {
      if (!isSilenceSubmit) {
        message.success('保存成功');
      }
      onSubmit?.({ isSilenceSubmit });
      return;
    }
    message.error(msg);
  };

  const handleTransferChange = (newTargetKeys: string[]) => {
    setSelectedKeyList(newTargetKeys);
  };

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          onClick={() => {
            saveEntity({ selectedKeyList, knowledgeInfosMap });
          }}
        >
          完成
        </Button>
      </>
    );
  };

  const tabItem = [
    {
      label: '可见设置',
      key: 'visibleSetting',
      children: (
        <DimensionMetricVisibleTransfer
          onKnowledgeInfosMapChange={(knowledgeInfos) => {
            setKnowledgeInfosMap(knowledgeInfos);
            saveEntity({ selectedKeyList, knowledgeInfosMap: knowledgeInfos }, true);
          }}
          knowledgeInfosMap={knowledgeInfosMap}
          titles={settingTypeConfig.titles}
          sourceList={settingSourceList}
          targetList={selectedKeyList}
          onChange={(newTargetKeys) => {
            handleTransferChange(newTargetKeys);
            saveEntity({ selectedKeyList: newTargetKeys, knowledgeInfosMap }, true);
          }}
        />
      ),
    },
    {
      label: '全局维度值过滤',
      key: 'dimensionValueFilter',
      children: (
        <div style={{ margin: '0 auto', width: '975px' }}>
          <DimensionValueSettingForm
            initialValues={globalKnowledgeConfigInitialValues}
            ref={formRef}
          />
        </div>
      ),
    },
  ];

  return (
    <>
      <Modal
        width={1200}
        destroyOnClose
        title={settingTypeConfig.modalTitle}
        maskClosable={false}
        open={visible}
        footer={activeKey === 'visibleSetting' ? false : renderFooter()}
        // footer={false}
        onCancel={onCancel}
      >
        <Tabs
          items={tabItem}
          defaultActiveKey="visibleSetting"
          onChange={(key) => {
            setActiveKey(key);
          }}
        />
      </Modal>
    </>
  );
};

export default DimensionAndMetricVisibleModal;
