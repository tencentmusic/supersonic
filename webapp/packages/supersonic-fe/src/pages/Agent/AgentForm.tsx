import { Form, Input, Button, Switch, Tabs, Select, message, Space, Tooltip } from 'antd';
import MainTitleMark from '@/components/MainTitleMark';
import { AgentType } from './type';
import { useEffect, useState } from 'react';
import styles from './style.less';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { uuid, jsonParse } from '@/utils/utils';
import ToolsSection from './ToolsSection';
import globalStyles from '@/global.less';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { getLlmModelTypeList, getLlmList } from '../../services/system';
import MemorySection from './MemorySection';

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
  const [modelTypeOptions, setModelTypeOptions] = useState<any[]>([]);
  const [llmConfigListOptions, setLlmConfigListOptions] = useState<any[]>([]);
  const [formData, setFormData] = useState<any>({
    enableSearch: true,
    modelConfig: {
      timeOut: 60,
      provider: 'OPEN_AI',
      temperature: 0,
    },
    agentConfig: {
      ...defaultAgentConfig,
    },
  });
  const [form] = Form.useForm();

  useEffect(() => {
    if (editAgent) {
      const sourceData = { ...editAgent };
      if (!sourceData.modelConfig) {
        delete sourceData.modelConfig;
      }

      const config = jsonParse(editAgent.toolConfig, {});
      const initData = {
        ...sourceData,
        enableSearch: editAgent.enableSearch !== 0,
        agentConfig: { ...defaultAgentConfig, ...config },
      };
      form.setFieldsValue(initData);
      setFormData(initData);
      if (editAgent.examples) {
        setExamples(editAgent.examples.map((question) => ({ id: uuid(), question })));
      }
    } else {
      form.resetFields();
    }
    queryModelTypeList();
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
  const queryModelTypeList = async () => {
    const { code, data } = await getLlmModelTypeList();
    if (code === 200 && data) {
      const options = data.map((item) => {
        return {
          label: item.name,
          value: item.type,
          description: item.description,
        };
      });
      setModelTypeOptions(options);
    } else {
      message.error('获取模型场景类型失败');
    }
  };

  const layout = {
    labelCol: { span: 4 },
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
      agentConfig: JSON.stringify({
        ...config,
        ...values.toolConfig,
        debugMode: values.toolConfig?.simpleMode === true ? false : values.toolConfig?.debugMode,
      }) as any,
      examples: examples.map((example) => example.question),
      enableSearch: values.enableSearch ? 1 : 0,
      enableMemoryReview: values.enableMemoryReview ? 1 : 0,
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
          <FormItem name="enableSearch" label="开启输入联想" valuePropName="checked">
            <Switch />
          </FormItem>
          <FormItem
            name={['multiTurnConfig', 'enableMultiTurn']}
            label="开启多轮对话"
            valuePropName="checked"
          >
            <Switch />
          </FormItem>
          <FormItem name="enableMemoryReview" label="开启记忆评估" valuePropName="checked">
            <Switch />
          </FormItem>
          <FormItem
            name={['agentConfig', 'simpleMode']}
            label="开启精简模式"
            tooltip="精简模式下不可调整查询条件、不显示调试信息、不显示可视化组件"
            valuePropName="checked"
          >
            <Switch />
          </FormItem>

          <FormItem
            name={['agentConfig', 'debugMode']}
            label="开启调试信息"
            hidden={formData?.toolConfig?.simpleMode === true}
            tooltip="包含Schema映射、SQL生成每阶段的关键信息"
            valuePropName="checked"
          >
            <Switch />
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
        <div className={styles.agentFormContainer}>
          <div className={styles.agentFormTitle}>
            <Space>
              应用场景 <MainTitleMark />
            </Space>
          </div>
          {modelTypeOptions.map((item) => {
            return (
              <FormItem name={['chatModelConfig', item.value]} label={item.label}>
                <Select placeholder="" options={llmConfigListOptions} />
              </FormItem>
            );
          })}
        </div>
      ),
    },
    {
      label: '提示词配置',
      key: 'promptConfig',
      children: (
        <div className={styles.agentFormContainer}>
          <FormItem
            name={['promptConfig', 'promptTemplate']}
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
            {activeKey !== 'memory' && (
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
