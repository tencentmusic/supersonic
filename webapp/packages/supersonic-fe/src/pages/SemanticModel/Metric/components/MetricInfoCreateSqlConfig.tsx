import React, { useEffect, useRef, useState } from 'react';
import {
  Form,
  Radio,
  message,
  Tag,
} from 'antd';

import MetricMeasuresFormTable from '../../components/MetricMeasuresFormTable';
import { METRIC_DEFINE_TYPE } from '../../constant';
import styles from '../../components/style.less';
import {
  getMetricsToCreateNewMetric,
  getModelDetail,
  getDrillDownDimension,
} from '../../service';
import MetricMetricFormTable from '../../components/MetricMetricFormTable';
import MetricFieldFormTable from '../../components/MetricFieldFormTable';
import { MetricSettingKey } from '../constants';
import { ISemantic } from '../../data';

export type CreateFormProps = {
  datasourceId?: number;
  domainId: number;
  modelId: number;
  metricItem: any;
  settingKey: MetricSettingKey;
  onCancel?: () => void;
  onSubmit?: (values: any) => void;
};

const MetricInfoCreateSqlConfig: React.FC<CreateFormProps> = ({
  datasourceId,
  modelId,
  metricItem,
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

  // const [exprTypeParamsState, setExprTypeParamsState] = useState<ISemantic.IMeasure[]>([]);

  const [defineType, setDefineType] = useState(METRIC_DEFINE_TYPE.MEASURE);

  const [createNewMetricList, setCreateNewMetricList] = useState<ISemantic.IMetricItem[]>([]);
  const [fieldList, setFieldList] = useState<ISemantic.IFieldTypeParamsItem[]>([]);

  const [, setDrillDownDimensionsConfig] = useState<
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
