import React, { useEffect, useRef, useState } from 'react';
import { Form, Button, Modal, Steps, message } from 'antd';
import ModelBasicForm from './ModelBasicForm';
import ModelFieldForm from './ModelFieldForm';
import { formLayout } from '@/components/FormHelper/utils';
import { EnumDataSourceType } from '../constants';
import styles from '../style.less';
import {
  updateModel,
  createModel,
  getColumns,
  getUnAvailableItem,
  getTagObjectList,
} from '../../service';
import { useModel } from '@umijs/max';
import { ISemantic, IDataSource } from '../../data';
import { isArrayOfValues } from '@/utils/utils';
import EffectDimensionAndMetricTipsModal from './EffectDimensionAndMetricTipsModal';

export type CreateFormProps = {
  createModalVisible: boolean;
  sql?: string;
  sqlParams?: IDataSource.ISqlParamsItem[];
  databaseId?: number;
  modelItem: ISemantic.IModelItem;
  onCancel?: () => void;
  onSubmit?: (modelItem: ISemantic.IModelItem) => void;
  scriptColumns?: any[] | undefined;
  basicInfoFormMode?: 'normal' | 'fast';
  onDataBaseTableChange?: (tableName: string) => void;
  onDataSourceBtnClick?: () => void;
  onOpenDataSourceEdit?: () => void;
};
const { Step } = Steps;

const initFormVal = {
  name: '', // 模型名称
  bizName: '', // 模型英文名
  description: '', // 模型描述
};

