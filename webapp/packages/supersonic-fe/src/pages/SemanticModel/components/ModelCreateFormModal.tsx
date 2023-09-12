import React, { useState, useEffect } from 'react';
import { Form, Button, Modal, Input, Switch, Select } from 'antd';
import styles from './style.less';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { createModel, updateModel } from '../service';

const FormItem = Form.Item;

export type ModelCreateFormModalProps = {
  domainId: number;
  basicInfo: any;
  onCancel: () => void;
  onSubmit: (values: any) => void;
};

const ModelCreateFormModal: React.FC<ModelCreateFormModalProps> = (props) => {
  const { basicInfo, domainId, onCancel, onSubmit } = props;

  const [formVals, setFormVals] = useState<any>(basicInfo);
  const [saveLoading, setSaveLoading] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue({
      ...basicInfo,
      alias: basicInfo?.alias && basicInfo.alias.trim() ? basicInfo.alias.split(',') : [],
    });
  }, [basicInfo]);

  const handleConfirm = async () => {
    const fieldsValue = await form.validateFields();
    const columnsValue = { ...fieldsValue, isUnique: 1, domainId };
    const submitData = {
      ...formVals,
      ...columnsValue,
      alias: Array.isArray(fieldsValue.alias) ? fieldsValue.alias.join(',') : '',
    };
    setFormVals(submitData);
    setSaveLoading(true);
    const { code, msg } = await (!submitData.id ? createModel : updateModel)(submitData);
    setSaveLoading(false);
    if (code === 200) {
      onSubmit?.(submitData);
    } else {
      message.error(msg);
    }
  };

  const footer = (
    <>
      <Button onClick={onCancel}>取消</Button>
      <Button type="primary" loading={saveLoading} onClick={handleConfirm}>
        确定
      </Button>
    </>
  );

  return (
    <Modal
      width={640}
      bodyStyle={{ padding: '32px 40px 48px' }}
      destroyOnClose
      title={'模型信息'}
      open={true}
      footer={footer}
      onCancel={onCancel}
    >
      <Form
        {...formLayout}
        form={form}
        initialValues={{
          ...formVals,
        }}
        className={styles.form}
      >
        <FormItem
          name="name"
          label="模型名称"
          rules={[{ required: true, message: '请输入模型名称！' }]}
        >
          <Input placeholder="模型名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label="模型英文名称"
          rules={[{ required: true, message: '请输入模型英文名称！' }]}
        >
          <Input placeholder="请输入模型英文名称" />
        </FormItem>
        <FormItem name="alias" label="别名">
          <Select
            mode="tags"
            placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
            tokenSeparators={[',']}
            maxTagCount={9}
          />
        </FormItem>
        <FormItem name="description" label="模型描述">
          <Input.TextArea placeholder="模型描述" />
        </FormItem>
        <FormItem name="isUnique" label="是否唯一" hidden={true}>
          <Switch size="small" checked={true} />
        </FormItem>
      </Form>
    </Modal>
  );
};

export default ModelCreateFormModal;
