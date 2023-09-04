import React, { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Select, Row, Col, Space, Tooltip } from 'antd';
import { SENSITIVE_LEVEL_OPTIONS } from '../constant';
import { formLayout } from '@/components/FormHelper/utils';
import SqlEditor from '@/components/SqlEditor';
import InfoTagList from './InfoTagList';
import { ISemantic } from '../data';
import { InfoCircleOutlined } from '@ant-design/icons';
import { createDimension, updateDimension, mockDimensionAlias } from '../service';

import { message } from 'antd';

export type CreateFormProps = {
  modelId: number;
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
  modelId,
  onCancel,
  bindModalVisible,
  dimensionItem,
  dataSourceList,
  onSubmit: handleUpdate,
}) => {
  const isEdit = !!dimensionItem?.id;
  const [dimensionValueSettingList, setDimensionValueSettingList] = useState<
    ISemantic.IDimensionValueSettingItem[]
  >([]);
  const [form] = Form.useForm();
  const { setFieldsValue, resetFields } = form;
  const [llmLoading, setLlmLoading] = useState<boolean>(false);

  const handleSubmit = async (
    isSilenceSubmit = false,
    dimValueMaps?: ISemantic.IDimensionValueSettingItem[],
  ) => {
    const fieldsValue = await form.validateFields();
    await saveDimension(
      {
        ...fieldsValue,
        dimValueMaps: dimValueMaps || dimensionValueSettingList,
        alias: Array.isArray(fieldsValue.alias) ? fieldsValue.alias.join(',') : '',
      },
      isSilenceSubmit,
    );
  };

  const saveDimension = async (fieldsValue: any, isSilenceSubmit = false) => {
    const queryParams = {
      modelId,
      type: 'categorical',
      ...fieldsValue,
    };
    let saveDimensionQuery = createDimension;
    if (queryParams.id) {
      saveDimensionQuery = updateDimension;
    }
    const { code, msg } = await saveDimensionQuery(queryParams);
    if (code === 200) {
      if (!isSilenceSubmit) {
        message.success('编辑维度成功');
        handleUpdate(fieldsValue);
      }
      return;
    }
    message.error(msg);
  };

  const setFormVal = () => {
    if (dimensionItem) {
      const { alias } = dimensionItem;
      setFieldsValue({ ...dimensionItem, alias: alias && alias.trim() ? alias.split(',') : [] });
    }
  };

  useEffect(() => {
    if (dimensionItem) {
      setFormVal();
      if (Array.isArray(dimensionItem.dimValueMaps)) {
        setDimensionValueSettingList(dimensionItem.dimValueMaps);
      } else {
        setDimensionValueSettingList([]);
      }
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
        <Button
          type="primary"
          onClick={() => {
            handleSubmit();
          }}
        >
          完成
        </Button>
      </>
    );
  };

  const generatorDimensionAlias = async () => {
    const fieldsValue = await form.validateFields();
    setLlmLoading(true);
    const { code, data } = await mockDimensionAlias({
      ...dimensionItem,
      ...fieldsValue,
      alias: fieldsValue.alias?.join(','),
    });
    setLlmLoading(false);
    const formAlias = form.getFieldValue('alias');
    setLlmLoading(false);
    if (code === 200) {
      form.setFieldValue('alias', Array.from(new Set([...formAlias, ...data])));
    } else {
      message.error('大语言模型解析异常');
    }
  };

  const renderContent = () => {
    return (
      <>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem
          name="name"
          label="维度名称"
          rules={[{ required: true, message: '请输入维度名称' }]}
        >
          <Input placeholder="名称不可重复" />
        </FormItem>
        <FormItem
          hidden={isEdit}
          name="bizName"
          label="字段名称"
          rules={[{ required: true, message: '请输入字段名称' }]}
        >
          <Input placeholder="名称不可重复" disabled={isEdit} />
        </FormItem>
        <FormItem
          hidden={isEdit}
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
        <FormItem label="别名">
          <Row>
            <Col flex="1 1 200px">
              <FormItem name="alias" noStyle>
                <Select
                  mode="tags"
                  placeholder="输入别名后回车确认，多别名输入、复制粘贴支持英文逗号自动分隔"
                  tokenSeparators={[',']}
                  maxTagCount={9}
                />
              </FormItem>
            </Col>
            {isEdit && (
              <Col flex="0 1 75px">
                <Button
                  type="link"
                  size="small"
                  loading={llmLoading}
                  style={{ top: '2px' }}
                  onClick={() => {
                    generatorDimensionAlias();
                  }}
                >
                  <Space>
                    智能填充
                    <Tooltip title="智能填充将根据维度相关信息，使用大语言模型获取维度别名">
                      <InfoCircleOutlined />
                    </Tooltip>
                  </Space>
                </Button>
              </Col>
            )}
          </Row>
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
    <>
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
        <Form {...formLayout} form={form}>
          {renderContent()}
        </Form>
      </Modal>
    </>
  );
};

export default DimensionInfoModal;
