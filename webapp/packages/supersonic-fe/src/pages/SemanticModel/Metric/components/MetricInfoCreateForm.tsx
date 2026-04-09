import React from 'react';
import {
  Form,
  Button,
  Input,
  Select,
  Radio,
  Switch,
  InputNumber,
  Result,
  Row,
  Col,
  Divider,
  Tooltip,
} from 'antd';

import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { formLayout } from '@/components/FormHelper/utils';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import styles from '../../components/style.less';
import DimensionAndMetricRelationModal from '../../components/DimensionAndMetricRelationModal';
import TableTitleTooltips from '../../components/TableTitleTooltips';
import MetricExpressionEditor from '../../components/MetricExpressionEditor';
import useMetricForm, { queryParamsTypeParamsKey } from '../../hooks/useMetricForm';
import { MetricSettingKey, MetricSettingWording } from '../constants';
import { toModelList, toDomainList } from '@/pages/SemanticModel/utils';
import globalStyles from '@/global.less';

export type CreateFormProps = {
  modelId: number;
  domainId: number;
  datasourceId?: number;
  metricItem: any;
  settingKey: MetricSettingKey;
  onCancel?: () => void;
  onSubmit?: (values: any) => void;
};

const FormItem = Form.Item;
const { TextArea } = Input;
const { Option } = Select;

const MetricInfoCreateForm: React.FC<CreateFormProps> = ({
  modelId,
  domainId,
  datasourceId,
  onCancel,
  settingKey,
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
    onSaveSuccess: () => {
      if (!metricItem?.id) {
        toModelList(domainId, modelId, 'metric');
      }
    },
  });

  const handleSave = async () => {
    const fieldsValue = await form.validateFields();
    const submitForm = {
      ...formValRef.current,
      ...fieldsValue,
      metricDefineType: defineType,
      [queryParamsTypeParamsKey[defineType]]: exprTypeParamsState[defineType],
    };
    updateFormVal(submitForm);

    await saveMetric(submitForm);
  };

  const renderContent = () => {
    return (
      <>
        <div
          style={{
            display: settingKey === MetricSettingKey.SQL_CONFIG ? 'block' : 'none',
            marginLeft: '-24px',
          }}
        >
          <div
            style={{
              padding: '0 0 0px 24px',
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

        <div style={{ display: settingKey === MetricSettingKey.BASIC ? 'block' : 'none' }}>
          <FormItem hidden={true} name="id" label="ID">
            <Input placeholder="id" />
          </FormItem>
          <Row gutter={20}>
            <Col span={12}>
              <FormItem
                name="name"
                label="指标名称"
                rules={[{ required: true, message: '请输入指标名称' }]}
              >
                <Input placeholder="名称不可重复" />
              </FormItem>
            </Col>
            <Col span={12}>
              <FormItem
                name="bizName"
                label="英文名称"
                rules={[{ required: true, message: '请输入英文名称' }]}
              >
                <Input placeholder="名称不可重复" disabled={isEdit} />
              </FormItem>
            </Col>
          </Row>
          <Row gutter={20}>
            <Col span={12}>
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
            </Col>
            <Col span={12}>
              <FormItem name="classifications" label="分类">
                <Select
                  mode="tags"
                  placeholder="支持手动输入及选择"
                  tokenSeparators={[',']}
                  maxTagCount={9}
                  options={tagOptions}
                />
              </FormItem>
            </Col>
          </Row>

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
                    <p>
                      3. 使用场景：说明该指标适用于哪些业务场景，以及如何在这些场景中使用该指标。
                    </p>
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
            <TextArea placeholder="请输入业务口径" style={{ minHeight: 173 }} />
          </FormItem>

          <FormItem label="别名">
            <Row gutter={20}>
              <Col flex="1 1 200px">
                <FormItem name="alias" noStyle>
                  <Select
                    style={{ maxWidth: 500 }}
                    mode="tags"
                    placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
                    tokenSeparators={[',']}
                    maxTagCount={9}
                  />
                </FormItem>
              </Col>
              {isEdit && (
                <Col flex="0 1 75px">
                  <Tooltip title="智能填充将根据指标相关信息，使用大语言模型获取指标别名">
                    <Button
                      type="primary"
                      loading={llmLoading}
                      style={{ top: '5px' }}
                      onClick={() => {
                        generatorMetricAlias();
                      }}
                    >
                      智能填充
                    </Button>
                  </Tooltip>
                </Col>
              )}
            </Row>
          </FormItem>
          <Divider />
          <FormItem
            name="isTag"
            valuePropName="checked"
            hidden={!!!process.env.SHOW_TAG}
            getValueFromEvent={(value) => {
              return value === true ? 1 : 0;
            }}
            getValueProps={(value) => {
              return {
                checked: value === 1,
              };
            }}
          >
            <Row gutter={20}>
              <Col flex="1 1 200px">
                <FormItemTitle
                  title={`设为标签`}
                  subTitle={`如果勾选，代表取值都是一种'标签'，可用作对实体的圈选`}
                />
              </Col>

              <Col flex="0 1 75px">
                <Switch />
              </Col>
            </Row>
            <Divider />
          </FormItem>

          <FormItem>
            <Row gutter={20}>
              <Col flex="1 1 200px">
                <FormItemTitle
                  title={'下钻维度配置'}
                  subTitle={'配置下钻维度后，将可以在指标卡中进行下钻'}
                />
              </Col>

              <Col flex="0 1 75px">
                <Button
                  type="primary"
                  onClick={() => {
                    setMetricRelationModalOpenState(true);
                  }}
                >
                  设 置
                </Button>
              </Col>
            </Row>
          </FormItem>
          <Divider />
          <FormItem label={<FormItemTitle title={'数据格式化'} />} name="dataFormatType">
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
        </div>
      </>
    );
  };

  return (
    <>
      {hasMeasuresState ? (
        <>
          <div className={styles.infoCard}>
            <div className={styles.infoCardTitle}>
              <span style={{ flex: 'auto' }}>{MetricSettingWording[settingKey]}</span>

              <span style={{ flex: 'none' }}>
                <Button type="primary" onClick={handleSave}>
                  保 存
                </Button>
              </span>
            </div>
            <div className={styles.infoCardContainer}>
              <Form
                className={globalStyles.supersonicForm}
                {...formLayout}
                form={form}
                initialValues={{
                  ...formValRef.current,
                  dataFormatType: '',
                }}
                onValuesChange={handleFormValuesChange}
              >
                {renderContent()}
              </Form>
            </div>
          </div>
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
          style={{ background: '#fff' }}
          status="warning"
          subTitle="当前数据模型缺少度量，无法创建指标。请前往模型配置中，将字段设置为度量"
          extra={
            <Button
              type="primary"
              key="console"
              onClick={() => {
                toDomainList(domainId, 'menuKey');
                onCancel?.();
              }}
            >
              去创建
            </Button>
          }
        />
      )}
    </>
  );
};

export default MetricInfoCreateForm;
