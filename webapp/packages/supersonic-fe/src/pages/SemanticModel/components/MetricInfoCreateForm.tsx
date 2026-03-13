import React, { useState } from 'react';
import {
  Form,
  Button,
  Modal,
  Steps,
  Input,
  Select,
  Switch,
  InputNumber,
  Result,
  Row,
  Col,
  Space,
  Tooltip,
  Radio,
} from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { SENSITIVE_LEVEL_OPTIONS } from '../constant';
import { formLayout } from '@/components/FormHelper/utils';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import styles from './style.less';
import DimensionAndMetricRelationModal from './DimensionAndMetricRelationModal';
import TableTitleTooltips from '../components/TableTitleTooltips';
import MetricExpressionEditor from './MetricExpressionEditor';
import useMetricForm, { queryParamsTypeParamsKey } from '../hooks/useMetricForm';
import { ISemantic } from '../data';
import { history } from '@umijs/max';

export type CreateFormProps = {
  datasourceId?: number;
  domainId?: number;
  modelId?: number;
  createModalVisible: boolean;
  metricItem?: ISemantic.IMetricItem;
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
  const {
    isEdit,
    form,
    formValRef,
    classMeasureList,
    exprTypeParamsState,
    defineType,
    createNewMetricList,
    fieldList,
    isPercentState,
    isDecimalState,
    hasMeasuresState,
    llmLoading,
    tagOptions,
    metricRelationModalOpenState,
    drillDownDimensionsConfig,
    updateFormVal,
    queryDrillDownDimension,
    saveMetric,
    generatorMetricAlias,
    setDefineType,
    setExprTypeParamsState,
    setMetricRelationModalOpenState,
    setDrillDownDimensions,
    handleFormValuesChange,
  } = useMetricForm({
    modelId,
    domainId,
    datasourceId,
    metricItem,
    onSubmit,
  });

  const [currentStep, setCurrentStep] = useState(0);

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const handleNext = async () => {
    const fieldsValue = await form.validateFields();
    const submitForm = {
      ...formValRef.current,
      ...fieldsValue,
      metricDefineType: defineType,
      [queryParamsTypeParamsKey[defineType]]: exprTypeParamsState[defineType],
    };
    updateFormVal(submitForm);
    if (currentStep < 1) {
      forward();
    } else {
      await saveMetric(submitForm);
    }
  };

  const renderContent = () => {
    if (currentStep === 1) {
      return (
        <div>
          <div
            style={{
              padding: '0 0 20px 24px',
            }}
          >
            <MetricExpressionEditor
              defineType={defineType}
              onDefineTypeChange={setDefineType}
              exprTypeParamsState={exprTypeParamsState}
              onExprTypeParamsChange={setExprTypeParamsState}
              classMeasureList={classMeasureList}
              createNewMetricList={createNewMetricList}
              fieldList={fieldList}
              datasourceId={datasourceId}
            />
          </div>
        </div>
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
          label="英文名称"
          rules={[{ required: true, message: '请输入英文名称' }]}
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
        <FormItem name="classifications" label="分类">
          <Select
            mode="tags"
            placeholder="输入分类名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
            tokenSeparators={[',']}
            maxTagCount={9}
            options={tagOptions}
          />
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

        <Form.Item
          hidden={!!!process.env.SHOW_TAG}
          label={
            <FormItemTitle
              title={`设为标签`}
              subTitle={`如果勾选，代表取值都是一种'标签'，可用作对实体的圈选`}
            />
          }
          name="isTag"
          valuePropName="checked"
          getValueFromEvent={(value) => {
            return value === true ? 1 : 0;
          }}
          getValueProps={(value) => {
            return {
              checked: value === 1,
            };
          }}
        >
          <Switch />
        </Form.Item>

        <FormItem
          label={
            <FormItemTitle
              title={'下钻维度配置'}
              subTitle={'配置下钻维度后，将可以在指标卡中进行下钻'}
            />
          }
        >
          <Button
            type="primary"
            onClick={() => {
              setMetricRelationModalOpenState(true);
            }}
          >
            设 置
          </Button>
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

        {(isPercentState || isDecimalState) && (
          <FormItem
            label={
              <FormItemTitle
                title={'小数位数'}
                subTitle={`对小数位数进行设置，如保留两位，0.021252 -> 0.02${
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
      width={800}
      style={{ top: 48 }}
      // styles={{ padding: '32px 40px 48px' }}
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
            <Step title="表达式" />
          </Steps>
          <Form
            {...formLayout}
            form={form}
            initialValues={{
              ...formValRef.current,
              dataFormatType: '',
            }}
            onValuesChange={handleFormValuesChange}
            className={styles.form}
          >
            {renderContent()}
          </Form>
          <DimensionAndMetricRelationModal
            metricItem={metricItem}
            relationsInitialValue={drillDownDimensionsConfig}
            open={metricRelationModalOpenState}
            onCancel={() => {
              setMetricRelationModalOpenState(false);
            }}
            onSubmit={(relations) => {
              setDrillDownDimensions(relations);
              setMetricRelationModalOpenState(false);
            }}
            onRefreshRelationData={() => {
              queryDrillDownDimension(metricItem?.id);
            }}
          />
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
                history.replace(
                  `/model/domain/manager/${domainId}/${modelId || metricItem?.modelId}/dataSource`,
                );
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
