import React, { useEffect, useRef, useState } from 'react';
import {
  Form,
  Button,
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
  Divider,
  Tooltip,
  Tag,
} from 'antd';

import MetricMeasuresFormTable from '../../components/MetricMeasuresFormTable';
import { SENSITIVE_LEVEL_OPTIONS, METRIC_DEFINE_TYPE, TAG_DEFINE_TYPE } from '../../constant';
import { formLayout } from '@/components/FormHelper/utils';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import styles from '../../components/style.less';
import {
  getMetricsToCreateNewMetric,
  getModelDetail,
  getDrillDownDimension,
  batchCreateTag,
  batchDeleteTag,
} from '../../service';
import MetricMetricFormTable from '../../components/MetricMetricFormTable';
import MetricFieldFormTable from '../../components/MetricFieldFormTable';
import DimensionAndMetricRelationModal from '../../components/DimensionAndMetricRelationModal';
import TableTitleTooltips from '../../components/TableTitleTooltips';
import { createMetric, updateMetric, mockMetricAlias, getMetricTags } from '../../service';
import { MetricSettingKey, MetricSettingWording } from '../constants';
import { ISemantic } from '../../data';
import { history } from '@umijs/max';
import { toDomainList, toModelList } from '@/pages/SemanticModel/utils';
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

const queryParamsTypeParamsKey = {
  [METRIC_DEFINE_TYPE.MEASURE]: 'metricDefineByMeasureParams',
  [METRIC_DEFINE_TYPE.METRIC]: 'metricDefineByMetricParams',
  [METRIC_DEFINE_TYPE.FIELD]: 'metricDefineByFieldParams',
};

