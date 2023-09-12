import React, { useEffect, useRef, useState } from 'react';
import {
  Form,
  Button,
  Modal,
  Steps,
  Input,
  Select,
  Radio,
  Switch,
  InputNumber,
  message,
  Result,
  Row,
  Col,
  Space,
  Tooltip,
} from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import MetricMeasuresFormTable from './MetricMeasuresFormTable';
import { SENSITIVE_LEVEL_OPTIONS } from '../constant';
import { formLayout } from '@/components/FormHelper/utils';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import styles from './style.less';
import { getMeasureListByModelId } from '../service';
import TableTitleTooltips from '../components/TableTitleTooltips';
import { creatExprMetric, updateExprMetric, mockMetricAlias } from '../service';
import { ISemantic } from '../data';
import { history } from 'umi';

export type CreateFormProps = {
  datasourceId?: number;
  domainId: number;
  modelId: number;
  createModalVisible: boolean;
  metricItem: any;
  onCancel?: () => void;
  onSubmit?: (values: any) => void;
};

const { Step } = Steps;
const FormItem = Form.Item;
const { TextArea } = Input;
const { Option } = Select;

const MetricInfoCreateForm: React.FC<CreateFormProps> = ({
  datasourceId,
  domainId,
  modelId,
  onCancel,
  createModalVisible,
  metricItem,
  onSubmit,
}) => {
  const isEdit = !!metricItem?.id;
  const [currentStep, setCurrentStep] = useState(0);
  const formValRef = useRef({} as any);
  const [form] = Form.useForm();
  const updateFormVal = (val: any) => {
    const formVal = {
      ...formValRef.current,
      ...val,
    };
    formValRef.current = formVal;
  };

  const [classMeasureList, setClassMeasureList] = useState<ISemantic.IMeasure[]>([]);

  const [exprTypeParamsState, setExprTypeParamsState] = useState<ISemantic.IMeasure[]>([]);

  const [exprSql, setExprSql] = useState<string>('');

  const [isPercentState, setIsPercentState] = useState<boolean>(false);
  const [isDecimalState, setIsDecimalState] = useState<boolean>(false);
  const [hasMeasuresState, setHasMeasuresState] = useState<boolean>(true);
  const [llmLoading, setLlmLoading] = useState<boolean>(false);

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const queryClassMeasureList = async () => {
    const { code, data } = await getMeasureListByModelId(modelId);
    if (code === 200) {
      setClassMeasureList(data);
      if (datasourceId) {
        const hasMeasures = data.some(
          (item: ISemantic.IMeasure) => item.datasourceId === datasourceId,
        );
        setHasMeasuresState(hasMeasures);
      }
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
    };
    updateFormVal(submitForm);
    if (currentStep < 1) {
      forward();
    } else {
      await saveMetric(submitForm);
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
      alias,
    } = metricItem as any;
    const isPercent = dataFormatType === 'percent';
    const isDecimal = dataFormatType === 'decimal';
    const initValue = {
      id,
      name,
      bizName,
      sensitiveLevel,
      description,
      // isPercent,
      dataFormatType: dataFormatType || '',
      alias: alias && alias.trim() ? alias.split(',') : [],
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
    setIsDecimalState(isDecimal);
  };

  useEffect(() => {
    if (isEdit) {
      initData();
    }
  }, [metricItem]);

  const saveMetric = async (fieldsValue: any) => {
    const queryParams = {
      modelId: isEdit ? metricItem.modelId : modelId,
      ...fieldsValue,
    };
    const { typeParams, alias, dataFormatType } = queryParams;
    queryParams.alias = Array.isArray(alias) ? alias.join(',') : '';
    if (!typeParams?.expr) {
      message.error('请输入度量表达式');
      return;
    }
    if (!dataFormatType) {
      delete queryParams.dataFormat;
    }
    if (!(Array.isArray(typeParams?.measures) && typeParams.measures.length > 0)) {
      message.error('请添加一个度量');
      return;
    }
    let saveMetricQuery = creatExprMetric;
    if (queryParams.id) {
      saveMetricQuery = updateExprMetric;
    }
    const { code, msg } = await saveMetricQuery(queryParams);
    if (code === 200) {
      message.success('编辑指标成功');
      onSubmit?.(queryParams);
      return;
    }
    message.error(msg);
  };

  const generatorMetricAlias = async () => {
    setLlmLoading(true);
    const { code, data } = await mockMetricAlias({ ...metricItem });
    const formAlias = form.getFieldValue('alias');
    setLlmLoading(false);
    if (code === 200) {
      form.setFieldValue('alias', Array.from(new Set([...formAlias, ...data])));
    } else {
      message.error('大语言模型解析异常');
    }
  };

  const renderContent = () => {
    if (currentStep === 1) {
      return (
        <MetricMeasuresFormTable
          datasourceId={datasourceId}
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
          label="指标名称"
          rules={[{ required: true, message: '请输入指标名称' }]}
        >
          <Input placeholder="名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label="字段名称"
          rules={[{ required: true, message: '请输入字段名称' }]}
        >
          <Input placeholder="名称不可重复" disabled={isEdit} />
        </FormItem>
        <FormItem label="别名">
          <Row>
            <Col flex="1 1 200px">
              <FormItem name="alias" noStyle>
                <Select
                  mode="tags"
                  placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
                  tokenSeparators={[',']}
                  maxTagCount={9}
                />
              </FormItem>
            </Col>
            {isEdit && (
              <Col flex="0 1 75px">
                <Button
                  type="link"
                  loading={llmLoading}
                  size="small"
                  style={{ top: '2px' }}
                  onClick={() => {
                    generatorMetricAlias();
                  }}
                >
                  <Space>
                    智能填充
                    <Tooltip title="智能填充将根据指标相关信息，使用大语言模型获取指标别名">
                      <InfoCircleOutlined />
                    </Tooltip>
                  </Space>
                </Button>
              </Col>
            )}
          </Row>
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
          label={
            <TableTitleTooltips
              title="业务口径"
              overlayInnerStyle={{ width: 600 }}
              tooltips={
                <>
                  <p>
                    在录入指标时，请务必详细填写指标口径。口径描述对于理解指标的含义、计算方法和使用场景至关重要。一个清晰、准确的口径描述可以帮助其他用户更好地理解和使用该指标，避免因为误解而导致错误的数据分析和决策。在填写口径时，建议包括以下信息：
                  </p>
                  <p>1. 指标的计算方法：详细说明指标是如何计算的，包括涉及的公式、计算步骤等。</p>
                  <p>2. 数据来源：描述指标所依赖的数据来源，包括数据表、字段等信息。</p>
                  <p>3. 使用场景：说明该指标适用于哪些业务场景，以及如何在这些场景中使用该指标。</p>
                  <p>4. 任何其他相关信息：例如数据更新频率、数据质量要求等。</p>
                  <p>
                    请确保口径描述清晰、简洁且易于理解，以便其他用户能够快速掌握指标的核心要点。
                  </p>
                </>
              }
            />
          }
          rules={[{ required: true, message: '请输入业务口径' }]}
        >
          <TextArea placeholder="请输入业务口径" />
        </FormItem>
        <FormItem
          label={
            <FormItemTitle
              title={'数据格式化'}
              // subTitle={'开启后，指标数据展示时会根据配置进行格式化，如0.02 -> 2%'}
            />
          }
          name="dataFormatType"
        >
          <Radio.Group buttonStyle="solid" size="middle">
            <Radio.Button value="">默认</Radio.Button>
            <Radio.Button value="decimal">小数</Radio.Button>
            <Radio.Button value="percent">百分比</Radio.Button>
          </Radio.Group>
        </FormItem>

        {/* <FormItem
          label={
            <FormItemTitle
              title={'是否展示为百分比'}
              subTitle={'开启后，指标数据展示时会根据配置进行格式化，如0.02 -> 2%'}
            />
          }
          name="isPercent"
          valuePropName="checked"
        >
          <Switch
            onChange={(checked) => {
              form.setFieldValue(['dataFormat', 'needMultiply100'], checked);
            }}
          />
        </FormItem> */}
        {(isPercentState || isDecimalState) && (
          <FormItem
            label={
              <FormItemTitle
                title={'小数位数'}
                subTitle={`对小数位数进行设置，如保留两位，0.021252 -> 2.12${
                  isPercentState ? '%' : ''
                }`}
              />
            }
            name={['dataFormat', 'decimalPlaces']}
          >
            <InputNumber placeholder="请输入需要保留小数位数" style={{ width: '300px' }} />
          </FormItem>
        )}
        {isPercentState && (
          <>
            <FormItem
              label={
                <FormItemTitle
                  title={'原始值是否乘以100'}
                  subTitle={'如 原始值0.001 ->展示值0.1% '}
                />
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
    if (!hasMeasuresState) {
      return <Button onClick={onCancel}>取消</Button>;
    }
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
      {hasMeasuresState ? (
        <>
          <Steps style={{ marginBottom: 28 }} size="small" current={currentStep}>
            <Step title="基本信息" />
            <Step title="度量信息" />
          </Steps>
          <Form
            {...formLayout}
            form={form}
            initialValues={{
              ...formValRef.current,
              dataFormatType: '',
            }}
            onValuesChange={(value, values: any) => {
              const { isPercent, dataFormatType } = values;
              // if (isPercent !== undefined) {
              //   setIsPercentState(isPercent);
              // }
              if (dataFormatType === 'percent') {
                setIsPercentState(true);
                setIsDecimalState(false);
              }
              if (dataFormatType === 'decimal') {
                setIsPercentState(false);
                setIsDecimalState(true);
              }
              if (!dataFormatType) {
                setIsPercentState(false);
                setIsDecimalState(false);
              }
            }}
            className={styles.form}
          >
            {renderContent()}
          </Form>
        </>
      ) : (
        <Result
          status="warning"
          subTitle="当前数据源缺少度量，无法创建指标。请前往数据源配置中，将字段设置为度量"
          extra={
            <Button
              type="primary"
              key="console"
              onClick={() => {
                history.replace(`/model/${domainId}/${modelId}/dataSource`);
                onCancel?.();
              }}
            >
              去创建
            </Button>
          }
        />
      )}
    </Modal>
  );
};

export default MetricInfoCreateForm;
