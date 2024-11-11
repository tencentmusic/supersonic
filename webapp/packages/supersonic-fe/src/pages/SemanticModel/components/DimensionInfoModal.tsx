import React, { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Select, Row, Col, Space, Tooltip, Switch } from 'antd';
import { SENSITIVE_LEVEL_OPTIONS, TAG_DEFINE_TYPE } from '../constant';
import { StatusEnum } from '../enum';
import { formLayout } from '@/components/FormHelper/utils';
import SqlEditor from '@/components/SqlEditor';
import InfoTagList from './InfoTagList';
import { ISemantic } from '../data';
import {
  DIM_OPTIONS,
  EnumDataSourceType,
  PARTITION_TIME_FORMATTER,
  DATE_FORMATTER,
} from '@/pages/SemanticModel/Datasource/constants';
import { InfoCircleOutlined } from '@ant-design/icons';
import {
  createDimension,
  updateDimension,
  mockDimensionAlias,
  batchCreateTag,
  batchDeleteTag,
} from '../service';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';

import { message } from 'antd';
import { values } from 'lodash';

export type CreateFormProps = {
  modelId: number;
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
  const [formData, setFormData] = useState<ISemantic.IDimensionItem>();

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
      modelId: isEdit ? dimensionItem.modelId : modelId,
      type: 'categorical',
      ...fieldsValue,
    };
    let saveDimensionQuery = createDimension;
    if (queryParams.id) {
      saveDimensionQuery = updateDimension;
    }
    const { code, msg, data } = await saveDimensionQuery(queryParams);
    if (code === 200) {
      if (queryParams.isTag) {
        queryBatchExportTag(data.id || dimensionItem?.id);
      }
      if (dimensionItem?.id && !queryParams.isTag) {
        queryBatchDeleteTag(dimensionItem);
      }
      if (!isSilenceSubmit) {
        message.success('编辑维度成功');
        handleUpdate(fieldsValue);
      }
      return;
    }
    message.error(msg);
  };

  const queryBatchDeleteTag = async (dimensionItem: ISemantic.IDimensionItem) => {
    const { code, msg } = await batchDeleteTag([
      {
        itemIds: [dimensionItem.id],
        tagDefineType: TAG_DEFINE_TYPE.DIMENSION,
      },
    ]);
    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const queryBatchExportTag = async (id: number) => {
    const { code, msg } = await batchCreateTag([
      { itemId: id, tagDefineType: TAG_DEFINE_TYPE.DIMENSION },
    ]);

    if (code === 200) {
      return;
    }
    message.error(msg);
  };

  const setFormVal = () => {
    if (dimensionItem) {
      const { alias } = dimensionItem;
      const dimensionData = {
        ...dimensionItem,
        alias: alias && alias.trim() ? alias.split(',') : [],
      };
      setFieldsValue(dimensionData);
      setFormData(dimensionData);
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
          label="英文名称"
          rules={[{ required: true, message: '请输入英文名称' }]}
        >
          <Input placeholder="名称不可重复" disabled={isEdit} />
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
        <FormItem name="type" label="类型" rules={[{ required: true, message: '请选择维度类型' }]}>
          <Select placeholder="请选择维度类型">
            {DIM_OPTIONS.map((item) => (
              <Option key={item.value} value={item.value}>
                {item.label}
              </Option>
            ))}
          </Select>
        </FormItem>
        {formData?.type &&
          [EnumDataSourceType.PARTITION_TIME, EnumDataSourceType.TIME].includes(formData.type) && (
            <FormItem
              name={['ext', 'time_format']}
              label="时间格式"
              rules={[{ required: true, message: '请选择时间格式' }]}
              tooltip="请选择数据库中时间字段对应格式"
            >
              <Select placeholder="请选择维度类型">
                {(formData?.type === EnumDataSourceType.TIME
                  ? DATE_FORMATTER
                  : PARTITION_TIME_FORMATTER
                ).map((item) => (
                  <Option key={item} value={item}>
                    {item}
                  </Option>
                ))}
              </Select>
            </FormItem>
          )}

        <FormItem
          name="semanticType"
          label="类型"
          hidden={true}
          // rules={[{ required: true, message: '请选择维度类型' }]}
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
        {/* <FormItem name="commonDimensionId" label="公共维度">
          <Select placeholder="请绑定公共维度" allowClear options={commonDimensionOptions} />
        </FormItem> */}
        {/* <FormItem name="defaultValues" label="默认值">
          <InfoTagList />
        </FormItem> */}
        <Form.Item
          hidden={!!!process.env.SHOW_TAG}
          label={
            <FormItemTitle
              title={`设为标签`}
              subTitle={`如果勾选，代表维度的取值都是一种'标签'，可用作对实体的圈选`}
            />
          }
          name="isTag"
          valuePropName="checked"
          getValueFromEvent={(value) => {
            return value === true ? 1 : 0;
          }}
          getValueProps={(value) => {
            return {
              checked: value === 1,
            };
          }}
        >
          <Switch />
        </Form.Item>
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
          tooltip="表达式中的字段必须在创建模型的时候被标记为日期或者维度"
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
        <Form
          {...formLayout}
          form={form}
          onValuesChange={(value, values) => {
            setFormData(values);
          }}
        >
          {renderContent()}
        </Form>
      </Modal>
    </>
  );
};

export default DimensionInfoModal;
