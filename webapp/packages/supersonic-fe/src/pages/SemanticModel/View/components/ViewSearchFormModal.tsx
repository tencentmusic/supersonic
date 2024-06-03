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
import { ChatConfigType } from '../../enum';

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

  useEffect(() => {
    const dataSetModelConfigs = viewItem?.dataSetDetail?.dataSetModelConfigs;
    if (Array.isArray(dataSetModelConfigs)) {
      const allMetrics: number[] = [];
      const allDimensions: number[] = [];
      dataSetModelConfigs.forEach((item: ISemantic.IViewModelConfigItem) => {
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
        <ProCard title="指标模式" style={{ marginBottom: 10, borderBottom: '1px solid #eee' }}>
          <DefaultSettingForm
            form={form}
            dimensionList={dimensionList}
            metricList={metricList}
            chatConfigType={ChatConfigType.METRIC}
          />
        </ProCard>

        <ProCard title="明细模式">
          <DefaultSettingForm
            form={form}
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
        onValuesChange={(value, values) => {}}
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
