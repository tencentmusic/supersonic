import React, { useState, useEffect, useRef } from 'react';
import { Form, Button, Modal, Input, Select, Steps, Radio, Space } from 'antd';
import styles from '../../components/style.less';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { createView, updateView, getDimensionList, queryMetric, getTagList } from '../../service';
import { ISemantic } from '../../data';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import ViewModelConfigTransfer from './ViewModelConfigTransfer';

const FormItem = Form.Item;

export type ModelCreateFormModalProps = {
  domainId: number;
  viewItem: any;
  modelList: ISemantic.IModelItem[];
  onCancel: () => void;
  onSubmit: (values: any) => void;
};
const { Step } = Steps;
const ViewCreateFormModal: React.FC<ModelCreateFormModalProps> = ({
  viewItem,
  domainId,
  onCancel,
  onSubmit,
  modelList,
}) => {
  const [currentStep, setCurrentStep] = useState(0);

  const [formVals, setFormVals] = useState<ISemantic.IModelItem>({
    ...viewItem,
    currentModel: modelList[0]?.id,
  });

  const [queryType, setQueryType] = useState<string>('METRIC');

  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [modalWidth, setModalWidth] = useState<number>(800);
  const [selectedModelItem, setSelectedModelItem] = useState<ISemantic.IModelItem | undefined>(
    modelList[0],
  );
  const [form] = Form.useForm();
  const configTableRef = useRef<any>();

  useEffect(() => {
    form.setFieldsValue({
      ...viewItem,
    });
    // setQueryType(viewItem?.queryType);
  }, [viewItem]);

  const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>();
  const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>();
  const [tagList, setTagList] = useState<ISemantic.ITagItem[]>();

  useEffect(() => {
    if (selectedModelItem?.id) {
      queryDimensionList(selectedModelItem.id);
      queryMetricList(selectedModelItem.id);
      // queryTagList(selectedModelItem.id);
    }
  }, [selectedModelItem]);

  const queryDimensionList = async (modelId: number) => {
    const { code, data, msg } = await getDimensionList({ modelId });
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
      ...formVals,
      ...fieldsValue,
      queryType,
      dataSetDetail: {
        dataSetModelConfigs: Object.values(viewModelConfigsMap),
      },
      domainId,
    };
    setFormVals(queryData);
    setSaveLoading(true);
    const { code, msg } = await (!queryData.id ? createView : updateView)(queryData);
    setSaveLoading(false);
    if (code === 200) {
      onSubmit?.(queryData);
    } else {
      message.error(msg);
    }
  };

  const stepWidth: any = {
    '0': 800,
    '1': 1200,
    '2': 800,
  };

  const forward = () => {
    setModalWidth(stepWidth[`${currentStep + 1}`]);
    setCurrentStep(currentStep + 1);
  };
  const backward = () => {
    setModalWidth(stepWidth[`${currentStep - 1}`]);
    setCurrentStep(currentStep - 1);
  };

  const handleNext = async () => {
    await form.validateFields();
    forward();
  };

  const renderFooter = () => {
    if (currentStep === 1) {
      return (
        <>
          <Button style={{ float: 'left' }} onClick={backward}>
            上一步
          </Button>
          {/* <Button type="primary" onClick={handleNext}>
            下一步
          </Button> */}
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
    }
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" onClick={handleNext}>
          下一步
        </Button>
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
      <>
        <div style={{ display: currentStep === 1 ? 'block' : 'none' }}>
          <ViewModelConfigTransfer
            key={queryType}
            queryType={queryType}
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
            tagList={tagList}
            modelItem={selectedModelItem}
            viewItem={viewItem}
            ref={configTableRef}
          />
        </div>

        <div style={{ display: currentStep === 0 ? 'block' : 'none' }}>
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
    <Modal
      width={modalWidth}
      destroyOnClose
      title={'数据集信息'}
      open={true}
      maskClosable={false}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Steps style={{ marginBottom: 28 }} size="small" current={currentStep}>
        <Step title="基本信息" />
        <Step title="关联信息" />
        {/* <Step title="进阶设置" /> */}
      </Steps>
      <Form
        {...formLayout}
        form={form}
        initialValues={{
          ...formVals,
        }}
        onValuesChange={(value, values) => {}}
        className={styles.form}
      >
        {renderContent()}
      </Form>
    </Modal>
  );
};

export default ViewCreateFormModal;
