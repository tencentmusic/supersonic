import React, { useEffect, useState } from 'react';
import { Button, Modal, message } from 'antd';
import { addDomainExtend, editDomainExtend, getDomainExtendDetailConfig } from '../../service';
import DimensionMetricVisibleTransfer from './DimensionMetricVisibleTransfer';
import { exChangeRichEntityListToIds } from './utils';

type Props = {
  domainId: number;
  themeData: any;
  settingType: 'dimension' | 'metric';
  settingSourceList: any[];
  onCancel: () => void;
  visible: boolean;
  onSubmit: (params?: any) => void;
};

const dimensionConfig = {
  blackIdListKey: 'blackDimIdList',
  visibleIdListKey: 'whiteDimIdList',
  modalTitle: '问答可见维度信息',
  titles: ['不可见维度', '可见维度'],
};

const metricConfig = {
  blackIdListKey: 'blackMetricIdList',
  visibleIdListKey: 'whiteMetricIdList',
  modalTitle: '问答可见指标信息',
  titles: ['不可见指标', '可见指标'],
};

const DimensionMetricVisibleModal: React.FC<Props> = ({
  domainId,
  visible,
  themeData = {},
  settingType,
  settingSourceList,
  onCancel,
  onSubmit,
}) => {
  const [sourceList, setSourceList] = useState<any[]>([]);
  const [selectedKeyList, setSelectedKeyList] = useState<string[]>([]);
  const settingTypeConfig = settingType === 'dimension' ? dimensionConfig : metricConfig;
  useEffect(() => {
    const list = settingSourceList.map((item: any) => {
      const { id, name } = item;
      return { id, name, type: settingType };
    });
    setSourceList(list);
  }, [settingSourceList]);

  useEffect(() => {
    setSelectedKeyList(themeData.visibility?.[settingTypeConfig.visibleIdListKey] || []);
  }, [themeData]);

  const saveEntity = async () => {
    const { id, entity } = themeData;
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }
    const blackIdList = settingSourceList.reduce((list, item: any) => {
      const { id: targetId } = item;
      if (!selectedKeyList.includes(targetId)) {
        list.push(targetId);
      }
      return list;
    }, []);
    const entityParams = exChangeRichEntityListToIds(entity);
    themeData.entity = entityParams;
    const params = {
      ...themeData,
      visibility: themeData.visibility || {},
    };
    params.visibility[settingTypeConfig.blackIdListKey] = blackIdList;

    if (!params.visibility.blackDimIdList) {
      params.visibility.blackDimIdList = [];
    }
    if (!params.visibility.blackMetricIdList) {
      params.visibility.blackMetricIdList = [];
    }

    const { code, msg } = await saveDomainExtendQuery({
      ...params,
      id,
      domainId,
    });
    if (code === 200) {
      onSubmit?.();
      message.success('保存成功');
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
            saveEntity();
          }}
        >
          完成
        </Button>
      </>
    );
  };
  return (
    <>
      <Modal
        width={1200}
        destroyOnClose
        title={settingTypeConfig.modalTitle}
        maskClosable={false}
        open={visible}
        footer={renderFooter()}
        onCancel={onCancel}
      >
        <DimensionMetricVisibleTransfer
          titles={settingTypeConfig.titles}
          sourceList={sourceList}
          targetList={selectedKeyList}
          onChange={(newTargetKeys) => {
            handleTransferChange(newTargetKeys);
          }}
        />
      </Modal>
    </>
  );
};

export default DimensionMetricVisibleModal;
