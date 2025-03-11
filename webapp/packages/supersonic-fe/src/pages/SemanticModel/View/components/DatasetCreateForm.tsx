import React, { useState, useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import { Form, Input, Select, Spin, Space } from 'antd';
import type { Ref } from 'react';
import styles from '../../components/style.less';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { createView, updateView, getDimensionList, queryMetric } from '../../service';
import { ISemantic } from '../../data';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import ViewModelConfigTransfer from './ViewModelConfigTransfer';

const FormItem = Form.Item;

export type ModelCreateFormModalProps = {
  activeKey: string;
  domainId: number;
  datasetItem: any;
  modelList: ISemantic.IModelItem[];
  onCancel: () => void;
  onSubmit: (values: any) => void;
};

const DatasetCreateForm: React.FC<ModelCreateFormModalProps> = forwardRef(
  (
    { activeKey, datasetItem, domainId, onCancel, onSubmit, modelList }: ModelCreateFormModalProps,
    ref: Ref<any>,
  ) => {
    const [saveLoading, setSaveLoading] = useState<boolean>(false);
    const [dimensionLoading, setDimensionLoading] = useState<boolean>(false);
    const [selectedModelItem, setSelectedModelItem] = useState<ISemantic.IModelItem | undefined>(
      modelList[0],
    );
    const [form] = Form.useForm();
    const configTableRef = useRef<any>();

    useImperativeHandle(ref, () => ({
      onSave: () => {
        return handleConfirm();
      },
    }));

    useEffect(() => {
      if (Array.isArray(modelList) || !selectedModelItem) {
        setSelectedModelItem(modelList[0]);
      }
    }, [modelList]);

    useEffect(() => {
      form.setFieldsValue({
        ...datasetItem,
      });
    }, [datasetItem]);

    const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>();
    const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>();

    useEffect(() => {
      if (selectedModelItem?.id) {
        queryDimensionList(selectedModelItem.id);
        queryMetricList(selectedModelItem.id);
      }
    }, [selectedModelItem]);

    const queryDimensionList = async (modelId: number) => {
      setDimensionLoading(true);
      const { code, data, msg } = await getDimensionList({ modelId });
      setDimensionLoading(false);
      if (code === 200 && Array.isArray(data?.list)) {
        setDimensionList(data.list);
      } else {
        message.error(msg);
      }
    };

    const queryMetricList = async (modelId: number) => {
      const { code, data, msg } = await queryMetric({ modelId });
      if (code === 200 && Array.isArray(data?.list)) {
        setMetricList(data.list);
      } else {
        message.error(msg);
      }
    };

    const handleConfirm = async () => {
      const fieldsValue = await form.validateFields();
      const viewModelConfigsMap = configTableRef?.current.getViewModelConfigs() || {};

      const queryData: ISemantic.IModelItem = {
        ...datasetItem,
        ...fieldsValue,
        dataSetDetail: {
          dataSetModelConfigs: Object.values(viewModelConfigsMap),
        },
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

    const renderContent = () => {
      return (
        <>
          <div style={{ display: activeKey === 'relation' ? 'block' : 'none' }}>
            <Spin spinning={dimensionLoading}>
              <ViewModelConfigTransfer
                toolbarSolt={
                  <Space>
                    <span>切换模型: </span>
                    <Select
                      style={{
                        minWidth: 150,
                        textAlign: 'left',
                      }}
                      value={selectedModelItem?.id}
                      placeholder="请选择模型，获取当前模型下指标维度信息"
                      onChange={(val) => {
                        setDimensionList(undefined);
                        setMetricList(undefined);
                        const modelItem = modelList.find((item) => item.id === val);
                        setSelectedModelItem(modelItem);
                      }}
                      options={modelList.map((item) => {
                        return { label: item.name, value: item.id };
                      })}
                    />
                  </Space>
                }
                dimensionList={dimensionList}
                metricList={metricList}
                modelItem={selectedModelItem}
                viewItem={datasetItem}
                ref={configTableRef}
              />
            </Spin>
          </div>
          <div style={{ display: activeKey === 'basic' ? 'block' : 'none' }}>
            <FormItem
              name="name"
              label="数据集名称"
              rules={[{ required: true, message: '请输入数据集名称！' }]}
            >
              <Input placeholder="数据集名称不可重复" />
            </FormItem>
            <FormItem
              name="bizName"
              label="数据集英文名称"
              rules={[{ required: true, message: '请输入数据集英文名称！' }]}
            >
              <Input placeholder="请输入数据集英文名称" />
            </FormItem>
            {/* <FormItem
            name="alias"
            label="别名"
            getValueFromEvent={(value) => {
              return Array.isArray(value) ? value.join(',') : '';
            }}
            getValueProps={(value) => {
              return {
                value: isString(value) ? value.split(',') : [],
              };
            }}
          >
            <Select
              mode="tags"
              placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
              tokenSeparators={[',']}
              maxTagCount={9}
            />
          </FormItem> */}
            <FormItem name="admins" label={<FormItemTitle title={'责任人'} />}>
              <SelectTMEPerson placeholder="请邀请团队成员" />
            </FormItem>
            <FormItem name="description" label="数据集描述">
              <Input.TextArea placeholder="数据集描述" />
            </FormItem>
          </div>
        </>
      );
    };

    return (
      <>
        <Form
          {...formLayout}
          form={form}
          onValuesChange={(value, values) => {}}
          className={styles.form}
        >
          {renderContent()}
        </Form>
      </>
    );
  },
);
export default DatasetCreateForm;
