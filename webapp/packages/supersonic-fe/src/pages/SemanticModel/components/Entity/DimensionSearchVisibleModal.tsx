import React, { useEffect, useState } from 'react';
import { Button, Modal, message, Space } from 'antd';
import ProCard from '@ant-design/pro-card';
import { addDomainExtend, editDomainExtend } from '../../service';
import DimensionMetricVisibleTransfer from './DimensionMetricVisibleTransfer';
import SqlEditor from '@/components/SqlEditor';
type Props = {
  domainId: number;
  themeData: any;
  settingSourceList: any[];
  onCancel: () => void;
  visible: boolean;
  onSubmit: (params?: any) => void;
};

const DimensionSearchVisibleModal: React.FC<Props> = ({
  domainId,
  themeData,
  visible,
  settingSourceList,
  onCancel,
  onSubmit,
}) => {
  const [sourceList, setSourceList] = useState<any[]>([]);
  const [selectedKeyList, setSelectedKeyList] = useState<string[]>([]);
  const [dictRules, setDictRules] = useState<string>('');

  useEffect(() => {
    const dictionaryInfos = themeData?.dictionaryInfos;
    if (Array.isArray(dictionaryInfos)) {
      const target = dictionaryInfos[0];
      if (Array.isArray(target?.ruleList)) {
        setDictRules(target.ruleList[0]);
      }
      const selectKeys = dictionaryInfos.map((item: any) => {
        return item.itemId;
      });
      setSelectedKeyList(selectKeys);
    }
  }, [themeData]);

  useEffect(() => {
    const list = settingSourceList.map((item: any) => {
      const { id, name } = item;
      return { id, name, type: 'dimension' };
    });
    setSourceList(list);
  }, [settingSourceList]);

  const saveDictBatch = async () => {
    const dictionaryInfos = selectedKeyList.map((key: string) => {
      return {
        itemId: key,
        type: 'DIMENSION',
        isDictInfo: true,
        ruleList: dictRules ? [dictRules] : [],
      };
    });
    const id = themeData?.id;
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }
    const { code, msg } = await saveDomainExtendQuery({
      dictionaryInfos,
      domainId,
      id,
    });

    if (code === 200) {
      message.success('保存可见维度值成功');
      onSubmit?.();
      return;
    }
    message.error(msg);
  };

  const saveDictSetting = async () => {
    await saveDictBatch();
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
            saveDictSetting();
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
        title={'可见维度值设置'}
        maskClosable={false}
        open={visible}
        footer={renderFooter()}
        onCancel={onCancel}
      >
        <Space direction="vertical" style={{ width: '100%' }} size={20}>
          <ProCard bordered title="可见设置">
            <DimensionMetricVisibleTransfer
              titles={['不可见维度值', '可见维度值']}
              sourceList={sourceList}
              targetList={selectedKeyList}
              onChange={(newTargetKeys) => {
                handleTransferChange(newTargetKeys);
              }}
            />
          </ProCard>
          <ProCard bordered title="维度值过滤">
            <SqlEditor
              height={'150px'}
              value={dictRules}
              onChange={(sql: string) => {
                setDictRules(sql);
              }}
            />
          </ProCard>
        </Space>
      </Modal>
    </>
  );
};

export default DimensionSearchVisibleModal;
