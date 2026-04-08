import React, { useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Switch } from 'antd';
import type { BusinessTopic } from '@/services/businessTopic';

interface TopicFormModalProps {
  visible: boolean;
  record?: BusinessTopic;
  onCancel: () => void;
  onSubmit: (values: Partial<BusinessTopic>) => void;
}

const TopicFormModal: React.FC<TopicFormModalProps> = ({
  visible,
  record,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      if (record) {
        form.setFieldsValue({
          name: record.name,
          description: record.description,
          priority: record.priority ?? 0,
          enabled: record.enabled !== 0,
        });
      } else {
        form.resetFields();
      }
    }
  }, [visible, record]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onSubmit({
      ...values,
      enabled: values.enabled ? 1 : 0,
    });
  };

  return (
    <Modal
      title={record ? '编辑经营主题' : '新建经营主题'}
      open={visible}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
      width={520}
    >
      <Form form={form} layout="vertical" initialValues={{ priority: 0, enabled: true }}>
        <Form.Item
          name="name"
          label="主题名称"
          rules={[{ required: true, message: '请输入主题名称' }]}
        >
          <Input maxLength={200} placeholder="如：每日收入看板" />
        </Form.Item>
        <Form.Item name="description" label="说明">
          <Input.TextArea rows={3} placeholder="描述该主题关注的业务范围" />
        </Form.Item>
        <Form.Item
          name="priority"
          label="优先级"
          tooltip="数值越小优先级越高，驾驶舱按此排序"
        >
          <InputNumber min={0} max={999} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TopicFormModal;
