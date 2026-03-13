import { useEffect, useRef, useState } from 'react';
import { Form, message } from 'antd';
import { METRIC_DEFINE_TYPE, TAG_DEFINE_TYPE } from '../constant';
import {
  getMetricsToCreateNewMetric,
  getModelDetail,
  getDrillDownDimension,
  batchCreateTag,
  batchDeleteTag,
  createMetric,
  updateMetric,
  mockMetricAlias,
  getMetricTags,
} from '../service';
import { ISemantic } from '../data';

const queryParamsTypeParamsKey = {
  [METRIC_DEFINE_TYPE.MEASURE]: 'metricDefineByMeasureParams',
  [METRIC_DEFINE_TYPE.METRIC]: 'metricDefineByMetricParams',
  [METRIC_DEFINE_TYPE.FIELD]: 'metricDefineByFieldParams',
};

export { queryParamsTypeParamsKey };

export interface UseMetricFormParams {
  modelId?: number;
  domainId?: number;
  datasourceId?: number;
  metricItem?: ISemantic.IMetricItem;
  onSubmit?: (values: any) => void;
  onSaveSuccess?: () => void;
}

const useMetricForm = ({
  modelId,
  domainId,
  datasourceId,
  metricItem,
  onSubmit,
  onSaveSuccess,
}: UseMetricFormParams) => {
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
    const resolvedModelId = modelId || metricItem?.modelId;
    if (!resolvedModelId) {
      return;
    }
    const { code, data } = await getModelDetail({ modelId: resolvedModelId });
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
    } = metricItem as ISemantic.IMetricItem;
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
      setExprTypeParamsState((prev) => ({
        ...prev,
        [METRIC_DEFINE_TYPE.MEASURE]: {
          measures: measures || [],
          expr: expr || '',
        },
      }));
    }
    if (metricDefineType === METRIC_DEFINE_TYPE.METRIC) {
      const { metrics, expr } = metricDefineByMetricParams || {};
      setExprTypeParamsState((prev) => ({
        ...prev,
        [METRIC_DEFINE_TYPE.METRIC]: {
          metrics: metrics || [],
          expr: expr || '',
        },
      }));
    }
    if (metricDefineType === METRIC_DEFINE_TYPE.FIELD) {
      const { fields, expr } = metricDefineByFieldParams || {};
      setExprTypeParamsState((prev) => ({
        ...prev,
        [METRIC_DEFINE_TYPE.FIELD]: {
          fields: fields || [],
          expr: expr || '',
        },
      }));
    }
    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
    setDefineType(metricDefineType);
    setIsPercentState(isPercent);
    setIsDecimalState(isDecimal);
    queryDrillDownDimension(metricItem?.id!);
  };

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
      modelId: isEdit ? metricItem!.modelId : modelId,
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

    if (defineType === METRIC_DEFINE_TYPE.MEASURE) {
      const { bizName, name, metricDefineByMeasureParams } = queryParams;
      queryParams[queryParamsTypeParamsKey[METRIC_DEFINE_TYPE.MEASURE]].measures =
        metricDefineByMeasureParams.measures.map((item: ISemantic.IMeasure) => {
          return item.bizName === bizName && name ? { ...item, name } : item;
        });
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
      onSaveSuccess?.();
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

  const handleFormValuesChange = (_value: any, values: any) => {
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
  };

  useEffect(() => {
    queryModelDetail();
    queryMetricsToCreateNewMetric();
    queryMetricTags();
  }, [metricItem]);

  useEffect(() => {
    if (isEdit) {
      initData();
    }
  }, [metricItem]);

  return {
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
    drillDownDimensions,
    drillDownDimensionsConfig,
    updateFormVal,
    queryModelDetail,
    queryDrillDownDimension,
    initData,
    isEmptyConditions,
    saveMetric,
    queryBatchDelete,
    queryBatchExportTag,
    generatorMetricAlias,
    queryMetricTags,
    queryMetricsToCreateNewMetric,
    setDefineType,
    setExprTypeParamsState,
    setMetricRelationModalOpenState,
    setDrillDownDimensions,
    handleFormValuesChange,
    queryParamsTypeParamsKey,
  };
};

export default useMetricForm;
