import React, { useEffect } from 'react';
import { Button, Form, Input, Modal, Select } from 'antd';
import { SENSITIVE_LEVEL_OPTIONS } from '../constant';
import { formLayout } from '@/components/FormHelper/utils';
import SqlEditor from '@/components/SqlEditor';
import InfoTagList from './InfoTagList';
import { ISemantic } from '../data';
import { createDimension, updateDimension } from '../service';
import { message } from 'antd';

export type CreateFormProps = {
  domainId: number;
  dimensionItem?: ISemantic.IDimensionItem;
  onCancel: () => void;
  bindModalVisible: boolean;
  dataSourceList: any[];
  onSubmit: (values?: any) => void;
};

const FormItem = Form.Item;
const { Option } = Select;

const { TextArea } = Input;

const DimensionInfoModal: React.FC<CreateFormProps> = ({
  domainId,
  onCancel,
  bindModalVisible,
  dimensionItem,
  dataSourceList,
  onSubmit: handleUpdate,
}) => {
  const isEdit = !!dimensionItem?.id;

  const [form] = Form.useForm();
  const { setFieldsValue, resetFields } = form;

  const handleSubmit = async () => {
    const fieldsValue = await form.validateFields();
    await saveDimension(fieldsValue);
  };

  const saveDimension = async (fieldsValue: any) => {
    const queryParams = {
      domainId,
      type: 'categorical',
      ...fieldsValue,
    };
    let saveDimensionQuery = createDimension;
    if (queryParams.id) {
      saveDimensionQuery = updateDimension;
    }
    const { code, msg } = await saveDimensionQuery(queryParams);
    if (code === 200) {
      message.success('编辑维度成功');
      handleUpdate(fieldsValue);
      return;
    }
    message.error(msg);
  };

  const setFormVal = () => {
    setFieldsValue(dimensionItem);
  };

  useEffect(() => {
    if (dimensionItem) {
      setFormVal();
    } else {
      resetFields();
    }
    if (!isEdit && Array.isArray(dataSourceList) && dataSourceList[0]?.id) {
      setFieldsValue({ datasourceId: dataSourceList[0].id });
    }
  }, [dimensionItem, dataSourceList]);

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" onClick={handleSubmit}>
          完成
        </Button>
      </>
    );
  };

  const renderContent = () => {
    return (
      <>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name="name"
          label="维度中文名"
          rules={[{ required: true, message: '请输入维度中文名' }]}
        >
          <Input placeholder="名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label="维度英文名"
          rules={[{ required: true, message: '请输入维度英文名' }]}
        >
          <Input placeholder="名称不可重复" disabled={isEdit} />
        </FormItem>

        <FormItem
          name="datasourceId"
          label="所属数据源"
          rules={[{ required: true, message: '请选择所属数据源' }]}
        >
          <Select placeholder="请选择数据源" disabled={isEdit}>
            {dataSourceList.map((item) => (
              <Option key={item.id} value={item.id}>
                {item.name}
              </Option>
            ))}
          </Select>
        </FormItem>
        <FormItem name="alias" label="别名">
          <Input placeholder="多个别名用英文逗号隔开" />
        </FormItem>
        <FormItem
          name="semanticType"
          label="类型"
          rules={[{ required: true, message: '请选择维度类型' }]}
        >
          <Select placeholder="请选择维度类型">
            {['CATEGORY', 'ID', 'DATE'].map((item) => (
              <Option key={item} value={item}>
                {item}
              </Option>
            ))}
          </Select>
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
        <FormItem name="defaultValues" label="默认值">
          <InfoTagList />
        </FormItem>
        <FormItem
          name="description"
          label="维度描述"
          rules={[{ required: true, message: '请输入维度描述' }]}
        >
          <TextArea placeholder="请输入维度描述" />
        </FormItem>
        <FormItem
          name="expr"
          label="表达式"
          tooltip="表达式中的字段必须在创建数据源的时候被标记为日期或者维度"
          rules={[{ required: true, message: '请输入表达式' }]}
        >
          <SqlEditor height={'150px'} />
        </FormItem>
      </>
    );
  };

  return (
    <Modal
      width={800}
      destroyOnClose
      title="维度信息"
      style={{ top: 48 }}
      maskClosable={false}
      open={bindModalVisible}
      footer={renderFooter()}
      onCancel={onCancel}
    >
      <Form
        {...formLayout}
        form={form}
        initialValues={
          {
            // ...formVals,
          }
        }
      >
        {renderContent()}
      </Form>
    </Modal>
  );
};

export default DimensionInfoModal;
