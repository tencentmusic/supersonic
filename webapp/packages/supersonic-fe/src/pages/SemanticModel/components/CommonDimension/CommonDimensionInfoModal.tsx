import React, { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Select } from 'antd';

import { formLayout } from '@/components/FormHelper/utils';
import { ISemantic } from '../../data';
import { saveCommonDimension, getDimensionList } from '../../service';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { message } from 'antd';

export type CreateFormProps = {
  domainId: number;
  dimensionItem?: ISemantic.IDimensionItem;
  onCancel: () => void;
  bindModalVisible: boolean;
  dimensionList: ISemantic.IDimensionItem[];
  onSubmit: (values?: any) => void;
};

const FormItem = Form.Item;

const { TextArea } = Input;

const CommonDimensionInfoModal: React.FC<CreateFormProps> = ({
  domainId,
  onCancel,
  bindModalVisible,
  dimensionItem,
  onSubmit: handleUpdate,
}) => {
  const isEdit = !!dimensionItem?.id;
  const [form] = Form.useForm();
  const { setFieldsValue, resetFields } = form;
  const [saveLoading, setSaveLoading] = useState<boolean>(false);
  const [dimensionOptions, setDimensionOptions] = useState([]);

  useEffect(() => {
    queryDimensionList();
  }, []);

  const queryDimensionList = async () => {
    setSaveLoading(true);
    const { code, data, msg } = await getDimensionList({ domainId });
    setSaveLoading(false);
    const dimensionList = data?.list;
    if (code === 200 && Array.isArray(dimensionList)) {
      const dimensionMap = dimensionList.reduce(
        (
          dataMap: Record<
            string,
            { label: string; options: { label: string; value: number; disabled?: boolean }[] }
          >,
          item: ISemantic.IDimensionItem,
        ) => {
          const { modelId, modelName, name, id, commonDimensionId } = item;
          const target = dataMap[modelId];
          if (target) {
            target.options.push({
              label: name,
              value: id,
              disabled: !!commonDimensionId,
            });
          } else {
            dataMap[modelId] = {
              label: modelName || `${modelId}`,
              options: [
                {
                  label: name,
                  value: id,
                  disabled: !!commonDimensionId,
                },
              ],
            };
          }
          return dataMap;
        },
        {},
      );
      setDimensionOptions(Object.values(dimensionMap));
    } else {
      message.error(msg);
    }
  };

  const handleSubmit = async (isSilenceSubmit = false) => {
    const fieldsValue = await form.validateFields();
    await saveDimension(
      {
        ...fieldsValue,
      },
      isSilenceSubmit,
    );
  };

  const saveDimension = async (fieldsValue: any, isSilenceSubmit = false) => {
    const queryParams = {
      domainId: isEdit ? dimensionItem.domainId : domainId,
      type: 'categorical',
      ...fieldsValue,
    };

    const { code, msg } = await saveCommonDimension(queryParams);
    if (code === 200) {
      if (!isSilenceSubmit) {
        message.success('编辑公共维度成功');
        handleUpdate(fieldsValue);
      }
      return;
    }
    message.error(msg);
  };

  useEffect(() => {
    if (dimensionItem) {
      setFieldsValue({ ...dimensionItem });
    } else {
      resetFields();
    }
  }, [dimensionItem]);

  const renderFooter = () => {
    return (
      <>
        <Button onClick={onCancel}>取消</Button>
        <Button
          type="primary"
          loading={saveLoading}
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
          label="公共维度名称"
          rules={[{ required: true, message: '请输入公共维度名称' }]}
        >
          <Input placeholder="名称不可重复" />
        </FormItem>
        <FormItem
          name="bizName"
          label="英文名称"
          rules={[{ required: true, message: '请输入字段名称' }]}
        >
          <Input placeholder="名称不可重复" disabled={isEdit} />
        </FormItem>
        <FormItem
          // label="关联维度"
          label={
            <FormItemTitle
              title={`关联维度`}
              subTitle={`维度不能关联多个公共维度，已关联的维度会被禁用`}
            />
          }
          name="dimensionIds"
          // rules={[{ required: true, message: '请选择所要关联的维度' }]}
        >
          <Select
            showSearch
            mode="multiple"
            allowClear
            placeholder="选择所要关联的维度"
            options={dimensionOptions}
            filterOption={(input, option: any) =>
              ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
            }
          />
        </FormItem>
        <FormItem name="description" label="维度描述">
          <TextArea placeholder="请输入维度描述" />
        </FormItem>
      </>
    );
  };

  return (
    <>
      <Modal
        width={800}
        destroyOnClose
        title="公共维度信息"
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

export default CommonDimensionInfoModal;
