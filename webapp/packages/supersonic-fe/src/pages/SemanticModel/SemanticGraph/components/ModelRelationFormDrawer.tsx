import React, { useEffect, useState } from 'react';
import { Form, Button, Drawer, Space, Input, Select, message, Popconfirm } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { TYPE_OPTIONS_LABEL } from '@/pages/SemanticModel/Datasource/constants';
import { createOrUpdateModelRela, deleteModelRela, getModelDetail } from '../../service';

export type ModelRelationFormDrawerProps = {
  domainId: number;
  nodeModel: any;
  relationData: any;
  open: boolean;
  onSave?: () => void;
  onDelete?: () => void;
  onClose?: () => void;
};

const FormItem = Form.Item;

const ModelRelationFormDrawer: React.FC<ModelRelationFormDrawerProps> = ({
  domainId,
  open,
  nodeModel,
  relationData,
  onSave,
  onDelete,
  onClose,
}) => {
  const [form] = Form.useForm();
  const [saveLoading, setSaveLoading] = useState<boolean>(false);

  const [sourcePrimaryOptions, setSourcePrimaryOptions] = useState<OptionsItem[]>([]);
  const [targetPrimaryOptions, setTargetPrimaryOptions] = useState<OptionsItem[]>([]);

  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);

  useEffect(() => {
    if (!relationData?.id) {
      form.resetFields();
      return;
    }
    const { joinConditions = [] } = relationData;
    const firstCondition = joinConditions[0] || {};
    const formData = {
      ...relationData,
      ...firstCondition,
    };
    form.setFieldsValue(formData);
  }, [relationData]);

  const queryModelDetail = async (modelId: number, isSource: boolean) => {
    if (!modelId) {
      return;
    }
    const { code, data } = await getModelDetail({ modelId });
    if (code === 200) {
      if (Array.isArray(data?.modelDetail?.identifiers)) {
        const dataSourceIdentifiers = data.modelDetail.identifiers;
        const options = dataSourceIdentifiers.map((item: any) => {
          const typeLabel = TYPE_OPTIONS_LABEL[item.type];
          return {
            label: `${item.bizName}${item.name ? `(${item.name})` : ''}${
              typeLabel ? `-${typeLabel}` : ''
            }`,
            value: item.bizName,
          };
        });
        if (isSource) {
          setSourcePrimaryOptions(options);
        } else {
          setTargetPrimaryOptions(options);
        }
      }
    }
  };

  useEffect(() => {
    const { sourceData, targetData } = nodeModel;
    queryModelDetail(sourceData.uid, true);
    queryModelDetail(targetData.uid, false);
  }, [nodeModel]);

  const renderContent = () => {
    return (
      <>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem label="起始模型:">
          <span style={{ color: '#296df3', fontWeight: 500 }}>{nodeModel?.sourceData?.name}</span>
        </FormItem>
        <FormItem label="目标模型:">
          <span style={{ color: '#296df3', fontWeight: 500 }}>{nodeModel?.targetData?.name}</span>
        </FormItem>
        <FormItem
          name="leftField"
          label="起始关联字段:"
          rules={[{ required: true, message: '请选择关联主键' }]}
        >
          <Select placeholder="请选择关联主键" options={sourcePrimaryOptions} />
        </FormItem>
        <FormItem
          name="rightField"
          label="目标关联字段:"
          rules={[{ required: true, message: '请选择关联主键' }]}
        >
          <Select placeholder="请选择关联主键" options={targetPrimaryOptions} />
        </FormItem>

        <FormItem
          name="operator"
          label="算子"
          rules={[{ required: true, message: '请选择关联算子' }]}
        >
          <Select
            placeholder="请选择关联算子"
            options={[
              { label: '=', value: '=' },
              { label: '!=', value: '!=' },
              { label: '>', value: '>' },
              { label: '>=', value: '>=' },
              { label: '<', value: '<' },
              { label: '<=', value: '<=' },
              // { label: 'IN', value: 'IN' },
              // { label: 'NOT_IN', value: 'NOT_IN' },
              // { label: 'EQUALS', value: 'EQUALS' },
              // { label: 'BETWEEN', value: 'BETWEEN' },
              // { label: 'GREATER_THAN', value: 'GREATER_THAN' },
              // { label: 'GREATER_THAN_EQUALS', value: 'GREATER_THAN_EQUALS' },
              // { label: 'IS_NULL', value: 'IS_NULL' },
              // { label: 'IS_NOT_NULL', value: 'IS_NOT_NULL' },
              // { label: 'LIKE', value: 'LIKE' },
              // { label: 'MINOR_THAN', value: 'MINOR_THAN' },
              // { label: 'MINOR_THAN_EQUALS', value: 'MINOR_THAN_EQUALS' },
              // { label: 'NOT_EQUALS', value: 'NOT_EQUALS' },
              // { label: 'SQL_PART', value: 'SQL_PART' },
              // { label: 'EXISTS', value: 'EXISTS' },
            ]}
          />
        </FormItem>

        <FormItem
          name="joinType"
          label="join类型"
          rules={[{ required: true, message: '请选择join类型' }]}
        >
          <Select
            placeholder="请选择join类型"
            options={[
              { label: 'left join', value: 'left join' },
              { label: 'inner join', value: 'inner join' },
              { label: 'right join', value: 'right join' },
              { label: 'outer join', value: 'outer join' },
            ]}
          />
        </FormItem>
      </>
    );
  };

  const saveRelation = async () => {
    const values = await form.validateFields();
    setSaveLoading(true);
    const { code, msg } = await createOrUpdateModelRela({
      id: values.id,
      domainId,
      fromModelId: nodeModel?.sourceData?.uid,
      toModelId: nodeModel?.targetData?.uid,
      joinType: values.joinType,
      joinConditions: [
        {
          ...values,
        },
      ],
    });
    setSaveLoading(false);
    if (code === 200) {
      message.success('保存成功');
      onSave?.();
      return;
    }
    message.error(msg);
  };

  const deleteRelation = async () => {
    setDeleteLoading(true);
    const { code, msg } = await deleteModelRela(relationData?.id);
    setDeleteLoading(false);
    if (code === 200) {
      message.success('删除成功');
      onDelete?.();
      return;
    }
    message.error(msg);
  };

  const renderFooter = () => {
    return (
      <Space>
        <Button
          onClick={() => {
            onClose?.();
          }}
        >
          取 消
        </Button>

        <Popconfirm
          title="确定删除吗？"
          onCancel={(e) => {
            e?.stopPropagation();
          }}
          onConfirm={() => {
            if (relationData?.id) {
              deleteRelation();
            } else {
              onDelete?.();
            }
          }}
        >
          <Button type="primary" danger loading={deleteLoading}>
            删 除
          </Button>
        </Popconfirm>

        <Button
          type="primary"
          loading={saveLoading}
          onClick={() => {
            saveRelation();
          }}
        >
          保 存
        </Button>
      </Space>
    );
  };

  return (
    <Drawer
      forceRender
      width={400}
      destroyOnClose
      getContainer={false}
      title={'模型关联信息'}
      mask={false}
      open={open}
      footer={renderFooter()}
      onClose={() => {
        onClose?.();
      }}
    >
      <Form {...formLayout} form={form}>
        {renderContent()}
      </Form>
    </Drawer>
  );
};

export default ModelRelationFormDrawer;
