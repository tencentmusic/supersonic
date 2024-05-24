import React, { useEffect, useRef, useState } from 'react';
import { Form, Button, Modal, Steps, Input, Select, Radio, message, Tag } from 'antd';

import { SENSITIVE_LEVEL_OPTIONS, TAG_DEFINE_TYPE } from '../../constant';
import { formLayout } from '@/components/FormHelper/utils';
import styles from '../../components/style.less';
import { getModelDetail } from '../../service';
import { isArrayOfValues } from '@/utils/utils';
import MetricFieldFormTable from '../../components/MetricFieldFormTable';
import TagDimensionFormTable from '../../components/TagDimensionFormTable';
import MetricMetricFormTable from '../../components/MetricMetricFormTable';
import TableTitleTooltips from '../../components/TableTitleTooltips';
import { createTag, updateTag, getDimensionList, queryMetric } from '../../service';
import { ISemantic } from '../../data';

export type CreateFormProps = {
  datasourceId?: number;
  modelId?: number;
  createModalVisible: boolean;
  tagItem?: ISemantic.ITagItem;
  onCancel?: () => void;
  onSubmit?: (values: any) => void;
};

const { Step } = Steps;
const FormItem = Form.Item;
const { TextArea } = Input;
const { Option } = Select;

const TagInfoCreateForm: React.FC<CreateFormProps> = ({
  modelId,
  onCancel,
  createModalVisible,
  tagItem,
  onSubmit,
}) => {
  const isEdit = !!tagItem?.id;
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

  const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>([]);

  const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>([]);
  const [metricMap, setMetricMap] = useState<Record<string, ISemantic.IMetricItem>>({});

  const [exprTypeParamsState, setExprTypeParamsState] = useState<{
    [TAG_DEFINE_TYPE.DIMENSION]: ISemantic.ITagDefineParams;
    [TAG_DEFINE_TYPE.METRIC]: ISemantic.ITagDefineParams;
    [TAG_DEFINE_TYPE.FIELD]: ISemantic.ITagDefineParams;
  }>({
    [TAG_DEFINE_TYPE.DIMENSION]: {
      dependencies: [],
      expr: '',
    },
    [TAG_DEFINE_TYPE.METRIC]: {
      dependencies: [],
      expr: '',
    },
    [TAG_DEFINE_TYPE.FIELD]: {
      dependencies: [],
      expr: '',
    },
  } as any);

  const [defineType, setDefineType] = useState<TAG_DEFINE_TYPE>(TAG_DEFINE_TYPE.DIMENSION);

  const [fieldList, setFieldList] = useState<ISemantic.IFieldTypeParamsItem[]>([]);

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const queryModelDetail = async (modelId) => {
    const { code, data } = await getModelDetail({ modelId });
    if (code === 200) {
      if (Array.isArray(data?.modelDetail?.fields)) {
        if (Array.isArray(tagItem?.tagDefineParams?.dependencies)) {
          const fieldList = data.modelDetail.fields.map((item: ISemantic.IFieldTypeParamsItem) => {
            const { fieldName } = item;
            if (tagItem?.tagDefineParams?.dependencies.includes(fieldName)) {
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
    }
  };

  useEffect(() => {
    const id = modelId || tagItem?.modelId;
    if (!id) {
      return;
    }
    queryModelDetail(id);
    queryDimensionList(id);
    queryMetricList(id);
  }, [modelId, tagItem]);

  const handleNext = async () => {
    const fieldsValue = await form.validateFields();
    const submitForm = {
      ...formValRef.current,
      ...fieldsValue,
      tagDefineType: defineType,
      tagDefineParams: exprTypeParamsState[defineType],
    };
    updateFormVal(submitForm);
    if (currentStep < 1) {
      forward();
    } else {
      await saveTag(submitForm);
    }
  };

  const initData = () => {
    if (!tagItem) {
      return;
    }
    const { id, name, bizName, description, sensitiveLevel, tagDefineType, tagDefineParams } =
      tagItem;

    const initValue = {
      id,
      name,
      bizName,
      sensitiveLevel,
      description,
      tagDefineType,
      tagDefineParams,
    };
    const editInitFormVal = {
      ...formValRef.current,
      ...initValue,
    };
    const { dependencies, expr } = tagDefineParams || {};
    setExprTypeParamsState({
      ...exprTypeParamsState,
      [tagDefineType]: {
        dependencies: dependencies || [],
        expr: expr || '',
      },
    });
    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
    if (tagDefineType) {
      setDefineType(tagDefineType);
    }
  };

  useEffect(() => {
    if (isEdit) {
      initData();
    }
  }, [tagItem]);

  const isEmptyConditions = (
    tagDefineType: TAG_DEFINE_TYPE,
    metricDefineParams: ISemantic.ITagDefineParams,
  ) => {
    const { dependencies, expr } = metricDefineParams || {};
    if (!expr) {
      message.error('请输入度量表达式');
      return true;
    }
    if (tagDefineType === TAG_DEFINE_TYPE.DIMENSION) {
      if (!(Array.isArray(dependencies) && dependencies.length > 0)) {
        message.error('请添加一个维度');
        return true;
      }
    }
    if (tagDefineType === TAG_DEFINE_TYPE.FIELD) {
      if (!(Array.isArray(dependencies) && dependencies.length > 0)) {
        message.error('请添加一个字段');
        return true;
      }
    }
    return false;
  };

  const saveTag = async (fieldsValue: any) => {
    const queryParams = {
      modelId: isEdit ? tagItem.modelId : modelId,
      ...fieldsValue,
    };
    if (isEmptyConditions(defineType, queryParams.tagDefineParams)) {
      return;
    }
    let saveTagQuery = createTag;
    if (queryParams.id) {
      saveTagQuery = updateTag;
    }
    const { code, msg } = await saveTagQuery(queryParams);
    if (code === 200) {
      message.success('编辑标签成功');
      onSubmit?.(queryParams);
      return;
    }
    message.error(msg);
  };

  const queryDimensionList = async (modelId: number) => {
    const { code, data, msg } = await getDimensionList({ modelId: modelId || tagItem?.modelId });
    if (code === 200 && Array.isArray(data?.list)) {
      const { list } = data;
      if (isArrayOfValues(tagItem?.tagDefineParams?.dependencies)) {
        const fieldList = list.map((item: ISemantic.IDimensionItem) => {
          const { id } = item;
          if (tagItem?.tagDefineParams.dependencies.includes(id)) {
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
            a: ISemantic.IDimensionItem & { orderNumber: number },
            b: ISemantic.IDimensionItem & { orderNumber: number },
          ) => b.orderNumber - a.orderNumber,
        );
        setDimensionList(sortList);
      } else {
        setDimensionList(list);
      }
    } else {
      message.error(msg);
    }
  };

  const queryMetricList = async (modelId: number) => {
    const { code, data, msg } = await queryMetric({
      modelId: modelId || tagItem?.modelId,
    });
    const { list } = data || {};
    if (code === 200) {
      if (isArrayOfValues(tagItem?.tagDefineParams?.dependencies)) {
        const fieldList = list.map((item: ISemantic.IMetricItem) => {
          const { id } = item;
          if (tagItem?.tagDefineParams.dependencies.includes(id)) {
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
            a: ISemantic.IMetricItem & { orderNumber: number },
            b: ISemantic.IMetricItem & { orderNumber: number },
          ) => b.orderNumber - a.orderNumber,
        );
        setMetricList(sortList);
      } else {
        setMetricList(list);
      }

      setMetricMap(
        list.reduce(
          (infoMap: Record<string, ISemantic.IMetricItem>, item: ISemantic.IMetricItem) => {
            infoMap[`${item.id}`] = item;
            return infoMap;
          },
          {},
        ),
      );
    } else {
      message.error(msg);
      setMetricList([]);
    }
  };

  const renderContent = () => {
    if (currentStep === 1) {
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
              <Radio.Button value={TAG_DEFINE_TYPE.DIMENSION}>按维度</Radio.Button>
              <Radio.Button value={TAG_DEFINE_TYPE.METRIC}>按指标</Radio.Button>
              <Radio.Button value={TAG_DEFINE_TYPE.FIELD}>按字段</Radio.Button>
            </Radio.Group>
          </div>

          {defineType === TAG_DEFINE_TYPE.DIMENSION && (
            <>
              <p className={styles.desc}>
                基于
                <Tag color="#2499ef14" className={styles.markerTag}>
                  维度
                </Tag>
                来生成新的标签
              </p>

              <TagDimensionFormTable
                typeParams={exprTypeParamsState[TAG_DEFINE_TYPE.DIMENSION]}
                dimensionList={dimensionList}
                onFieldChange={(dimension: ISemantic.IDimensionTypeParamsItem[]) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [TAG_DEFINE_TYPE.DIMENSION]: {
                        ...prevState[TAG_DEFINE_TYPE.DIMENSION],
                        dependencies: dimension.map((item) => item.id),
                      },
                    };
                  });
                }}
                onSqlChange={(expr: string) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [TAG_DEFINE_TYPE.DIMENSION]: {
                        ...prevState[TAG_DEFINE_TYPE.DIMENSION],
                        expr,
                      },
                    };
                  });
                }}
              />
            </>
          )}
          {defineType === TAG_DEFINE_TYPE.METRIC && (
            <>
              <p className={styles.desc}>
                基于
                <Tag color="#2499ef14" className={styles.markerTag}>
                  指标
                </Tag>
                来生成新的标签
              </p>

              <MetricMetricFormTable
                typeParams={{
                  ...exprTypeParamsState[TAG_DEFINE_TYPE.METRIC],
                  metrics: exprTypeParamsState[TAG_DEFINE_TYPE.METRIC].dependencies.map((id) => {
                    return { id: Number(id), bizName: metricMap[id]?.bizName || '' };
                  }),
                }}
                metricList={metricList}
                onFieldChange={(metrics: ISemantic.IMetricTypeParamsItem[]) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [TAG_DEFINE_TYPE.METRIC]: {
                        ...prevState[TAG_DEFINE_TYPE.METRIC],
                        dependencies: metrics.map((item) => item.id),
                      },
                    };
                  });
                }}
                onSqlChange={(expr: string) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [TAG_DEFINE_TYPE.METRIC]: {
                        ...prevState[TAG_DEFINE_TYPE.METRIC],
                        expr,
                      },
                    };
                  });
                }}
              />
            </>
          )}
          {defineType === TAG_DEFINE_TYPE.FIELD && (
            <>
              <MetricFieldFormTable
                typeParams={{
                  ...exprTypeParamsState[TAG_DEFINE_TYPE.FIELD],
                  fields: exprTypeParamsState[TAG_DEFINE_TYPE.FIELD].dependencies.map(
                    (fieldName) => {
                      return { fieldName: `${fieldName}` };
                    },
                  ),
                }}
                fieldList={fieldList}
                onFieldChange={(fields: ISemantic.IFieldTypeParamsItem[]) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [TAG_DEFINE_TYPE.FIELD]: {
                        ...prevState[TAG_DEFINE_TYPE.FIELD],
                        dependencies: fields.map((item) => item.fieldName),
                      },
                    };
                  });
                }}
                onSqlChange={(expr: string) => {
                  setExprTypeParamsState((prevState) => {
                    return {
                      ...prevState,
                      [TAG_DEFINE_TYPE.FIELD]: {
                        ...prevState[TAG_DEFINE_TYPE.FIELD],
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
    }

    return (
      <>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name="name"
          label="标签名称"
          rules={[{ required: true, message: '请输入标签名称' }]}
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
                    在录入标签时，请务必详细填写标签口径。口径描述对于理解标签的含义、计算方法和使用场景至关重要。一个清晰、准确的口径描述可以帮助其他用户更好地理解和使用该标签，避免因为误解而导致错误的数据分析和决策。在填写口径时，建议包括以下信息：
                  </p>
                  <p>1. 标签的计算方法：详细说明标签是如何计算的，包括涉及的公式、计算步骤等。</p>
                  <p>2. 数据来源：描述标签所依赖的数据来源，包括数据表、字段等信息。</p>
                  <p>3. 使用场景：说明该标签适用于哪些业务场景，以及如何在这些场景中使用该标签。</p>
                  <p>4. 任何其他相关信息：例如数据更新频率、数据质量要求等。</p>
                  <p>
                    请确保口径描述清晰、简洁且易于理解，以便其他用户能够快速掌握标签的核心要点。
                  </p>
                </>
              }
            />
          }
          rules={[{ required: true, message: '请输入业务口径' }]}
        >
          <TextArea placeholder="请输入业务口径" />
        </FormItem>
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
      width={800}
      style={{ top: 48 }}
      // styles={{ padding: '32px 40px 48px' }}
      destroyOnClose
      title={`${isEdit ? '编辑' : '新建'}标签`}
      maskClosable={false}
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
    >
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
          }}
          className={styles.form}
        >
          {renderContent()}
        </Form>
      </>
    </Modal>
  );
};

export default TagInfoCreateForm;
