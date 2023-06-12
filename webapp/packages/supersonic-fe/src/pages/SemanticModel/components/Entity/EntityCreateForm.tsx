import { useEffect, useState, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button } from 'antd';
import { addDomainExtend, editDomainExtend } from '../../service';
import { formLayout } from '@/components/FormHelper/utils';

import styles from '../style.less';
type Props = {
  entityData: any;
  metricList: any[];
  dimensionList: any[];
  domainId: number;
  onSubmit: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const EntityCreateForm: ForwardRefRenderFunction<any, Props> = (
  { entityData, metricList, dimensionList, domainId, onSubmit },
  ref,
) => {
  const [form] = Form.useForm();

  const [metricListOptions, setMetricListOptions] = useState<any>([]);
  const [dimensionListOptions, setDimensionListOptions] = useState<any>([]);

  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useEffect(() => {
    form.resetFields();
    if (Object.keys(entityData).length === 0) {
      return;
    }
    const { detailData = {}, names = [] } = entityData;
    if (!detailData.dimensionIds) {
      entityData = {
        ...entityData,
        detailData: {
          ...detailData,
          dimensionIds: [],
        },
      };
    }
    if (!detailData.metricIds) {
      entityData = {
        ...entityData,
        detailData: {
          ...detailData,
          metricIds: [],
        },
      };
    }
    form.setFieldsValue({ ...entityData, name: names.join(',') });
  }, [entityData]);

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));
  useEffect(() => {
    const metricOption = metricList.map((item: any) => {
      return {
        label: item.name,
        value: item.id,
      };
    });
    setMetricListOptions(metricOption);
  }, [metricList]);

  useEffect(() => {
    const dimensionEnum = dimensionList.map((item: any) => {
      return {
        label: item.name,
        value: item.id,
      };
    });
    setDimensionListOptions(dimensionEnum);
  }, [dimensionList]);

  const saveEntity = async () => {
    const values = await form.validateFields();
    const { id, name } = values;
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }
    const { code, msg, data } = await saveDomainExtendQuery({
      entity: {
        ...values,
        names: name.split(','),
      },
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
          label="实体名称"
          rules={[{ required: true, message: '请输入实体名称' }]}
        >
          <TextArea
            placeholder="请输入实体名称,多个实体名称以英文逗号分隔"
            style={{ height: 100 }}
          />
        </FormItem>
        <FormItem
          name="entityIds"
          label="唯一标识"
          rules={[{ required: true, message: '请选择实体标识' }]}
        >
          <Select
            mode="multiple"
            allowClear
            style={{ width: '100%' }}
            placeholder="请选择主体标识"
            options={dimensionListOptions}
          />
        </FormItem>
        <FormItem name={['detailData', 'dimensionIds']} label="维度信息">
          <Select
            mode="multiple"
            allowClear
            style={{ width: '100%' }}
            placeholder="请选择展示维度信息"
            options={dimensionListOptions}
          />
        </FormItem>
        <FormItem name={['detailData', 'metricIds']} label="指标信息">
          <Select
            mode="multiple"
            allowClear
            style={{ width: '100%' }}
            placeholder="请选择展示指标信息"
            options={metricListOptions}
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
