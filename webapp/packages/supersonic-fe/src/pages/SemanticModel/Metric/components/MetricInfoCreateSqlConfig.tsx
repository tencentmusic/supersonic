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
import { history } from 'umi';

export type CreateFormProps = {
  datasourceId?: number;
  domainId: number;
  modelId: number;
  metricItem: any;
  settingKey: MetricSettingKey;
  onCancel?: () => void;
  onSubmit?: (values: any) => void;
};

const queryParamsTypeParamsKey = {
  [METRIC_DEFINE_TYPE.MEASURE]: 'metricDefineByMeasureParams',
  [METRIC_DEFINE_TYPE.METRIC]: 'metricDefineByMetricParams',
  [METRIC_DEFINE_TYPE.FIELD]: 'metricDefineByFieldParams',
};

const MetricInfoCreateSqlConfig: React.FC<CreateFormProps> = ({
  datasourceId,
  modelId,
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

  // const [exprTypeParamsState, setExprTypeParamsState] = useState<ISemantic.IMeasure[]>([]);

  const [defineType, setDefineType] = useState(METRIC_DEFINE_TYPE.MEASURE);

  const [createNewMetricList, setCreateNewMetricList] = useState<ISemantic.IMetricItem[]>([]);
  const [fieldList, setFieldList] = useState<ISemantic.IFieldTypeParamsItem[]>([]);

  const [drillDownDimensions, setDrillDownDimensions] = useState<
    ISemantic.IDrillDownDimensionItem[]
  >([]);

  const [drillDownDimensionsConfig, setDrillDownDimensionsConfig] = useState<
    ISemantic.IDrillDownDimensionItem[]
  >([]);

  const queryModelDetail = async () => {
    const { code, data } = await getModelDetail({ modelId: modelId || metricItem?.modelId });
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
  }, []);

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

  const queryMetricsToCreateNewMetric = async () => {
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

  return (
    <div>
      <div
        style={{
          padding: '0 0 20px 24px',
          // borderBottom: '1px solid #eee',
        }}
      >
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
      </div>
      {defineType === METRIC_DEFINE_TYPE.MEASURE && (
        <>
          <MetricMeasuresFormTable
            datasourceId={datasourceId}
            typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.MEASURE]}
            measuresList={classMeasureList}
            onFieldChange={(measures: ISemantic.IMeasure[]) => {
              // setClassMeasureList(measures);
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
          <p className={styles.desc}>
            基于
            <Tag color="#2499ef14" className={styles.markerTag}>
              已有
            </Tag>
            指标来衍生新的指标
          </p>

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
  );
};

export default MetricInfoCreateSqlConfig;
