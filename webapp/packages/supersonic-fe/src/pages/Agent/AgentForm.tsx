import { Form, Input, Button, Switch, Tabs, Select, message, Space, Tooltip } from 'antd';
import MainTitleMark from '@/components/MainTitleMark';
import { AgentType, ChatAppConfig } from './type';
import { useEffect, useState } from 'react';
import styles from './style.less';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { uuid, jsonParse } from '@/utils/utils';
import ToolsSection from './ToolsSection';
import globalStyles from '@/global.less';
import { QuestionCircleOutlined } from '@ant-design/icons';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { getLlmModelAppList, getLlmList } from '../../services/system';
import MemorySection from './MemorySection';
import PermissionSection from './PermissionSection';

const FormItem = Form.Item;
const { TextArea } = Input;

type Props = {
  editAgent?: AgentType;
  onSaveAgent: (agent: AgentType) => Promise<void>;
  onCreateToolBtnClick?: () => void;
};

const defaultAgentConfig = {
  simpleMode: false,
  debugMode: true,
};

const AgentForm: React.FC<Props> = ({ editAgent, onSaveAgent, onCreateToolBtnClick }) => {
  const [saveLoading, setSaveLoading] = useState(false);
  const [examples, setExamples] = useState<{ id: string; question?: string }[]>([]);
  const [activeKey, setActiveKey] = useState('basic');
  const [modelTypeOptions, setModelTypeOptions] = useState<
    (OptionsItem & { enable: boolean; prompt: string; description: string })[]
  >([]);
  const [llmConfigListOptions, setLlmConfigListOptions] = useState<OptionsItem[]>([]);
  const [currentChatModel, setCurrentChatModel] = useState<string>('');
  const [defaultChatAppConfig, setDefaultChatAppConfig] = useState<ChatAppConfig>({});
  const [formData, setFormData] = useState<any>({
    enableSearch: true,
    modelConfig: {
      timeOut: 60,
      provider: 'OPEN_AI',
      temperature: 0,
    },
    toolConfig: {
      ...defaultAgentConfig,
    },
  });
  const [form] = Form.useForm();

  useEffect(() => {
    if (editAgent) {
      const config = jsonParse(editAgent.toolConfig, {});
      const initData = {
        ...editAgent,
        enableSearch: editAgent.enableSearch !== 0,
        enableFeedback: editAgent.enableFeedback !== 0,
        toolConfig: { ...defaultAgentConfig, ...config },
      };

      form.setFieldsValue(initData);
      setFormData(initData);
      if (editAgent.examples) {
        setExamples(editAgent.examples.map((question) => ({ id: uuid(), question })));
      }
    } else {
      form.resetFields();
    }
    queryModelTypeList(editAgent?.chatAppConfig);
    queryLlmList();
  }, [editAgent]);

  const queryLlmList = async () => {
    const { code, data } = await getLlmList();
    if (code === 200 && data) {
      const options = data.map((item) => {
        return {
          label: item.name,
          value: item.id,
        };
      });
      setLlmConfigListOptions(options);
    } else {
      message.error('获取模型场景类型失败');
    }
  };
  const queryModelTypeList = async (currentAgentChatConfig: any = {}) => {
    const { code, data } = await getLlmModelAppList();
    if (code === 200 && data) {
      let options = Object.keys(data).map((key: string) => {
        let config = data[key];
        if (currentAgentChatConfig[key]) {
          config = currentAgentChatConfig[key];
        }
        return {
          label: config.name,
          value: key,
          enable: config.enable,
          description: config.description,
          prompt: config.prompt,
        };
      });
      const sqlParserIndex = options.findIndex((item) => item.value === 'S2SQL_PARSER');
      if (sqlParserIndex >= 0) {
        options.splice(0, 0, options.splice(sqlParserIndex, 1)[0]);
      }
      const firstOption = options[0];
      if (firstOption) {
        setCurrentChatModel(firstOption.value);
      }
      const initChatModelConfig = Object.keys(data).reduce(
        (modelConfig: ChatAppConfig, key: string) => {
          let config = data[key];
          if (currentAgentChatConfig[key]) {
            config = currentAgentChatConfig[key];
          }
          return {
            ...modelConfig,
            [key]: config,
          };
        },
        {},
      );
      setDefaultChatAppConfig(initChatModelConfig);
      const formData = form.getFieldsValue();
      form.setFieldsValue({
        ...formData,
        chatAppConfig: initChatModelConfig,
      });
      setModelTypeOptions(options);
    } else {
      message.error('获取模型场景类型失败');
    }
  };

  const layout = {
    labelCol: { span: 8 },
    wrapperCol: { span: 16 },
  };

  const onOk = async () => {
    const values = await form.validateFields();
    setSaveLoading(true);
    const config = jsonParse(editAgent?.toolConfig, {});
    await onSaveAgent?.({
      id: editAgent?.id,
      ...(editAgent || {}),
      ...values,
      toolConfig: JSON.stringify({
        ...config,
        ...values.toolConfig,
        debugMode: values.toolConfig?.simpleMode === true ? false : values.toolConfig?.debugMode,
      }) as any,
      examples: examples.map((example) => example.question),
      enableSearch: values.enableSearch ? 1 : 0,
      enableFeedback: values.enableFeedback ? 1 : 0,
      chatAppConfig: Object.keys(defaultChatAppConfig).reduce((mergeConfig, key) => {
        return {
          ...mergeConfig,
          [key]: {
            ...defaultChatAppConfig[key],
            ...(values.chatAppConfig?.[key] ? values.chatAppConfig[key] : {}),
          },
        };
      }, {}),
    });
    setSaveLoading(false);
  };

  const tips = [
    '自定义提示词模板可嵌入以下变量，将由系统自动进行替换：',
    '-{{exemplar}} :替换成few-shot示例，示例个数由系统配置',
    '-{{question}} :替换成用户问题，拼接了一定的补充信息',
    '-{{schema}} :替换成数据语义信息，根据用户问题映射而来',
  ];

  const formTabList = [
    {
      label: '基本信息',
      key: 'basic',
      children: (
        <div className={styles.agentFormContainer}>
          <FormItem
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入助理名称' }]}
          >
            <Input placeholder="请输入助理名称" />
          </FormItem>
          <FormItem name="enableSearch" label="开启输入联想" valuePropName="checked" htmlFor="">
            <Switch />
          </FormItem>
          <FormItem name="enableFeedback" label="开启用户确认" valuePropName="checked" htmlFor="">
            <Switch />
          </FormItem>
          <FormItem
            name={['toolConfig', 'simpleMode']}
            label="开启精简模式"
            tooltip="精简模式下不可调整查询条件、不显示调试信息、不显示可视化组件"
            valuePropName="checked"
            htmlFor=""
          >
            <Switch />
          </FormItem>
          <FormItem
            name={['toolConfig', 'debugMode']}
            label="开启调试信息"
            hidden={formData?.toolConfig?.simpleMode === true}
            tooltip="包含Schema映射、SQL生成每阶段的关键信息"
            valuePropName="checked"
            htmlFor=""
          >
            <Switch />
          </FormItem>
          <FormItem
            name="admins"
            label="管理员"
            // rules={[{ required: true, message: '请设定数据库连接管理者' }]}
          >
            <SelectTMEPerson placeholder="请邀请团队成员" />
          </FormItem>
          <FormItem tooltip="选择用户后，该助理只对所选用户可见" name="viewers" label="使用者">
            <SelectTMEPerson placeholder="请邀请团队成员" />
          </FormItem>
          <FormItem name="examples" label="示例问题">
            <div className={styles.paramsSection}>
              {examples.map((example) => {
                const { id, question } = example;
                return (
                  <div className={styles.filterRow} key={id}>
                    <Input
                      placeholder="示例问题"
                      value={question}
                      className={styles.questionExample}
                      onChange={(e) => {
                        example.question = e.target.value;
                        setExamples([...examples]);
                      }}
                      allowClear
                    />
                    <DeleteOutlined
                      onClick={() => {
                        setExamples(examples.filter((item) => item.id !== id));
                      }}
                    />
                  </div>
                );
              })}
              <Button
                onClick={() => {
                  setExamples([...examples, { id: uuid() }]);
                }}
              >
                <PlusOutlined />
                新增示例问题
              </Button>
            </div>
          </FormItem>
          <FormItem name="description" label="描述">
            <TextArea placeholder="请输入助理描述" />
          </FormItem>
        </div>
      ),
    },
    {
      label: '大模型配置',
      key: 'modelConfig',
      children: (
        <div className={styles.agentFormContainer} style={{ width: '1200px', marginTop: 20 }}>
          <div className={styles.agentFormTitle}>
            <Space>
              应用场景 <MainTitleMark />
            </Space>
          </div>
          <Space style={{ alignItems: 'start' }}>
            <div style={{ width: 350 }}>
              {modelTypeOptions.map((item) => {
                return (
                  <div
                    className={`${styles.agentChatModelCell} ${
                      currentChatModel === item.value ? styles.agentChatModelCellActive : ''
                    }`}
                    onClick={() => {
                      setCurrentChatModel(item.value);
                    }}
                  >
                    <FormItem
                      name={['chatAppConfig', item.value, 'enable']}
                      label={item.label}
                      valuePropName="checked"
                      tooltip={item.description}
                      htmlFor=""
                    >
                      <Switch />
                    </FormItem>
                  </div>
                );
              })}
            </div>
            <div style={{ width: 900 }}>
              {modelTypeOptions.map((item) => {
                return (
                  <div
                    key={`setting-${item.value}`}
                    style={{
                      display: currentChatModel === item.value ? 'block' : 'none',
                    }}
                  >
                    <FormItem
                      name={['chatAppConfig', item.value, 'chatModelId']}
                      label="应用模型"
                      tooltip={item.description}
                    >
                      <Select placeholder="" options={llmConfigListOptions} />
                    </FormItem>
                    <FormItem
                      name={['chatAppConfig', item.value, 'prompt']}
                      label={
                        <>
                          <Space>
                            提示词模板
                            <Tooltip
                              overlayInnerStyle={{ width: 400 }}
                              title={
                                <>
                                  {tips.map((tip) => (
                                    <div>{tip}</div>
                                  ))}
                                </>
                              }
                            >
                              <QuestionCircleOutlined />
                            </Tooltip>
                          </Space>
                        </>
                      }
                    >
                      <Input.TextArea style={{ minHeight: 600 }} />
                    </FormItem>
                  </div>
                );
              })}
            </div>
          </Space>
        </div>
      ),
    },
    {
      label: '工具配置',
      key: 'tools',
      children: <ToolsSection currentAgent={editAgent} onSaveAgent={onSaveAgent} />,
    },
    {
      label: '记忆管理',
      key: 'memory',
      children: <MemorySection agentId={editAgent?.id} />,
    },
    {
      label: '权限管理',
      key: 'permissonSetting',
      children: <PermissionSection currentAgent={editAgent} onSaveAgent={onSaveAgent} />,
    },
  ];

  return (
    <Form
      {...layout}
      form={form}
      initialValues={formData}
      onValuesChange={(value, values) => {
        setFormData(values);
      }}
      className={globalStyles.supersonicForm}
    >
      <Tabs
        tabBarExtraContent={
          <Space>
            {activeKey !== 'memory' && activeKey !== 'permissonSetting' && (
              <Button
                type="primary"
                loading={saveLoading}
                onClick={() => {
                  onOk();
                }}
              >
                保 存
              </Button>
            )}
            {activeKey === 'tools' && (
              <Button
                type="primary"
                onClick={() => {
                  onCreateToolBtnClick?.();
                }}
              >
                <PlusOutlined /> 新增工具
              </Button>
            )}
          </Space>
        }
        defaultActiveKey="basic"
        activeKey={activeKey}
        onChange={(key) => {
          setActiveKey(key);
        }}
        items={formTabList}
      />
    </Form>
  );
};

export default AgentForm;
