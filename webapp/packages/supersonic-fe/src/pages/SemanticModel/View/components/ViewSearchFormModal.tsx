import React, { useState, useEffect, useRef } from 'react';
import { Form, Button, Modal, Input } from 'antd';
import styles from '../style.less';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { createView, updateView, getDimensionList, queryMetric } from '../../service';
import { ISemantic } from '../../data';
import DefaultSettingForm from './DefaultSettingForm';
import { isArrayOfValues } from '@/utils/utils';
import { ProCard } from '@ant-design/pro-components';
import { ChatConfigType, DetailTypeDefaultConfig, TimeModeEnum, DatePeriod } from '../../enum';

export type ModelCreateFormModalProps = {
  domainId: number;
  viewItem: any;
  onCancel: () => void;
  onSubmit: (values: any) => void;
};

const ViewSearchFormModal: React.FC<ModelCreateFormModalProps> = ({
  viewItem,
  domainId,
  onCancel,
  onSubmit,
}) => {
  const FormItem = Form.Item;
  const [saveLoading, setSaveLoading] = useState<boolean>(false);

  const [form] = Form.useForm();

  const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>();
  const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>();

  const [formData, setFormData] = useState(viewItem);

  useEffect(() => {
    const dataSetModelConfigs = viewItem?.dataSetDetail?.dataSetModelConfigs;
    if (Array.isArray(dataSetModelConfigs)) {
      const allMetrics: number[] = [];
      const allDimensions: number[] = [];
      dataSetModelConfigs.forEach((item: ISemantic.IDatasetModelConfigItem) => {
        const { metrics, dimensions } = item;
        allMetrics.push(...metrics);
        allDimensions.push(...dimensions);
      });
      queryDimensionListByIds(allDimensions);
      queryMetricListByIds(allMetrics);
    }
  }, [viewItem]);

  const queryDimensionListByIds = async (ids: number[]) => {
    if (!isArrayOfValues(ids)) {
      setDimensionList([]);
      return;
    }
    const { code, data, msg } = await getDimensionList({ ids });
    if (code === 200 && Array.isArray(data?.list)) {
      setDimensionList(data.list);
    } else {
      message.error(msg);
    }
  };

  const queryMetricListByIds = async (ids: number[]) => {
    if (!isArrayOfValues(ids)) {
      setMetricList([]);
      return;
    }
    const { code, data, msg } = await queryMetric({ ids });
    if (code === 200 && Array.isArray(data?.list)) {
      setMetricList(data.list);
    } else {
      message.error(msg);
    }
  };

  const handleConfirm = async () => {
    const fieldsValue = await form.validateFields();

    const queryData: ISemantic.IModelItem = {
      ...viewItem,
      ...fieldsValue,
      domainId,
    };
    setSaveLoading(true);
    const { code, msg } = await (!queryData.id ? createView : updateView)(queryData);
    setSaveLoading(false);
    if (code === 200) {
      onSubmit?.(queryData);
      message.success('保存成功');
    } else {
      message.error(msg);
    }
  };

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          loading={saveLoading}
          onClick={() => {
            handleConfirm();
          }}
        >
          保 存
        </Button>
      </>
    );
  };

  const renderContent = () => {
    return (
      <div className={styles.viewSearchFormContainer}>
        <ProCard title="聚合模式" style={{ marginBottom: 10, borderBottom: '1px solid #eee' }}>
          <DefaultSettingForm
            form={form}
            formData={formData}
            dimensionList={dimensionList}
            metricList={metricList}
            chatConfigType={ChatConfigType.METRIC}
          />
        </ProCard>

        <ProCard title="明细模式">
          <DefaultSettingForm
            form={form}
            formData={formData}
            dimensionList={dimensionList}
            metricList={metricList}
            chatConfigType={ChatConfigType.TAG}
          />
        </ProCard>
      </div>
    );
  };

  return (
    <Modal
      width={800}
      destroyOnClose
      title={'查询设置'}
      open={true}
      maskClosable={false}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Form
        {...formLayout}
        form={form}
        initialValues={{
          ...viewItem,
        }}
        onValuesChange={(value, values) => {
          const target =
            values?.queryConfig?.[DetailTypeDefaultConfig[ChatConfigType.METRIC]]
              ?.timeDefaultConfig;
          if (target?.timeMode) {
            if (target?.timeMode === TimeModeEnum.CURRENT) {
              if (![DatePeriod.MONTH, DatePeriod.YEAR].includes(target.period)) {
                values.queryConfig[
                  DetailTypeDefaultConfig[ChatConfigType.METRIC]
                ].timeDefaultConfig.period = DatePeriod.MONTH;
                form.setFieldsValue({ ...values });
              }
            } else {
              values.queryConfig[
                DetailTypeDefaultConfig[ChatConfigType.METRIC]
              ].timeDefaultConfig.period = DatePeriod.DAY;
              form.setFieldsValue({ ...values });
            }
          }

          setFormData({ ...values });
        }}
      >
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        {renderContent()}
      </Form>
    </Modal>
  );
};

export default ViewSearchFormModal;