const MetricInfoCreateForm: React.FC<CreateFormProps> = ({
  modelId,
  domainId,
  datasourceId,
  onCancel,
  settingKey,
  metricItem,
  onSubmit,
}) => {
  const isEdit = !!metricItem?.id;
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

  const [exprTypeParamsState, setExprTypeParamsState] = useState<{
    [METRIC_DEFINE_TYPE.MEASURE]: ISemantic.IMeasureTypeParams;
    [METRIC_DEFINE_TYPE.METRIC]: ISemantic.IMetricTypeParams;
    [METRIC_DEFINE_TYPE.FIELD]: ISemantic.IFieldTypeParams;
  }>({
    [METRIC_DEFINE_TYPE.MEASURE]: {
      measures: [],
      expr: '',
    },
    [METRIC_DEFINE_TYPE.METRIC]: {
      metrics: [],
      expr: '',
    },
    [METRIC_DEFINE_TYPE.FIELD]: {
      fields: [],
      expr: '',
    },
  } as any);

  const [defineType, setDefineType] = useState(METRIC_DEFINE_TYPE.MEASURE);

  const [createNewMetricList, setCreateNewMetricList] = useState<ISemantic.IMetricItem[]>([]);
  const [fieldList, setFieldList] = useState<ISemantic.IFieldTypeParamsItem[]>([]);
  const [isPercentState, setIsPercentState] = useState<boolean>(false);
  const [isDecimalState, setIsDecimalState] = useState<boolean>(false);
  const [hasMeasuresState, setHasMeasuresState] = useState<boolean>(true);
  const [llmLoading, setLlmLoading] = useState<boolean>(false);

  const [tagOptions, setTagOptions] = useState<{ label: string; value: string }[]>([]);

  const [metricRelationModalOpenState, setMetricRelationModalOpenState] = useState<boolean>(false);

  const [drillDownDimensions, setDrillDownDimensions] = useState<
    ISemantic.IDrillDownDimensionItem[]
  >([]);

  const [drillDownDimensionsConfig, setDrillDownDimensionsConfig] = useState<
    ISemantic.IDrillDownDimensionItem[]
  >([]);

  const queryModelDetail = async () => {
    if (!modelId) {
      return;
    }
    const { code, data } = await getModelDetail({ modelId });
    if (code === 200) {
      if (Array.isArray(data?.modelDetail?.fields)) {
        if (Array.isArray(metricItem?.metricDefineByFieldParams?.fields)) {
          const fieldList = data.modelDetail.fields.map((item: ISemantic.IFieldTypeParamsItem) => {
            const { fieldName } = item;
            if (
              metricItem?.metricDefineByFieldParams?.fields.find(
                (measureParamsItem: ISemantic.IFieldTypeParamsItem) =>
                  measureParamsItem.fieldName === fieldName,
              )
            ) {
              return {
                ...item,
                orderNumber: 9999,
              };
            }
            return {
              ...item,
              orderNumber: 0,
            };
          });

          const sortList = fieldList.sort(
            (
              a: ISemantic.IFieldTypeParamsItem & { orderNumber: number },
              b: ISemantic.IFieldTypeParamsItem & { orderNumber: number },
            ) => b.orderNumber - a.orderNumber,
          );
          setFieldList(sortList);
        } else {
          setFieldList(data.modelDetail.fields);
        }
      }
      if (Array.isArray(data?.modelDetail?.measures)) {
        if (Array.isArray(metricItem?.metricDefineByMeasureParams?.measures)) {
          const measureList = data.modelDetail.measures.map((item: ISemantic.IMeasure) => {
            const { bizName } = item;
            if (
              metricItem?.metricDefineByMeasureParams?.measures.find(
                (measureParamsItem: ISemantic.IMeasure) => measureParamsItem.bizName === bizName,
              )
            ) {
              return {
                ...item,
                orderNumber: 9999,
              };
            }
            return {
              ...item,
              orderNumber: 0,
            };
          });
          const sortMeasureList = measureList.sort(
            (
              a: ISemantic.IMeasure & { orderNumber: number },
              b: ISemantic.IMeasure & { orderNumber: number },
            ) => b.orderNumber - a.orderNumber,
          );
          setClassMeasureList(sortMeasureList);
        } else {
          setClassMeasureList(data.modelDetail.measures);
        }

        if (datasourceId) {
          const hasMeasures = data.some(
            (item: ISemantic.IMeasure) => item.datasourceId === datasourceId,
          );
          setHasMeasuresState(hasMeasures);
        }
        return;
      }
    }
    setClassMeasureList([]);
  };

  const queryDrillDownDimension = async (metricId: number) => {
    const { code, data, msg } = await getDrillDownDimension(metricId);
    if (code === 200 && Array.isArray(data)) {
      setDrillDownDimensionsConfig(data);
    }
    if (code !== 200) {
      message.error(msg);
    }
    return [];
  };

  useEffect(() => {
    queryModelDetail();
    queryMetricsToCreateNewMetric();
    queryMetricTags();
  }, [metricItem]);

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

  const initData = () => {
    const {
      id,
      name,
      bizName,
      description,
      sensitiveLevel,
      typeParams,
      isTag,
      dataFormat,
      dataFormatType,
      alias,
      classifications,
      metricDefineType,
      metricDefineByMeasureParams,
      metricDefineByMetricParams,
      metricDefineByFieldParams,
    } = metricItem;
    const isPercent = dataFormatType === 'percent';
    const isDecimal = dataFormatType === 'decimal';
    const initValue = {
      id,
      name,
      bizName,
      sensitiveLevel,
      description,
      classifications,
      isTag,
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
    if (metricDefineType === METRIC_DEFINE_TYPE.MEASURE) {
      const { measures, expr } = metricDefineByMeasureParams || {};
      setExprTypeParamsState({
        ...exprTypeParamsState,
        [METRIC_DEFINE_TYPE.MEASURE]: {
          measures: measures || [],
          expr: expr || '',
        },
      });
    }
    if (metricDefineType === METRIC_DEFINE_TYPE.METRIC) {
      const { metrics, expr } = metricDefineByMetricParams || {};
      setExprTypeParamsState({
        ...exprTypeParamsState,
        [METRIC_DEFINE_TYPE.METRIC]: {
          metrics: metrics || [],
          expr: expr || '',
        },
      });
    }
    if (metricDefineType === METRIC_DEFINE_TYPE.FIELD) {
      const { fields, expr } = metricDefineByFieldParams || {};
      setExprTypeParamsState({
        ...exprTypeParamsState,
        [METRIC_DEFINE_TYPE.FIELD]: {
          fields: fields || [],
          expr: expr || '',
        },
      });
    }
    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
    setDefineType(metricDefineType);
    setIsPercentState(isPercent);
    setIsDecimalState(isDecimal);
    queryDrillDownDimension(metricItem?.id);
  };

  useEffect(() => {
    if (isEdit) {
      initData();
    }
  }, [metricItem]);

  const isEmptyConditions = (
    metricDefineType: METRIC_DEFINE_TYPE,
    metricDefineParams:
      | ISemantic.IMeasureTypeParams
      | ISemantic.IMetricTypeParams
      | ISemantic.IFieldTypeParams,
  ) => {
    if (metricDefineType === METRIC_DEFINE_TYPE.MEASURE) {
      const { measures } = (metricDefineParams as ISemantic.IMeasureTypeParams) || {};
      if (!(Array.isArray(measures) && measures.length > 0)) {
        message.error('请添加一个度量');
        return true;
      }
    }
    if (metricDefineType === METRIC_DEFINE_TYPE.METRIC) {
      const { metrics } = (metricDefineParams as ISemantic.IMetricTypeParams) || {};
      if (!(Array.isArray(metrics) && metrics.length > 0)) {
        message.error('请添加一个指标');
        return true;
      }
    }
    if (metricDefineType === METRIC_DEFINE_TYPE.FIELD) {
      const { fields } = (metricDefineParams as ISemantic.IFieldTypeParams) || {};
      if (!(Array.isArray(fields) && fields.length > 0)) {
        message.error('请添加一个字段');
        return true;
      }
    }
    return false;
  };

  const saveMetric = async (fieldsValue: any) => {
    const queryParams = {
      modelId: isEdit ? metricItem.modelId : modelId,
      relateDimension: {
        ...(metricItem?.relateDimension || {}),
        drillDownDimensions,
      },
      ...fieldsValue,
    };
    const { alias, dataFormatType } = queryParams;
    queryParams.alias = Array.isArray(alias) ? alias.join(',') : '';
    if (!queryParams[queryParamsTypeParamsKey[defineType]]?.expr) {
      message.error('请输入度量表达式');
      return;
    }
    if (!dataFormatType) {
      delete queryParams.dataFormat;
    }
    if (isEmptyConditions(defineType, queryParams[queryParamsTypeParamsKey[defineType]])) {
      return;
    }

    let saveMetricQuery = createMetric;
    if (queryParams.id) {
      saveMetricQuery = updateMetric;
    }
    const { code, msg, data } = await saveMetricQuery(queryParams);
    if (code === 200) {
      if (queryParams.isTag) {
        queryBatchExportTag(data.id || metricItem?.id);
      }

      if (metricItem?.id && !queryParams.isTag) {
        queryBatchDelete(metricItem);
      }
      message.success('编辑指标成功');
      onSubmit?.(queryParams);
      if (!isEdit) {
        toModelList(domainId, modelId!, 'metric');
      }
      return;
    }
    message.error(msg);
  };

  const queryBatchDelete = async (metricItem: ISemantic.IMetricItem) => {
    const { code, msg } = await batchDeleteTag([
      {
        itemIds: [metricItem.id],
        tagDefineType: TAG_DEFINE_TYPE.METRIC,
      },
    ]);
    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const queryBatchExportTag = async (id: number) => {
    const { code, msg } = await batchCreateTag([
      { itemId: id, tagDefineType: TAG_DEFINE_TYPE.METRIC },
    ]);

    if (code === 200) {
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

  const queryMetricTags = async () => {
    const { code, data } = await getMetricTags();
    if (code === 200) {
      setTagOptions(
        Array.isArray(data)
          ? data.map((tag: string) => {
              return { label: tag, value: tag };
            })
          : [],
      );
    } else {
      message.error('获取指标标签失败');
    }
  };
  const queryMetricsToCreateNewMetric = async () => {
    if (!metricItem?.id) {
      return;
    }
    const { code, data } = await getMetricsToCreateNewMetric({
      modelId: modelId || metricItem?.modelId,
    });
    if (code === 200) {
      if (Array.isArray(metricItem?.metricDefineByMetricParams?.metrics)) {
        const fieldList = data.map((item: ISemantic.IMetricTypeParamsItem) => {
          const { bizName } = item;
          if (
            metricItem?.metricDefineByMetricParams?.metrics.find(
              (measureParamsItem: ISemantic.IMetricTypeParamsItem) =>
                measureParamsItem.bizName === bizName,
            )
          ) {
            return {
              ...item,
              orderNumber: 9999,
            };
          }
          return {
            ...item,
            orderNumber: 0,
          };
        });

        const sortList = fieldList.sort(
          (
            a: ISemantic.IMetricTypeParamsItem & { orderNumber: number },
            b: ISemantic.IMetricTypeParamsItem & { orderNumber: number },
          ) => b.orderNumber - a.orderNumber,
        );
        setCreateNewMetricList(sortList);
      } else {
        setCreateNewMetricList(data);
      }
    } else {
      message.error('获取指标标签失败');
    }
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
            <Space size={20}>
              <Radio.Group
                buttonStyle="solid"
                value={defineType}
                onChange={(e) => {
                  setDefineType(e.target.value);
                }}
              >
                <Radio.Button value={METRIC_DEFINE_TYPE.MEASURE}>按度量</Radio.Button>
                <Radio.Button value={METRIC_DEFINE_TYPE.METRIC}>按指标</Radio.Button>
                <Radio.Button value={METRIC_DEFINE_TYPE.FIELD}>按字段</Radio.Button>
              </Radio.Group>
              {defineType === METRIC_DEFINE_TYPE.METRIC && (
                <p className={styles.desc}>
                  基于
                  <Tag color="#2499ef14" className={styles.markerTag}>
                    已有
                  </Tag>
                  指标来衍生新的指标
                </p>
              )}
            </Space>
          </div>
          {defineType === METRIC_DEFINE_TYPE.MEASURE && (
            <>
              <MetricMeasuresFormTable
                datasourceId={datasourceId}
                typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.MEASURE]}
                measuresList={classMeasureList}
                onFieldChange={(measures: ISemantic.IMeasure[]) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [METRIC_DEFINE_TYPE.MEASURE]: {
                        ...prevState[METRIC_DEFINE_TYPE.MEASURE],
                        measures,
                      },
                    };
                  });
                }}
                onSqlChange={(expr: string) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [METRIC_DEFINE_TYPE.MEASURE]: {
                        ...prevState[METRIC_DEFINE_TYPE.MEASURE],
                        expr,
                      },
                    };
                  });
                }}
              />
            </>
          )}
          {defineType === METRIC_DEFINE_TYPE.METRIC && (
            <>
              <MetricMetricFormTable
                typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.METRIC]}
                metricList={createNewMetricList}
                onFieldChange={(metrics: ISemantic.IMetricTypeParamsItem[]) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [METRIC_DEFINE_TYPE.METRIC]: {
                        ...prevState[METRIC_DEFINE_TYPE.METRIC],
                        metrics,
                      },
                    };
                  });
                }}
                onSqlChange={(expr: string) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [METRIC_DEFINE_TYPE.METRIC]: {
                        ...prevState[METRIC_DEFINE_TYPE.METRIC],
                        expr,
                      },
                    };
                  });
                }}
              />
            </>
          )}
          {defineType === METRIC_DEFINE_TYPE.FIELD && (
            <>
              <MetricFieldFormTable
                typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.FIELD]}
                fieldList={fieldList}
                onFieldChange={(fields: ISemantic.IFieldTypeParamsItem[]) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [METRIC_DEFINE_TYPE.FIELD]: {
                        ...prevState[METRIC_DEFINE_TYPE.FIELD],
                        fields,
                      },
                    };
                  });
                }}
                onSqlChange={(expr: string) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [METRIC_DEFINE_TYPE.FIELD]: {
                        ...prevState[METRIC_DEFINE_TYPE.FIELD],
                        expr,
                      },
                    };
                  });
                }}
              />
            </>
          )}
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
                {/* <Button
                  size="middle"
                  type="link"
                  key="backListBtn"
                  onClick={() => {
                    history.back();
                  }}
                >
                  <Space>
                    <ArrowLeftOutlined />
                    返回列表页
                  </Space>
                </Button> */}
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
                onValuesChange={(value, values: any) => {
                  const { dataFormatType } = values;
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
