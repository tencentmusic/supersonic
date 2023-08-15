import React, { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Select } from 'antd';
import { SENSITIVE_LEVEL_OPTIONS } from '../constant';
import { formLayout } from '@/components/FormHelper/utils';
import SqlEditor from '@/components/SqlEditor';
import InfoTagList from './InfoTagList';
import { ISemantic } from '../data';
import { createDimension, updateDimension } from '../service';
// import DimensionValueSettingModal from './DimensionValueSettingModal';
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
  // const [dimensionValueSettingModalVisible, setDimensionValueSettingModalVisible] =
  //   useState<boolean>(false);
  const handleSubmit = async (
    isSilenceSubmit = false,
    dimValueMaps?: ISemantic.IDimensionValueSettingItem[],
  ) => {
    const fieldsValue = await form.validateFields();
    await saveDimension(
      {
        ...fieldsValue,
        dimValueMaps: dimValueMaps || dimensionValueSettingList,
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
    setFieldsValue(dimensionItem);
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
        {/* <FormItem name="dimValueMaps" label="维度值设置">
          <Button
            type="primary"
            onClick={() => {
              setDimensionValueSettingModalVisible(true);
            }}
          >
            设置
          </Button>
        </FormItem> */}
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
      {/* {dimensionValueSettingModalVisible && (
        <DimensionValueSettingModal
          dimensionValueSettingList={dimensionValueSettingList}
          open={dimensionValueSettingModalVisible}
          onCancel={() => {
            setDimensionValueSettingModalVisible(false);
          }}
          onSubmit={(dimValueMaps) => {
            if (isEdit) {
              handleSubmit(true, dimValueMaps);
            }
            setDimensionValueSettingList(dimValueMaps);
            setDimensionValueSettingModalVisible(false);
          }}
        />
      )} */}
    </>
  );
};

export default DimensionInfoModal;
