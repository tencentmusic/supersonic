import React, { useState } from 'react';
import { Form, Button, Modal, Input } from 'antd';
import type { RegisterFormDetail } from './types';

export type RegisterFormProps = {
  onCancel: () => void;
  onSubmit: (values: RegisterFormDetail) => Promise<any>;
  createModalVisible: boolean;
};

const formLayout = {
  labelCol: { span: 4 },
  wrapperCol: { span: 18 },
};

const { Item } = Form;

const ForgetPwdForm: React.FC<RegisterFormProps> = (props) => {
  const [formVals, setFormVals] = useState<Partial<RegisterFormDetail>>({
    email: '', // 邮箱
  });
  const [saveLoading, setSaveLoading] = useState(false);
  const [form] = Form.useForm();

  const { onSubmit: handleUpdate, onCancel, createModalVisible } = props;

  const handleSubmit = async () => {
    const fieldsValue = await form.validateFields();
    setFormVals({ ...formVals, ...fieldsValue });
    setSaveLoading(true);
    const formValues = {
      ...formVals,
      ...fieldsValue,
    };
    try {
      await handleUpdate(formValues);
      setSaveLoading(false);
    } catch (error) {
      setSaveLoading(false);
    }
  };

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" loading={saveLoading} onClick={handleSubmit}>
          发送邮件
        </Button>
      </>
    );
  };

  return (
    <Modal
      width={600}
      styles={{ padding: '32px 40px 48px' }}
      destroyOnClose
      title="忘记密码"
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Form
        {...formLayout}
        form={form}
        initialValues={{
          ...formVals,
        }}
      >
        <Item name="email" rules={[{ required: true }]} label="邮箱地址">
          <Input size="large" type="email" placeholder="请输入邮箱地址" />
        </Item>
      </Form>
    </Modal>
  );
};

export default ForgetPwdForm;
