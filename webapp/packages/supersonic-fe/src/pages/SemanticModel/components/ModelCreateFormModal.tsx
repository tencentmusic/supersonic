import React, { useState, useEffect } from 'react';
import { Form, Button, Modal, Input, Switch, Select } from 'antd';
import styles from './style.less';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { createModel, updateModel, getDimensionList } from '../service';
import { ISemantic } from '../data';

const FormItem = Form.Item;

export type ModelCreateFormModalProps = {
  domainId: number;
  basicInfo: any;
  onCancel: () => void;
  onSubmit: (values: any) => void;
};

const ModelCreateFormModal: React.FC<ModelCreateFormModalProps> = (props) => {
  const { basicInfo, domainId, onCancel, onSubmit } = props;

  const [formVals, setFormVals] = useState<ISemantic.IModelItem>(basicInfo);
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [form] = Form.useForm();

  const [dimensionOptions, setDimensionOptions] = useState<{ label: string; value: number }[]>([]);

  useEffect(() => {
    if (basicInfo?.id) {
      queryDimensionList();
    }
  }, []);

  const queryDimensionList = async () => {
    const { code, data, msg } = await getDimensionList({ modelId: basicInfo.id });
    if (code === 200 && Array.isArray(data?.list)) {
      setDimensionOptions(
        data.list.map((item: ISemantic.IDimensionItem) => {
          return {
            label: item.name,
            value: item.id,
          };
        }),
      );
    } else {
      message.error(msg);
    }
  };
  useEffect(() => {
    form.setFieldsValue({
      ...basicInfo,
      alias: basicInfo?.alias && basicInfo.alias.trim() ? basicInfo.alias.split(',') : [],
      drillDownDimensionsIds: Array.isArray(basicInfo?.drillDownDimensions)
        ? basicInfo.drillDownDimensions.map(
            (item: ISemantic.IDrillDownDimensionItem) => item.dimensionId,
          )
        : [],
    });
  }, [basicInfo]);

  const handleConfirm = async () => {
    const fieldsValue = await form.validateFields();
    const columnsValue = { ...fieldsValue, isUnique: 1, domainId };
    const submitData: ISemantic.IModelItem = {
      ...formVals,
      ...columnsValue,
      alias: Array.isArray(fieldsValue.alias) ? fieldsValue.alias.join(',') : '',
      drillDownDimensions: Array.isArray(fieldsValue.drillDownDimensionsIds)
        ? fieldsValue.drillDownDimensionsIds.map((id: number) => {
            return {
              dimensionId: id,
            };
          })
        : [],
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
        <FormItem
          name="drillDownDimensionsIds"
          label={
            <FormItemTitle
              title={'默认下钻维度'}
              subTitle={'配置之后,可在指标主页和问答指标卡处选择用来对指标进行下钻和过滤'}
            />
          }
          hidden={!basicInfo?.id}
        >
          <Select
            mode="multiple"
            options={dimensionOptions}
            placeholder="请选择默认下钻维度"
            maxTagCount={9}
          />
        </FormItem>
        <FormItem name="isUnique" label="是否唯一" hidden={true}>
          <Switch size="small" checked={true} />
        </FormItem>
      </Form>
    </Modal>
  );
};

export default ModelCreateFormModal;
