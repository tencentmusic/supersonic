import React, { useEffect, useRef } from 'react';
import { Form, Button, Modal, Input, Select } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import styles from '../style.less';
import { ISemantic } from '../../data';

export type CreateFormProps = {
  createModalVisible: boolean;
  termItem?: ISemantic.ITermItem;
  onCancel?: () => void;
  onSubmit?: (values: any) => void;
};

const FormItem = Form.Item;
const { TextArea } = Input;

const TermCreateForm: React.FC<CreateFormProps> = ({
  onCancel,
  createModalVisible,
  termItem,
  onSubmit,
}) => {
  const isEdit = !!termItem?.name;
  const formValRef = useRef({} as any);
  const [form] = Form.useForm();
  const updateFormVal = (val: any) => {
    const formVal = {
      ...formValRef.current,
      ...val,
    };
    formValRef.current = formVal;
  };

  const initData = () => {
    if (!termItem) {
      return;
    }

    const initValue = {
      ...termItem,
    };
    const editInitFormVal = {
      ...formValRef.current,
      ...initValue,
    };

    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
  };

  useEffect(() => {
    if (isEdit) {
      initData();
    }
  }, [termItem]);

  const renderContent = () => {
    return (
      <>
        <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="名称不可重复" />
        </FormItem>
        <FormItem name="alias" label={'近义词'}>
          <Select
            mode="tags"
            placeholder="输入近义词后回车确认，多近义词输入、复制粘贴支持英文逗号自动分隔"
            tokenSeparators={[',']}
            maxTagCount={9}
          />
        </FormItem>
        <FormItem
          name="description"
          label={'描述'}
          rules={[{ required: true, message: '请输入描述' }]}
        >
          <TextArea placeholder="请输入描述" />
        </FormItem>
      </>
    );
  };
  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          onClick={async () => {
            const fieldsValue = await form.validateFields();
            const submitForm = {
              ...formValRef.current,
              ...fieldsValue,
            };
            updateFormVal(submitForm);
            onSubmit?.(submitForm);
          }}
        >
          完成
        </Button>
      </>
    );
  };
  return (
    <Modal
      forceRender
      width={800}
      style={{ top: 48 }}
      destroyOnClose
      title={`${isEdit ? '编辑' : '新建'}术语`}
      maskClosable={false}
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <>
        <Form
          {...formLayout}
          form={form}
          initialValues={{
            ...formValRef.current,
          }}
          className={styles.form}
        >
          {renderContent()}
        </Form>
      </>
    </Modal>
  );
};

export default TermCreateForm;
