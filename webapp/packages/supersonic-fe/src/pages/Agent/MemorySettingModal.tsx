import React, { useState, useRef } from 'react';
import { Modal, Form, Input, message, Button, Space, Radio } from 'antd';
import { createMemory } from './service';
import { StatusEnum } from './type';
import { formLayout } from '@/components/FormHelper/utils';

export type CreateFormProps = {
  onCancel: () => void;
  agentId: number;
  open: boolean;
  onSubmit: (values?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const MemorySettingModal: React.FC<CreateFormProps> = ({ onCancel, agentId, open, onSubmit }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState<boolean>(false);

  const createTermConfig = async (data: any) => {
    setLoading(true);
    const { code, msg } = await createMemory({
      ...data,
      agentId,
    });
    setLoading(false);
    if (code === 200) {
      onSubmit?.();
    } else {
      message.error(msg);
    }
  };
  const renderFooter = () => {
    return (
      <>
        <Space>
          <Button
            onClick={() => {
              onCancel?.();
            }}
          >
            取消
          </Button>

          <Button
            type="primary"
            loading={loading}
            onClick={() => {
              const formData = form.getFieldsValue();
              createTermConfig(formData);
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
      width={600}
      destroyOnClose
      title="记忆设置"
      style={{ top: 48 }}
      maskClosable={false}
      open={open}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Form
        {...formLayout}
        form={form}
        layout="vertical"
        onValuesChange={(value) => {}}
        initialValues={{
          status: StatusEnum.PENDING,
        }}
      >
        <FormItem
          name="question"
          label="用户问题"
          rules={[{ required: true, message: '请输入用户问题' }]}
        >
          <Input placeholder="请输入用户问题" />
        </FormItem>
        <FormItem name="dbSchema" label="Schema映射">
          <TextArea placeholder="请输入Schema映射" style={{ height: 100 }} />
        </FormItem>
        <FormItem name="s2sql" label="语义S2SQL">
          <TextArea placeholder="请输入语义S2SQL" style={{ height: 100 }} />
        </FormItem>
        <FormItem name="status" label="状态">
          <Radio.Group size="small" buttonStyle="solid">
            <Radio.Button value={StatusEnum.PENDING}>待定</Radio.Button>
            <Radio.Button value={StatusEnum.ENABLED}>已启用</Radio.Button>
            <Radio.Button value={StatusEnum.DISABLED}>已禁用</Radio.Button>
          </Radio.Group>
        </FormItem>
      </Form>
    </Modal>
  );
};

export default MemorySettingModal;
