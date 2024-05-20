import React, { useState } from 'react';
import { Form, Button, Modal, Input, Switch } from 'antd';
import styles from './style.less';
import { useMounted } from '@/hooks/useMounted';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';

const FormItem = Form.Item;

export type Props = {
  basicInfo: any;
  onCancel: () => void;
  onSubmit: (values: any) => Promise<any>;
};

const DomaintInfoForm: React.FC<Props> = (props) => {
  const { basicInfo, onSubmit: handleUpdate, onCancel } = props;
  const { type, modelType } = basicInfo;

  const isMounted = useMounted();
  const [formVals, setFormVals] = useState<any>(basicInfo);
  const [saveLoading, setSaveLoading] = useState(false);
  const [form] = Form.useForm();

  const handleConfirm = async () => {
    const fieldsValue = await form.validateFields();
    const columnsValue = { ...fieldsValue, isUnique: 1 };
    setFormVals({ ...formVals, ...columnsValue });
    setSaveLoading(true);
    try {
      await handleUpdate({ ...formVals, ...columnsValue });
      if (isMounted()) {
        setSaveLoading(false);
      }
    } catch (error) {
      message.error('接口调用出错');
      setSaveLoading(false);
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

  const infoName = type === 'top' ? '主题域' : '模型集';

  return (
    <Modal
      width={640}
      destroyOnClose
      title={`${modelType === 'add' ? '新增' : '编辑'}${infoName}`}
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
        {type !== 'top' && modelType === 'add' && (
          <FormItem name="parentName" label="主题域名称">
            <Input disabled placeholder="主题域名称" />
          </FormItem>
        )}
        <FormItem
          name="name"
          label={`${infoName}名称`}
          rules={[{ required: true, message: `请输入${infoName}名称！` }]}
        >
          <Input placeholder="主题域名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label={`${infoName}英文名称`}
          rules={[{ required: true, message: `请输入${infoName}英文名称！` }]}
        >
          <Input placeholder={`请输入${infoName}英文名称`} />
        </FormItem>
        <FormItem name="description" label={`${infoName}描述`} hidden={true}>
          <Input.TextArea placeholder={`${infoName}描述`} />
        </FormItem>
        <FormItem name="isUnique" label="是否唯一" hidden={true}>
          <Switch size="small" checked={true} />
        </FormItem>
      </Form>
    </Modal>
  );
};

export default DomaintInfoForm;
