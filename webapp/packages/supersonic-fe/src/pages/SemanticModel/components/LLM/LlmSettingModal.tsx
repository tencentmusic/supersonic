import React, { useState, useRef } from 'react';
import { Button, Modal, Space } from 'antd';
import LlmCreateForm from './LlmCreateForm';
import { ISemantic } from '../../data';

export type CreateFormProps = {
  onCancel: () => void;
  llmItem?: ISemantic.IDatabaseItem;
  open: boolean;
  onSubmit: (values?: any) => void;
};

const DatabaseSettingModal: React.FC<CreateFormProps> = ({ onCancel, llmItem, open, onSubmit }) => {
  const [testLoading, setTestLoading] = useState<boolean>(false);

  const createFormRef = useRef<any>({});

  const handleTestConnection = async () => {
    setTestLoading(true);
    await createFormRef.current.testLlmConnection();
    setTestLoading(false);
  };

  const renderFooter = () => {
    return (
      <>
        <Space>
          <Button
            type="primary"
            loading={testLoading}
            onClick={() => {
              handleTestConnection();
            }}
          >
            连接测试
          </Button>

          <Button
            type="primary"
            onClick={() => {
              createFormRef.current.saveLlmConfig();
            }}
          >
            保 存
          </Button>
        </Space>
      </>
    );
  };

  return (
    <Modal
      width={800}
      destroyOnClose
      title="大模型设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <LlmCreateForm
        ref={createFormRef}
        llmItem={llmItem}
        onSubmit={() => {
          onSubmit?.();
        }}
      />
    </Modal>
  );
};

export default DatabaseSettingModal;
