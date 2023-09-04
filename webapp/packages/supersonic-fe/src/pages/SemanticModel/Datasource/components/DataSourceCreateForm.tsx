import React, { useEffect, useRef, useState } from 'react';
import { Form, Button, Modal, Steps, message } from 'antd';
import DataSourceBasicForm from './DataSourceBasicForm';
import FieldForm from './DataSourceFieldForm';
import { formLayout } from '@/components/FormHelper/utils';
import { EnumDataSourceType } from '../constants';
import type { DataInstanceItem } from '../data';
import styles from '../style.less';
import { createDatasource, updateDatasource, getColumns } from '../../service';
import type { Dispatch } from 'umi';
import type { StateType } from '../../model';
import { connect } from 'umi';

export type CreateFormProps = {
  domainManger: StateType;
  dispatch: Dispatch;
  createModalVisible: boolean;
  sql?: string;
  dataSourceItem: DataInstanceItem | any;
  onCancel?: () => void;
  onSubmit?: (dataSourceInfo: any) => void;
  scriptColumns?: any[] | undefined;
  basicInfoFormMode?: 'normal' | 'fast';
  onDataBaseTableChange?: (tableName: string) => void;
};
const { Step } = Steps;

const initFormVal = {
  name: '', // 数据源名称
  bizName: '', // 数据源英文名
  description: '', // 数据源描述
};

const DataSourceCreateForm: React.FC<CreateFormProps> = ({
  domainManger,
  onCancel,
  createModalVisible,
  scriptColumns,
  sql = '',
  onSubmit,
  dataSourceItem,
  basicInfoFormMode,
}) => {
  const isEdit = !!dataSourceItem?.id;
  const [fields, setFields] = useState<any[]>([]);
  const [currentStep, setCurrentStep] = useState(0);
  const [saveLoading, setSaveLoading] = useState(false);
  const [hasEmptyNameField, setHasEmptyNameField] = useState<boolean>(false);
  const [formDatabaseId, setFormDatabaseId] = useState<number>();
  const formValRef = useRef(initFormVal as any);
  const [form] = Form.useForm();
  const { databaseConfigList, selectModelId: modelId } = domainManger;
  const updateFormVal = (val: any) => {
    formValRef.current = val;
  };

  useEffect(() => {
    const hasEmpty = fields.some((item) => {
      const { name, isCreateDimension, isCreateMetric } = item;
      if ((isCreateMetric || isCreateDimension) && !name) {
        return true;
      }
      return false;
    });
    setHasEmptyNameField(hasEmpty);
  }, [fields]);

  const [fieldColumns, setFieldColumns] = useState(scriptColumns || []);
  useEffect(() => {
    if (scriptColumns) {
      setFieldColumns(scriptColumns);
    }
  }, [scriptColumns]);

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const getFieldsClassify = (fieldsList: any[]) => {
    const classify = fieldsList.reduce(
      (fieldsClassify, item: any) => {
        const {
          type,
          bizName,
          timeGranularity,
          agg,
          isCreateDimension: createDimension,
          name,
          isCreateMetric: createMetric,
          dateFormat,
        } = item;
        const isCreateDimension = createDimension ? 1 : 0;
        const isCreateMetric = createMetric ? 1 : 0;
        switch (type) {
          case EnumDataSourceType.CATEGORICAL:
            fieldsClassify.dimensions.push({
              bizName,
              type,
              isCreateDimension,
              name,
            });
            break;
          case EnumDataSourceType.TIME:
            fieldsClassify.dimensions.push({
              bizName,
              type,
              isCreateDimension,
              name,
              dateFormat,
              typeParams: {
                isPrimary: true,
                timeGranularity,
              },
            });
            break;
          case EnumDataSourceType.FOREIGN:
          case EnumDataSourceType.PRIMARY:
            fieldsClassify.identifiers.push({
              bizName,
              name,
              type,
            });
            break;
          case EnumDataSourceType.MEASURES:
            fieldsClassify.measures.push({
              bizName,
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
  const handleNext = async () => {
    const fieldsValue = await form.validateFields();

    const fieldsClassify = getFieldsClassify(fields);
    const submitForm = {
      ...formValRef.current,
      ...fieldsValue,
      ...fieldsClassify,
    };
    updateFormVal(submitForm);
    if (currentStep < 1) {
      forward();
    } else {
      setSaveLoading(true);
      const { dbName, tableName } = submitForm;
      const queryParams = {
        ...submitForm,
        sqlQuery: sql,
        databaseId: dataSourceItem?.databaseId || formDatabaseId,
        queryType: basicInfoFormMode === 'fast' ? 'table_query' : 'sql_query',
        tableQuery: dbName && tableName ? `${dbName}.${tableName}` : '',
        modelId,
      };
      const queryDatasource = isEdit ? updateDatasource : createDatasource;
      const { code, msg, data } = await queryDatasource(queryParams);
      setSaveLoading(false);
      if (code === 200) {
        message.success('保存数据源成功！');
        onSubmit?.({
          ...queryParams,
          ...data,
          resData: data,
        });
        return;
      }
      message.error(msg);
    }
  };

  const initFields = (fieldsClassifyList: any[], columns: any[]) => {
    const columnFields: any[] = columns.map((item: any) => {
      const { type, nameEn } = item;
      const oldItem = fieldsClassifyList.find((oItem) => oItem.bizName === item.nameEn) || {};
      return {
        ...oldItem,
        bizName: nameEn,
        // name,
        sqlType: type,
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
    const { queryType, tableQuery } = dataSourceItem.datasourceDetail;
    let tableQueryInitValue = {};
    let columns = fieldColumns;
    if (queryType === 'table_query') {
      const tableQueryString = tableQuery || '';
      const [dbName, tableName] = tableQueryString.split('.');
      columns = await queryTableColumnList(dataSourceItem.databaseId, dbName, tableName);
      tableQueryInitValue = {
        dbName,
        tableName,
      };
    }
    formatterInitData(columns, tableQueryInitValue);
  };

  const formatterInitData = (columns: any[], extendParams: Record<string, any> = {}) => {
    const { id, name, bizName, description, datasourceDetail, databaseId } = dataSourceItem as any;
    const { dimensions, identifiers, measures } = datasourceDetail;
    const initValue = {
      id,
      name,
      bizName,
      description,
      databaseId,
      ...extendParams,
      // ...tableQueryInitValue,
    };
    const editInitFormVal = {
      ...formValRef.current,
      ...initValue,
    };
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
    if (isEdit) {
      initData();
    } else {
      initFields([], fieldColumns);
    }
  }, [dataSourceItem]);

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
      // setTableNameList(list);
      const columns = list.map((item: any) => {
        const { dataType, name } = item;
        return {
          nameEn: name,
          type: dataType,
        };
      });
      // setFields(columns);
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
          <FieldForm fields={fields} onFieldChange={handleFieldChange} />;
        </div>
        <div style={{ display: currentStep !== 1 ? 'block' : 'none' }}>
          <DataSourceBasicForm
            form={form}
            isEdit={isEdit}
            mode={basicInfoFormMode}
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
          <Button onClick={onCancel}>取消</Button>
          <Button
            type="primary"
            loading={saveLoading}
            onClick={handleNext}
            disabled={hasEmptyNameField}
          >
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
      bodyStyle={{ padding: '32px 40px 48px' }}
      destroyOnClose
      title={`${isEdit ? '编辑' : '新建'}数据源`}
      maskClosable={false}
      open={createModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
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
    </Modal>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DataSourceCreateForm);
