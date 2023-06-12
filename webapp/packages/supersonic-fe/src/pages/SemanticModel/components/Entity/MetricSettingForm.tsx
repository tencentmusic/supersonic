import { useEffect, useState, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { formLayout } from '@/components/FormHelper/utils';
import { message, Form, Input, Select, Button, InputNumber } from 'antd';
import { addDomainExtend, editDomainExtend } from '../../service';

import styles from '../style.less';
type Props = {
  themeData: any;
  metricList: any[];
  domainId: number;
  onSubmit: (params?: any) => void;
};

const FormItem = Form.Item;
const Option = Select.Option;

const MetricSettingForm: ForwardRefRenderFunction<any, Props> = (
  { metricList, domainId, themeData: uniqueMetricData },
  ref,
) => {
  const [form] = Form.useForm();
  const [metricListOptions, setMetricListOptions] = useState<any>([]);
  const [unitState, setUnit] = useState<number | null>();
  const [periodState, setPeriod] = useState<string>();
  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));

  useEffect(() => {
    form.resetFields();
    setUnit(null);
    setPeriod('');
    if (Object.keys(uniqueMetricData).length === 0) {
      return;
    }
    const { defaultMetrics = [], id } = uniqueMetricData;
    const defaultMetric = defaultMetrics[0];
    const recordId = id === -1 ? undefined : id;
    if (defaultMetric) {
      const { period, unit } = defaultMetric;
      setUnit(unit);
      setPeriod(period);
      form.setFieldsValue({
        ...defaultMetric,
        id: recordId,
      });
    } else {
      form.setFieldsValue({
        id: recordId,
      });
    }
  }, [uniqueMetricData]);

  useEffect(() => {
    const metricOption = metricList.map((item: any) => {
      return {
        label: item.name,
        value: item.id,
      };
    });
    setMetricListOptions(metricOption);
  }, [metricList]);

  const saveEntity = async () => {
    const values = await form.validateFields();
    const { id } = values;
    let saveDomainExtendQuery = addDomainExtend;
    if (id) {
      saveDomainExtendQuery = editDomainExtend;
    }
    const { code, msg, data } = await saveDomainExtendQuery({
      defaultMetrics: [{ ...values }],
      domainId,
      id,
    });

    if (code === 200) {
      form.setFieldValue('id', data);
      message.success('保存成功');
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <Form
        {...formLayout}
        form={form}
        layout="vertical"
        className={styles.form}
        initialValues={{
          unit: 7,
          period: 'DAY',
        }}
      >
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name={'metricId'}
          label={
            <FormItemTitle
              title={'指标'}
              subTitle={'问答搜索结果选择中，如果没有指定指标，将会采用默认指标进行展示'}
            />
          }
        >
          <Select
            allowClear
            showSearch
            style={{ width: '100%' }}
            placeholder="请选择展示指标信息"
            options={metricListOptions}
          />
        </FormItem>
        <FormItem
          label={
            <FormItemTitle
              title={'时间范围'}
              subTitle={'问答搜索结果选择中，如果没有指定时间范围，将会采用默认时间范围'}
            />
          }
        >
          <Input.Group compact>
            <span
              style={{
                display: 'inline-block',
                lineHeight: '32px',
                marginRight: '8px',
              }}
            >
              最近
            </span>
            <InputNumber
              value={unitState}
              style={{ width: '120px' }}
              onChange={(value) => {
                setUnit(value);
                form.setFieldValue('unit', value);
              }}
            />
            <Select
              value={periodState}
              style={{ width: '100px' }}
              onChange={(value) => {
                form.setFieldValue('period', value);
                setPeriod(value);
              }}
            >
              <Option value="DAY">天</Option>
              <Option value="WEEK">周</Option>
              <Option value="MONTH">月</Option>
              <Option value="YEAR">年</Option>
            </Select>
          </Input.Group>
        </FormItem>

        <FormItem name="unit" hidden={true}>
          <InputNumber />
        </FormItem>
        <FormItem name="period" hidden={true}>
          <Input />
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

export default forwardRef(MetricSettingForm);
