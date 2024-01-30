import React, { useState, useEffect, useRef } from 'react';
import { Form, Button, Modal, Input, Select, Steps } from 'antd';
import styles from '../../components/style.less';
import { message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { createView, updateView } from '../../service';
import { ISemantic } from '../../data';
import { isString } from 'lodash';
import ViewModelConfigTable from './ViewModelConfigTable';
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

  const [submitData, setSubmitData] = useState({});
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
  }, [viewItem]);

  const handleConfirm = async () => {
    const fieldsValue = await form.validateFields();
    const viewModelConfigsMap = configTableRef?.current.getViewModelConfigs() || {};
    const queryData: ISemantic.IModelItem = {
      ...formVals,
      ...fieldsValue,
      ...submitData,
      viewDetail: {
        viewModelConfigs: Object.values(viewModelConfigsMap),
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

  const footer = (
    <>
      <Button onClick={onCancel}>取消</Button>
      <Button type="primary" loading={saveLoading} onClick={handleConfirm}>
        确定
      </Button>
    </>
  );

  const forward = () => {
    setModalWidth(1200);
    setCurrentStep(currentStep + 1);
  };
  const backward = () => {
    setModalWidth(800);
    setCurrentStep(currentStep - 1);
  };

  const handleNext = async () => {
    const fieldsValue = await form.validateFields();
    const submitForm = {
      ...submitData,
      ...fieldsValue,
    };
    setSubmitData(submitForm);
    if (currentStep < 1) {
      forward();
    } else {
      // await saveMetric(submitForm);
    }
  };

  const renderFooter = () => {
    if (currentStep === 1) {
      return (
        <>
          <Button style={{ float: 'left' }} onClick={backward}>
            上一步
          </Button>
          <Button onClick={onCancel}>取消</Button>
          {/* <Button type="primary" onClick={handleNext}> */}
          <Button
            type="primary"
            onClick={() => {
              handleConfirm();
            }}
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

  const renderContent = () => {
    if (currentStep === 1) {
      return (
        <div>
          <FormItem
            name="currentModel"
            label="选择模型"
            rules={[{ required: true, message: '请选择模型！' }]}
          >
            <Select
              placeholder="请选择模型，获取当前模型下指标维度信息"
              onChange={(val) => {
                const modelItem = modelList.find((item) => item.id === val);
                setSelectedModelItem(modelItem);
              }}
              options={modelList.map((item) => {
                return { label: item.name, value: item.id };
              })}
            />
          </FormItem>
          <ViewModelConfigTransfer
            modelItem={selectedModelItem}
            viewItem={viewItem}
            ref={configTableRef}
          />
        </div>
      );
    }

    return (
      <>
        <FormItem
          name="name"
          label="视图名称"
          rules={[{ required: true, message: '请输入视图名称！' }]}
        >
          <Input placeholder="视图名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label="视图英文名称"
          rules={[{ required: true, message: '请输入视图英文名称！' }]}
        >
          <Input placeholder="请输入视图英文名称" />
        </FormItem>
        <FormItem
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
        </FormItem>
        <FormItem name="description" label="视图描述">
          <Input.TextArea placeholder="视图描述" />
        </FormItem>
      </>
    );
  };

  return (
    <Modal
      width={modalWidth}
      destroyOnClose
      title={'视图信息'}
      open={true}
      // footer={footer}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Steps style={{ marginBottom: 28 }} size="small" current={currentStep}>
        <Step title="基本信息" />
        <Step title="关联信息" />
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
