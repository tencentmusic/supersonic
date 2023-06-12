import React, { useEffect, useRef, useState } from 'react';
import { Form, Button, Modal, Steps, Input, Select, Switch, InputNumber } from 'antd';
import MetricMeasuresFormTable from './MetricMeasuresFormTable';
import { SENSITIVE_LEVEL_OPTIONS } from '../constant';
import { formLayout } from '@/components/FormHelper/utils';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import styles from './style.less';
import { getMeasureListByDomainId } from '../service';

export type CreateFormProps = {
  domainId: number;
  createModalVisible: boolean;
  metricItem: any;
  onCancel?: () => void;
  onSubmit: (values: any) => void;
};

const { Step } = Steps;
const FormItem = Form.Item;
const { TextArea } = Input;
const { Option } = Select;

const MetricInfoCreateForm: React.FC<CreateFormProps> = ({
  domainId,
  onCancel,
  createModalVisible,
  metricItem,
  onSubmit,
}) => {
  const isEdit = !!metricItem?.id;
  const [currentStep, setCurrentStep] = useState(0);
  const formValRef = useRef({} as any);
  const [form] = Form.useForm();
  const updateFormVal = (val: SaveDataSetForm) => {
    formValRef.current = val;
  };

  const [classMeasureList, setClassMeasureList] = useState<any[]>([]);

  const [exprTypeParamsState, setExprTypeParamsState] = useState<any>([]);

  const [exprSql, setExprSql] = useState<string>('');

  const [isPercentState, setIsPercentState] = useState<boolean>(false);

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const queryClassMeasureList = async () => {
    const { code, data } = await getMeasureListByDomainId(domainId);
    if (code === 200) {
      setClassMeasureList(data);
      return;
    }
    setClassMeasureList([]);
  };

  useEffect(() => {
    queryClassMeasureList();
  }, []);

  const handleNext = async () => {
    const fieldsValue = await form.validateFields();
    const submitForm = {
      ...formValRef.current,
      ...fieldsValue,
      typeParams: {
        expr: exprSql,
        measures: exprTypeParamsState,
      },
      dataFormatType: isPercentState ? 'percent' : '',
    };
    updateFormVal(submitForm);
    if (currentStep < 1) {
      forward();
    } else {
      onSubmit?.(submitForm);
    }
  };

  const initData = () => {
    const {
      id,
      name,
      bizName,
      description,
      sensitiveLevel,
      typeParams: typeParams,
      dataFormat,
      dataFormatType,
    } = metricItem as any;
    const isPercent = dataFormatType === 'percent';
    const initValue = {
      id,
      name,
      bizName,
      sensitiveLevel,
      description,
      isPercent,
      dataFormat: dataFormat || {
        decimalPlaces: 2,
        needMultiply100: false,
      },
    };
    const editInitFormVal = {
      ...formValRef.current,
      ...initValue,
    };
    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
    setExprTypeParamsState(typeParams.measures);
    setExprSql(typeParams.expr);
    setIsPercentState(isPercent);
  };

  useEffect(() => {
    if (isEdit) {
      initData();
    } else {
      // initFields([]);
    }
  }, [metricItem]);

  const renderContent = () => {
    if (currentStep === 1) {
      return (
        <MetricMeasuresFormTable
          typeParams={{
            measures: exprTypeParamsState,
            expr: exprSql,
          }}
          measuresList={classMeasureList}
          onFieldChange={(typeParams: any) => {
            setExprTypeParamsState([...typeParams]);
          }}
          onSqlChange={(sql: string) => {
            setExprSql(sql);
          }}
        />
      );
    }

    return (
      <>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name="name"
          label="指标中文名"
          rules={[{ required: true, message: '请输入指标中文名' }]}
        >
          <Input placeholder="名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label="指标英文名"
          rules={[{ required: true, message: '请输入指标英文名' }]}
        >
          <Input placeholder="名称不可重复" disabled={isEdit} />
        </FormItem>
        <FormItem
          name="sensitiveLevel"
          label="敏感度"
          rules={[{ required: true, message: '请选择敏感度' }]}
        >
          <Select placeholder="请选择敏感度">
            {SENSITIVE_LEVEL_OPTIONS.map((item) => (
              <Option key={item.value} value={item.value}>
                {item.label}
              </Option>
            ))}
          </Select>
        </FormItem>
        <FormItem
          name="description"
          label="指标描述"
          rules={[{ required: true, message: '请输入指标描述' }]}
        >
          <TextArea placeholder="请输入指标描述" />
        </FormItem>
        <FormItem
          label={
            <FormItemTitle
              title={'是否展示为百分比'}
              subTitle={'开启后，指标数据展示时会根据配置进行格式化，如0.02 -> 2%'}
            />
          }
          name="isPercent"
          valuePropName="checked"
        >
          <Switch />
        </FormItem>
        {isPercentState && (
          <>
            <FormItem
              label={
                <FormItemTitle
                  title={'小数位数'}
                  subTitle={'对小数位数进行设置，如保留两位，0.021252 -> 2.12%'}
                />
              }
              name={['dataFormat', 'decimalPlaces']}
            >
              <InputNumber placeholder="请输入需要保留小数位数" style={{ width: '300px' }} />
            </FormItem>
            <FormItem
              label={
                <FormItemTitle
                  title={'原始值是否乘以100'}
                  subTitle={'如 原始值0.001 ->展示值0.1% '}
                />
                // <FormItemTitle
                //   title={'仅添加百分号'}
                //   subTitle={'开启后，会对原始数值直接加%，如0.02 -> 0.02%'}
                // />
              }
              name={['dataFormat', 'needMultiply100']}
              valuePropName="checked"
            >
              <Switch />
            </FormItem>
          </>
        )}
      </>
    );
  };
  const renderFooter = () => {
    if (currentStep === 1) {
      return (
        <>
          <Button style={{ float: 'left' }} onClick={backward}>
            上一步
          </Button>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" onClick={handleNext}>
            完成
          </Button>
        </>
      );
    }
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" onClick={handleNext}>
          下一步
        </Button>
      </>
    );
  };
  return (
    <Modal
      forceRender
      width={1300}
      style={{ top: 48 }}
      bodyStyle={{ padding: '32px 40px 48px' }}
      destroyOnClose
      title={`${isEdit ? '编辑' : '新建'}指标`}
      maskClosable={false}
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Steps style={{ marginBottom: 28 }} size="small" current={currentStep}>
        <Step title="基本信息" />
        <Step title="度量信息" />
      </Steps>
      <Form
        {...formLayout}
        form={form}
        initialValues={{
          ...formValRef.current,
        }}
        onValuesChange={(value) => {
          const { isPercent } = value;
          if (isPercent !== undefined) {
            setIsPercentState(isPercent);
          }
        }}
        className={styles.form}
      >
        {renderContent()}
      </Form>
    </Modal>
  );
};

export default MetricInfoCreateForm;
