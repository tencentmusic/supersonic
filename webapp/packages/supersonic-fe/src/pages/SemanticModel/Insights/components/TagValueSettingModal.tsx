import React, { useState } from 'react';
import { Button, Modal, Space } from 'antd';
import { KnowledgeConfigTypeEnum } from '../../enum';
import { ISemantic } from '../../data';
import DimensionValueSettingForm from '../../components/Entity/DimensionValueSettingForm';

export type CreateFormProps = {
  onCancel: () => void;
  tagItem: ISemantic.ITagItem;
  open: boolean;
  onSubmit: (values?: any) => void;
};

type TableDataSource = { techName: string; bizName: string; alias?: string[] };

const TagValueSettingModal: React.FC<CreateFormProps> = ({ onCancel, open, tagItem, onSubmit }) => {
  const [tableDataSource, setTableDataSource] = useState<TableDataSource[]>([]);

  // const handleSubmit = async () => {
  //   await saveDimension({ dimValueMaps });
  //   onSubmit?.(dimValueMaps);
  // };

  // const saveDimension = async (fieldsValue: any) => {
  //   if (!dimensionItem?.id) {
  //     return;
  //   }
  //   const queryParams = {
  //     ...dimensionItem,
  //     domainId: selectDomainId,
  //     ...fieldsValue,
  //   };
  //   const { code, msg } = await updateDimension(queryParams);
  //   if (code === 200) {
  //     return;
  //   }
  //   message.error(msg);
  // };

  const renderFooter = () => {
    return (
      <>
        <Space>
          <Button onClick={onCancel}>取消</Button>

          <Button
            type="primary"
            onClick={() => {
              // handleSubmit();
            }}
          >
            完成
          </Button>
        </Space>
      </>
    );
  };

  return (
    <Modal
      width={800}
      destroyOnClose
      title="标签值设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <DimensionValueSettingForm dataItem={tagItem} type={KnowledgeConfigTypeEnum.TAG} />
    </Modal>
  );
};

export default TagValueSettingModal;
