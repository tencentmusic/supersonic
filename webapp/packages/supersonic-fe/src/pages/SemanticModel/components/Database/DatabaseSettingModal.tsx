import React, { useState, useRef } from 'react';
import { Button, Modal, Space } from 'antd';
import DatabaseCreateForm from './DatabaseCreateForm';
import { ISemantic } from '../../data';

export type CreateFormProps = {
  onCancel: () => void;
  databaseItem?: ISemantic.IDatabaseItem;
  open: boolean;
  onSubmit: (values?: any) => void;
};

const DatabaseSettingModal: React.FC<CreateFormProps> = ({
  onCancel,
  databaseItem,
  open,
  onSubmit,
}) => {
  const [testLoading, setTestLoading] = useState<boolean>(false);

  const createFormRef = useRef<any>({});

  const handleTestConnection = async () => {
    setTestLoading(true);
    await createFormRef.current.testDatabaseConnection();
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
              createFormRef.current.saveDatabaseConfig();
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
      width={1200}
      destroyOnClose
      title="数据库连接设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <DatabaseCreateForm
        hideSubmitBtn={true}
        ref={createFormRef}
        dataBaseConfig={databaseItem}
        onSubmit={() => {
          onSubmit?.();
        }}
      />
    </Modal>
  );
};

export default DatabaseSettingModal;
