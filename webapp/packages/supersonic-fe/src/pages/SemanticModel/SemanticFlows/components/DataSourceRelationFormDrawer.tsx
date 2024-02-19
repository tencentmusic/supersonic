import React, { useEffect, useState } from 'react';
import { Form, Button, Drawer, Space, Input, Select, message } from 'antd';
import { formLayout } from '@/components/FormHelper/utils';
import { createOrUpdateDatasourceRela } from '../../service';
import { getRelationConfigInfo } from '../utils';
import { useXFlowApp } from '@antv/xflow';
import { CustomCommands } from '../CmdExtensions/constants';

export type DataSourceRelationFormDrawerProps = {
  domainId: number;
  nodeDataSource: any;
  open: boolean;
  onClose?: () => void;
};

const FormItem = Form.Item;
const { Option } = Select;

const DataSourceRelationFormDrawer: React.FC<DataSourceRelationFormDrawerProps> = ({
  domainId,
  open,
  nodeDataSource,
  onClose,
}) => {
  const [form] = Form.useForm();
  const [saveLoading, setSaveLoading] = useState(false);
  const [dataSourceOptions, setDataSourceOptions] = useState<any[]>([]);

  const app = useXFlowApp();

  const getRelationListInfo = async () => {
    await app.commandService.executeCommand(CustomCommands.DATASOURCE_RELATION.id, {});
  };

  useEffect(() => {
    const { sourceData, targetData } = nodeDataSource;
    const dataSourceFromIdentifiers = sourceData?.modelDetail?.identifiers || [];
    const dataSourceToIdentifiers = targetData?.modelDetail?.identifiers || [];
    const dataSourceToIdentifiersNames = dataSourceToIdentifiers.map((item) => {
      return item.bizName;
    });
    const keyOptions = dataSourceFromIdentifiers.reduce((options: any[], item: any) => {
      const { bizName } = item;
      if (dataSourceToIdentifiersNames.includes(bizName)) {
        options.push(item);
      }
      return options;
    }, []);
    setDataSourceOptions(
      keyOptions.map((item: any) => {
        const { name, bizName } = item;
        return {
          label: name,
          value: bizName,
        };
      }),
    );
  }, [nodeDataSource]);

  useEffect(() => {
    const { sourceData, targetData } = nodeDataSource;
    if (!sourceData || !targetData) {
      return;
    }
    const relationList = app.commandService.getGlobal('dataSourceRelationList') || [];
    const config = getRelationConfigInfo(sourceData.id, targetData.id, relationList);
    if (config) {
      form.setFieldsValue({
        joinKey: config.joinKey,
      });
    } else {
      form.setFieldsValue({
        joinKey: '',
      });
    }
  }, [nodeDataSource]);

  const renderContent = () => {
    return (
      <>
        <FormItem hidden={true} name="id" label="ID">
          <Input placeholder="id" />
        </FormItem>
        <FormItem label="主模型:">{nodeDataSource?.sourceData?.name}</FormItem>
        <FormItem label="关联模型:">{nodeDataSource?.targetData?.name}</FormItem>
        <FormItem
          name="joinKey"
          label="可关联Key:"
          tooltip="主从模型中必须具有相同的主键或外键才可建立关联关系"
          rules={[{ required: true, message: '请选择关联Key' }]}
        >
          <Select placeholder="请选择关联Key">
            {dataSourceOptions.map((item) => (
              <Option key={item.value} value={item.value}>
                {item.label}
              </Option>
            ))}
          </Select>
        </FormItem>
      </>
    );
  };

  const saveRelation = async () => {
    const values = await form.validateFields();
    setSaveLoading(true);
    const { code, msg } = await createOrUpdateDatasourceRela({
      domainId,
      datasourceFrom: nodeDataSource?.sourceData?.id,
      datasourceTo: nodeDataSource?.targetData?.id,
      ...values,
    });
    setSaveLoading(false);
    if (code === 200) {
      message.success('保存成功');
      getRelationListInfo();
      onClose?.();
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
          取消
        </Button>
        <Button
          type="primary"
          loading={saveLoading}
          onClick={() => {
            saveRelation();
          }}
        >
          完成
        </Button>
      </Space>
    );
  };

  return (
    <Drawer
      forceRender
      width={400}
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

export default DataSourceRelationFormDrawer;