const ModelCreateForm: React.FC<CreateFormProps> = ({
  onCancel,
  createModalVisible,
  scriptColumns,
  sql = '',
  sqlParams,
  onSubmit,
  modelItem,
  databaseId,
  basicInfoFormMode,
  onDataSourceBtnClick,
  onOpenDataSourceEdit,
  children,
}) => {
  const isEdit = !!modelItem?.id;

  const [fields, setFields] = useState<any[]>([]);
  const [currentStep, setCurrentStep] = useState(0);
  const [saveLoading, setSaveLoading] = useState(false);

  const [formDatabaseId, setFormDatabaseId] = useState<number>();
  const [queryParamsState, setQueryParamsState] = useState({});
  const [effectTipsModalOpenState, setEffectTipsModalOpenState] = useState<boolean>(false);
  const [effectTipsData, setEffectTipsData] = useState<
    (ISemantic.IDimensionItem | ISemantic.IMetricItem)[]
  >([]);
  const [tagObjectList, setTagObjectList] = useState<ISemantic.ITagObjectItem[]>([]);
  const [tagObjectIdState, setTagObjectIdState] = useState(modelItem?.tagObjectId);
  const formValRef = useRef(initFormVal as any);
  const [form] = Form.useForm();
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const { selectDomainId, selectDomain: domainData } = domainModel;
  const { selectModelId: modelId } = modelModel;

  const databaseModel = useModel('SemanticModel.databaseData');
  const { databaseConfigList } = databaseModel;

  const updateFormVal = (val: any) => {
    formValRef.current = val;
  };
  const [sqlFilter, setSqlFilter] = useState<string>('');

  const [fieldColumns, setFieldColumns] = useState<IDataSource.IExecuteSqlColumn[]>(
    scriptColumns || [],
  );
  useEffect(() => {
    if (scriptColumns) {
      setFieldColumns(scriptColumns);
    }
  }, [scriptColumns]);

  useEffect(() => {
    queryTagObjectList();
  }, []);

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const queryTagObjectList = async () => {
    const { code, msg, data } = await getTagObjectList({
      domainId: domainData?.parentId || domainData?.id,
    });
    if (code === 200) {
      setTagObjectList(data);

      return;
    }
    message.error(msg);
  };

  const checkAvailableItem = async (fields: string[] = []) => {
    if (!modelItem) {
      return false;
    }
    const originalFields = modelItem.modelDetail?.fields || [];
    const hasRemoveFields = originalFields.reduce(
      (fieldList: string[], item: IDataSource.IDataSourceDetailFieldsItem) => {
        const { fieldName } = item;
        if (!fields.includes(fieldName)) {
          fieldList.push(fieldName);
        }
        return fieldList;
      },
      [],
    );
    if (!isArrayOfValues(hasRemoveFields)) {
      return false;
    }
    const { code, data, msg } = await getUnAvailableItem({
      modelId: modelItem?.id,
      fields: hasRemoveFields,
    });
    if (code === 200 && data) {
      const { metricResps = [], dimensionResps = [] } = data;
      if (!isArrayOfValues(metricResps) && !isArrayOfValues(dimensionResps)) {
        return false;
      }
      setEffectTipsData([...metricResps, ...dimensionResps]);
      setEffectTipsModalOpenState(true);
      return true;
    }
    message.error(msg);
    return false;
  };

  const saveModel = async (queryParams: any) => {
    setSaveLoading(true);
    const querySaveModel = isEdit ? updateModel : createModel;
    const { code, msg, data } = await querySaveModel(queryParams);
    setSaveLoading(false);
    if (code === 200) {
      message.success('保存模型成功！');
      onSubmit?.({
        ...queryParams,
        ...data,
        resData: data,
      });
      return;
    }
    message.error(msg);
  };

  const getFieldsClassify = (fieldsList: any[]) => {
    const classify = fieldsList.reduce(
      (fieldsClassify, item: any) => {
        const {
          type,
          // bizName,
          fieldName,
          timeGranularity,
          agg,
          isCreateDimension: createDimension,
          name,
          isCreateMetric: createMetric,
          dateFormat,
          // entityNames,
          isTag,
        } = item;
        const isCreateDimension = createDimension ? 1 : 0;
        const isCreateMetric = createMetric ? 1 : 0;
        switch (type) {
          case EnumDataSourceType.CATEGORICAL:
            fieldsClassify.dimensions.push({
              bizName: fieldName,
              type,
              isCreateDimension,
              name,
              isTag: isTag ? 1 : 0,
            });
            break;
          case EnumDataSourceType.TIME:
            fieldsClassify.dimensions.push({
              bizName: fieldName,
              type,
              isCreateDimension,
              name,
              dateFormat,
              typeParams: {
                isPrimary: true,
                timeGranularity: timeGranularity || '',
              },
            });
            break;
          case EnumDataSourceType.FOREIGN:
          case EnumDataSourceType.PRIMARY:
            fieldsClassify.identifiers.push({
              bizName: fieldName,
              isCreateDimension,
              name,
              type,
              tagObjectId: modelItem?.tagObjectId,
            });
            break;
          case EnumDataSourceType.MEASURES:
            fieldsClassify.measures.push({
              bizName: fieldName,
              type,
              agg,
              name,
              isCreateMetric,
            });
            break;
          default:
            break;
        }
        return fieldsClassify;
      },
      {
        identifiers: [],
        dimensions: [],
        measures: [],
      } as any,
    );
    return classify;
  };

  const handleNext = async (saveState: boolean = false) => {
    const fieldsValue = await form.validateFields();

    const fieldsClassify = getFieldsClassify(fields);
    const submitForm = {
      ...formValRef.current,
      ...fieldsValue,
      ...fieldsClassify,
      fields: fieldColumns.map((item) => {
        return {
          fieldName: item.nameEn,
          dataType: item.type,
        };
      }),
    };
    updateFormVal(submitForm);
    if (!saveState && currentStep < 1) {
      forward();
    } else {
      const { dbName, tableName } = submitForm;
      const queryParams = {
        ...submitForm,
        databaseId: databaseId || modelItem?.databaseId || formDatabaseId,
        modelId: isEdit ? modelItem.id : modelId,
        filterSql: sqlFilter,
        domainId: isEdit ? modelItem.domainId : selectDomainId,
        modelDetail: {
          ...submitForm,
          queryType: basicInfoFormMode === 'fast' ? 'table_query' : 'sql_query',
          tableQuery: dbName && tableName ? `${dbName}.${tableName}` : '',
          sqlQuery: sql,
          sqlVariables: sqlParams,
        },
        tagObjectId: tagObjectIdState,
      };
      setQueryParamsState(queryParams);
      const checkState = await checkAvailableItem(fieldColumns.map((item) => item.nameEn));
      if (checkState) {
        return;
      }
      saveModel(queryParams);
    }
  };

  const initFields = (fieldsClassifyList: any[], columns: IDataSource.IExecuteSqlColumn[]) => {
    if (Array.isArray(columns) && columns.length === 0) {
      setFields(fieldsClassifyList || []);
      return;
    }
    const columnFields: any[] = columns.map((item: IDataSource.IExecuteSqlColumn) => {
      const { type, nameEn, comment } = item;
      const oldItem =
        fieldsClassifyList.find((oItem) => {
          return oItem.fieldName === item.nameEn;
        }) || {};
      return {
        ...oldItem,
        bizName: nameEn,
        fieldName: nameEn,
        dataType: type,
        comment,
      };
    });
    setFields(columnFields || []);
  };

  const formatterMeasures = (measuresList: any[] = []) => {
    return measuresList.map((measures: any) => {
      return {
        ...measures,
        type: EnumDataSourceType.MEASURES,
      };
    });
  };
  const formatterDimensions = (dimensionsList: any[] = []) => {
    return dimensionsList.map((dimension: any) => {
      const { typeParams } = dimension;
      return {
        ...dimension,
        timeGranularity: typeParams?.timeGranularity || '',
      };
    });
  };

  const initData = async () => {
    const { queryType, tableQuery } = modelItem?.modelDetail || {};
    let tableQueryInitValue = {};
    let columns = fieldColumns || [];
    if (queryType === 'table_query') {
      const tableQueryString = tableQuery || '';
      const [dbName, tableName] = tableQueryString.split('.');
      columns = await queryTableColumnList(modelItem.databaseId, dbName, tableName);
      tableQueryInitValue = {
        dbName,
        tableName,
      };
    }
    formatterInitData(columns, tableQueryInitValue);
  };

  const formatterInitData = (columns: any[], extendParams: Record<string, any> = {}) => {
    const { modelDetail, filterSql, drillDownDimensions = [] } = modelItem;
    const { dimensions, identifiers, measures } = modelDetail || {};
    const initValue = {
      ...modelItem,
      drillDownDimensions: Array.isArray(drillDownDimensions)
        ? drillDownDimensions.map((item) => {
            return item.dimensionId;
          })
        : [],
      ...extendParams,
    };
    const editInitFormVal = {
      ...formValRef.current,
      ...initValue,
    };
    setSqlFilter(filterSql);
    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
    const formatFields = [
      ...formatterDimensions(dimensions || []),
      ...(identifiers || []),
      ...formatterMeasures(measures || []),
    ];
    initFields(formatFields, columns);
  };

  useEffect(() => {
    const { queryType } = modelItem?.modelDetail || {};
    if (queryType === 'table_query') {
      if (isEdit) {
        initData();
      } else {
        initFields([], fieldColumns);
      }
    }
    setTagObjectIdState(modelItem?.tagObjectId);
  }, [modelItem]);

  useEffect(() => {
    const { queryType } = modelItem?.modelDetail || {};
    if (queryType !== 'table_query') {
      if (isEdit) {
        initData();
      } else {
        initFields([], fieldColumns);
      }
    }
  }, [modelItem, fieldColumns]);

  const handleFieldChange = (fieldName: string, data: any) => {
    const result = fields.map((field) => {
      if (field.bizName === fieldName) {
        return {
          ...field,
          ...data,
        };
      }
      return {
        ...field,
      };
    });
    setFields(result);
  };

  const queryTableColumnList = async (databaseId: number, dbName: string, tableName: string) => {
    const { code, data, msg } = await getColumns(databaseId, dbName, tableName);
    if (code === 200) {
      const list = data?.resultList || [];
      const columns = list.map((item: any, index: number) => {
        const { dataType, name, comment } = item;
        return {
          comment,
          nameEn: name,
          type: dataType,
        };
      });
      initFields([], columns);
      setFieldColumns(columns);
      return columns;
    } else {
      message.error(msg);
    }
  };

  const renderContent = () => {
    return (
      <>
        <div style={{ display: currentStep === 1 ? 'block' : 'none' }}>
          <ModelFieldForm
            fields={fields}
            tagObjectList={tagObjectList}
            tagObjectId={tagObjectIdState}
            onTagObjectChange={(tagObjectId) => {
              setTagObjectIdState(tagObjectId);
            }}
            onFieldChange={handleFieldChange}
            onSqlChange={(sql) => {
              setSqlFilter(sql);
            }}
            sql={sqlFilter}
          />
        </div>
        <div style={{ display: currentStep !== 1 ? 'block' : 'none' }}>
          <ModelBasicForm
            form={form}
            isEdit={isEdit}
            mode={basicInfoFormMode}
            modelItem={modelItem}
            databaseConfigList={databaseConfigList}
          />
        </div>
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
          <Button onClick={onCancel}>取 消</Button>
          {(modelItem?.modelDetail?.queryType === 'sql_query' || basicInfoFormMode !== 'fast') && (
            <Button
              type="primary"
              onClick={() => {
                onDataSourceBtnClick?.();
              }}
            >
              SQL编辑
            </Button>
          )}

          <Button
            type="primary"
            loading={saveLoading}
            onClick={() => {
              handleNext(true);
            }}
          >
            保 存
          </Button>
        </>
      );
    }
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          onClick={async () => {
            if (!isEdit && Array.isArray(fields) && fields.length === 0) {
              await form.validateFields();
              onOpenDataSourceEdit?.();
            }
            handleNext();
          }}
        >
          下一步
        </Button>
        {isEdit && (
          <Button
            type="primary"
            loading={saveLoading}
            onClick={() => {
              handleNext(true);
            }}
          >
            保 存
          </Button>
        )}
      </>
    );
  };

  return (
    <Modal
      forceRender
      width={currentStep ? 1300 : 800}
      destroyOnClose
      title={`${isEdit ? '编辑' : '新建'}模型`}
      maskClosable={false}
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={() => {
        onCancel?.();
      }}
    >
      <Steps style={{ marginBottom: 28 }} size="small" current={currentStep}>
        <Step title="基本信息" />
        <Step title="字段信息" />
      </Steps>
      <Form
        {...formLayout}
        form={form}
        initialValues={{
          ...formValRef.current,
        }}
        onValuesChange={(value, values) => {
          const { tableName } = value;
          const { dbName, databaseId } = values;
          setFormDatabaseId(databaseId);
          if (tableName) {
            queryTableColumnList(databaseId, dbName, tableName);
          }
        }}
        className={styles.form}
      >
        {renderContent()}
      </Form>
      {children}

      <EffectDimensionAndMetricTipsModal
        open={effectTipsModalOpenState}
        tableDataSource={effectTipsData}
        onCancel={() => {
          setEffectTipsModalOpenState(false);
        }}
        onOk={() => {
          saveModel(queryParamsState);
        }}
      />
    </Modal>
  );
};

export default ModelCreateForm;
