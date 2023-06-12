import React, { useEffect, useRef, useState } from 'react';
import { Form, Button, Modal, Steps, message } from 'antd';
import BasicInfoForm from './DataSourceBasicForm';
import FieldForm from './DataSourceFieldForm';
import { formLayout } from '@/components/FormHelper/utils';
import { EnumDataSourceType } from '../constants';
import type { DataInstanceItem, FieldItem, SaveDataSetForm } from '../data';
import styles from '../style.less';
import { createDatasource, updateDatasource } from '../../service';

export type CreateFormProps = {
  createModalVisible: boolean;
  sql: string;
  domainId: number;
  dataSourceItem: DataInstanceItem | any;
  onCancel?: () => void;
  onSubmit?: (dataSourceInfo: any) => void;
  scriptColumns: any[];
};
const { Step } = Steps;

const initFormVal = {
  name: '', // 数据源名称
  bizName: '', // 数据源英文名
  description: '', // 数据源描述
};

const DataSourceCreateForm: React.FC<CreateFormProps> = ({
  onCancel,
  createModalVisible,
  domainId,
  scriptColumns,
  sql,
  onSubmit,
  dataSourceItem,
}) => {
  const isEdit = !!dataSourceItem?.id;
  const [fields, setFields] = useState<FieldItem[]>([]);
  const [currentStep, setCurrentStep] = useState(0);
  const [saveLoading, setSaveLoading] = useState(false);
  const formValRef = useRef(initFormVal as any);
  const [form] = Form.useForm();
  const updateFormVal = (val: SaveDataSetForm) => {
    formValRef.current = val;
  };

  const forward = () => setCurrentStep(currentStep + 1);
  const backward = () => setCurrentStep(currentStep - 1);

  const getFieldsClassify = (fieldsList: FieldItem[]) => {
    const classify = fieldsList.reduce(
      (fieldsClassify, item: FieldItem) => {
        const {
          type,
          bizName,
          timeGranularity,
          agg,
          isCreateDimension,
          name,
          isCreateMetric,
          dateFormat,
        } = item;
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
      const queryParams = {
        ...submitForm,
        sqlQuery: sql,
        databaseId: dataSourceItem.databaseId,
        queryType: 'sql_query',
        domainId,
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

  const initFields = (fieldsClassifyList: any[]) => {
    const columnFields: any[] = scriptColumns.map((item: any) => {
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

  const initData = () => {
    const { id, name, bizName, description, datasourceDetail } = dataSourceItem as any;
    const initValue = {
      id,
      name,
      bizName,
      description,
    };
    const editInitFormVal = {
      ...formValRef.current,
      ...initValue,
    };
    updateFormVal(editInitFormVal);
    form.setFieldsValue(initValue);
    const { dimensions, identifiers, measures } = datasourceDetail;
    const formatFields = [
      ...formatterDimensions(dimensions || []),
      ...(identifiers || []),
      ...formatterMeasures(measures || []),
    ];
    initFields(formatFields);
  };

  useEffect(() => {
    if (isEdit) {
      initData();
    } else {
      initFields([]);
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

  const renderContent = () => {
    if (currentStep === 1) {
      return <FieldForm fields={fields} onFieldChange={handleFieldChange} />;
    }
    return <BasicInfoForm form={form} isEdit={isEdit} />;
  };

  const renderFooter = () => {
    if (currentStep === 1) {
      return (
        <>
          <Button style={{ float: 'left' }} onClick={backward}>
            上一步
          </Button>
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" loading={saveLoading} onClick={handleNext}>
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
        className={styles.form}
      >
        {renderContent()}
      </Form>
    </Modal>
  );
};

export default DataSourceCreateForm;
