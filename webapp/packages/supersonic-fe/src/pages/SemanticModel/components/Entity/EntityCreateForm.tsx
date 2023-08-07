import { useEffect, useState, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button } from 'antd';
import { updateDomain } from '../../service';
import type { ISemantic } from '../../data';
import { formLayout } from '@/components/FormHelper/utils';
import styles from '../style.less';

type Props = {
  domainData?: ISemantic.IDomainItem;
  dimensionList: ISemantic.IDimensionList;
  domainId: number;
  onSubmit: () => void;
};

const FormItem = Form.Item;

const EntityCreateForm: ForwardRefRenderFunction<any, Props> = (
  { domainData, dimensionList, domainId, onSubmit },
  ref,
) => {
  const [form] = Form.useForm();
  const [dimensionListOptions, setDimensionListOptions] = useState<any>([]);
  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useEffect(() => {
    form.resetFields();
    if (!domainData?.entity) {
      return;
    }
    const { entity } = domainData;
    form.setFieldsValue({
      ...entity,
      name: entity.names.join(','),
    });
  }, [domainData]);

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));

  useEffect(() => {
    const dimensionEnum = dimensionList.map((item: ISemantic.IDimensionItem) => {
      return {
        label: item.name,
        value: item.id,
      };
    });
    setDimensionListOptions(dimensionEnum);
  }, [dimensionList]);

  const saveEntity = async () => {
    const values = await form.validateFields();
    const { name } = values;
    const { code, msg, data } = await updateDomain({
      ...domainData,
      entity: {
        ...values,
        names: name.split(','),
      },
      id: domainId,
      domainId,
    });

    if (code === 200) {
      form.setFieldValue('id', data);
      onSubmit?.();
      message.success('保存成功');
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <Form {...formLayout} form={form} layout="vertical" className={styles.form}>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name="name"
          label="实体别名"
          // rules={[{ required: true, message: '请输入实体别名' }]}
        >
          <Input placeholder="请输入实体别名,多个名称以英文逗号分隔" />
        </FormItem>
        <FormItem
          name="entityId"
          label="唯一标识"
          // rules={[{ required: true, message: '请选择实体标识' }]}
        >
          <Select
            // mode="multiple"
            allowClear
            style={{ width: '100%' }}
            // filterOption={(inputValue: string, item: any) => {
            //   const { label } = item;
            //   if (label.includes(inputValue)) {
            //     return true;
            //   }
            //   return false;
            // }}
            placeholder="请选择主体标识"
            options={dimensionListOptions}
          />
        </FormItem>
        <FormItem>
          <Button
            type="primary"
            onClick={() => {
              saveEntity();
            }}
          >
            保 存
          </Button>
        </FormItem>
      </Form>
    </>
  );
};

export default forwardRef(EntityCreateForm);
