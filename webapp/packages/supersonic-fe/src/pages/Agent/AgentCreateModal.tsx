import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Switch, message, TreeSelect, Select } from 'antd';
import { saveAgent, getModelList, getToolTypes } from './service';
import { AgentType, AgentToolTypeEnum, ChatAppConfig } from './type';
import { getLlmModelAppList, getLlmList } from '@/services/system';
import { getPluginList } from '@/pages/ChatPlugin/service';
import { PluginType } from '@/pages/ChatPlugin/type';
import { traverseTree } from '@/utils/utils';
import { StatusEnum } from '@/common/constants';

const { TextArea } = Input;

interface AgentCreateModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess?: (agent: AgentType) => void;
}

const AgentCreateModal: React.FC<AgentCreateModalProps> = ({
  visible,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState<any[]>([]);
  const [toolType, setToolType] = useState<AgentToolTypeEnum>();
  const [toolTypesOptions, setToolTypesOptions] = useState<{ label: string; value: string }[]>([]);
  const [plugins, setPlugins] = useState<PluginType[]>([]);

  useEffect(() => {
    if (visible) {
      form.resetFields();
      setToolType(undefined);
      loadToolTypes();
      loadDataSets();
      loadPlugins();
    }
  }, [visible]);

  const loadToolTypes = async () => {
    try {
      const { code, data } = await getToolTypes();
      if (code === 200 && data) {
        const options = Object.keys(data).map((key: string) => ({
          label: data[key],
          value: key,
        }));
        setToolTypesOptions(options);
      }
    } catch (e) {
      console.error('Failed to load tool types', e);
    }
  };

  const loadDataSets = async () => {
    try {
      const res = await getModelList();
      if (res?.data) {
        const data = traverseTree(res.data, (node: any) => {
          node.title = node.name;
          node.value = node.type === 'DOMAIN' ? `DOMAIN_${node.id}` : node.id;
          node.checkable = node.type === 'DATASET';
          node.selectable = node.type === 'DATASET';
        });
        setTreeData([{ title: '默认', value: -1, type: 'DATASET' }, ...data]);
      }
    } catch (e) {
      console.error('Failed to load datasets', e);
    }
  };

  const loadPlugins = async () => {
    try {
      const res = await getPluginList({});
      setPlugins(res.data || []);
    } catch (e) {
      console.error('Failed to load plugins', e);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      // 获取默认的 ChatApp 配置
      let chatAppConfig: ChatAppConfig = {};
      try {
        const llmListRes: any = await getLlmList();
        const defaultLlmId =
          llmListRes?.code === 200 && llmListRes?.data?.length > 0
            ? llmListRes.data[0].id
            : undefined;

        const appListRes: any = await getLlmModelAppList();
        if (appListRes?.code === 200 && appListRes?.data) {
          chatAppConfig = Object.keys(appListRes.data).reduce(
            (config: ChatAppConfig, key: string) => {
              const appConfig = appListRes.data[key];
              return {
                ...config,
                [key]: {
                  ...appConfig,
                  chatModelId: defaultLlmId,
                },
              };
            },
            {}
          );
        }
      } catch (e) {
        console.warn('Failed to load ChatApp config', e);
      }

      // 构建 toolConfig 基于选择的工具类型
      let tools: any[] = [];
      if (values.toolType === AgentToolTypeEnum.PLUGIN && values.pluginId) {
        const plugin = plugins.find((p) => p.id === values.pluginId);
        tools = [{
          id: `plugin_${values.pluginId}`,
          type: AgentToolTypeEnum.PLUGIN,
          name: values.toolName || plugin?.name,
          plugins: [values.pluginId],
        }];
      } else if (values.dataSetIds?.length > 0) {
        tools = [{
          id: `dataset_${Date.now()}`,
          type: values.toolType || AgentToolTypeEnum.DATASET,
          name: values.toolName,
          dataSetIds: values.dataSetIds,
        }];
      }

      const toolConfig = { tools };

      const agent: Partial<AgentType> = {
        name: values.name,
        description: values.description,
        enableSearch: values.enableSearch ? 1 : 0,
        status: StatusEnum.ENABLED,
        toolConfig: JSON.stringify(toolConfig),
        chatAppConfig,
        isOpen: 0,
      };

      const res = await saveAgent(agent as AgentType);
      if (res?.code === 200) {
        message.success(
          '助理创建成功！如需配置大模型、记忆、权限等高级选项，请前往「助理管理」页面',
          5
        );
        onSuccess?.(res.data);
        onCancel();
      } else {
        message.error(res?.msg || '创建失败');
      }
    } catch (error: any) {
      if (!error.errorFields) {
        message.error('创建助理失败');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="新建助理"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      okText="创建"
      cancelText="取消"
      width={520}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          enableSearch: true,
        }}
      >
        <Form.Item
          name="name"
          label="助理名称"
          rules={[{ required: true, message: '请输入助理名称' }]}
        >
          <Input placeholder="请输入助理名称" />
        </Form.Item>

        <Form.Item name="description" label="描述">
          <TextArea rows={3} placeholder="请输入助理描述" />
        </Form.Item>

        <Form.Item
          label="工具配置"
          required
          tooltip="选择助理可以访问的工具类型，如需配置更多工具请前往「助理管理」"
        >
          <Form.Item
            name="toolType"
            rules={[{ required: true, message: '请选择工具类型' }]}
            style={{ marginBottom: 16 }}
          >
            <Select
              options={toolTypesOptions}
              placeholder="请选择工具类型"
              onChange={(value) => {
                setToolType(value);
                form.setFieldsValue({ dataSetIds: undefined, pluginId: undefined, toolName: undefined });
              }}
            />
          </Form.Item>

          {toolType && (
            <Form.Item name="toolName" style={{ marginBottom: 16 }}>
              <Input placeholder="请输入工具名称（可选）" />
            </Form.Item>
          )}

          {toolType && [AgentToolTypeEnum.NL2SQL_RULE, AgentToolTypeEnum.NL2SQL_LLM, AgentToolTypeEnum.DATASET].includes(toolType) && (
            <Form.Item
              name="dataSetIds"
              noStyle
              rules={[{ required: true, message: '请选择至少一个数据集' }]}
            >
              <TreeSelect
                treeData={treeData}
                multiple
                treeCheckable
                showCheckedStrategy={TreeSelect.SHOW_CHILD}
                placeholder="请选择数据集"
                treeDefaultExpandAll
                allowClear
                filterTreeNode={(input, node) =>
                  (node?.title as string)?.toLowerCase().includes(input.toLowerCase())
                }
              />
            </Form.Item>
          )}

          {toolType === AgentToolTypeEnum.PLUGIN && (
            <Form.Item
              name="pluginId"
              noStyle
              rules={[{ required: true, message: '请选择插件' }]}
            >
              <Select
                placeholder="请选择插件"
                options={plugins.map((plugin) => ({ label: plugin.name, value: plugin.id }))}
                showSearch
                filterOption={(input, option) =>
                  ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
                }
                onChange={(value) => {
                  const plugin = plugins.find((p) => p.id === value);
                  if (plugin && !form.getFieldValue('toolName')) {
                    form.setFieldsValue({ toolName: plugin.name });
                  }
                }}
              />
            </Form.Item>
          )}
        </Form.Item>

        <Form.Item
          name="enableSearch"
          label="开启输入联想"
          valuePropName="checked"
        >
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AgentCreateModal;
