import {
  Form,
  Modal,
  Input,
  Button,
  Switch,
  Tabs,
  Slider,
  InputNumber,
  Select,
  Row,
  message,
  Space,
} from 'antd';
import MainTitleMark from '@/components/MainTitleMark';
import { AgentType } from './type';
import { useEffect, useState } from 'react';
import styles from './style.less';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { uuid, jsonParse, encryptPassword, decryptPassword } from '@/utils/utils';
import ToolsSection from './ToolsSection';
import globalStyles from '@/global.less';
import { testLLMConn } from './service';
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
  const [llmTestLoading, setLlmTestLoading] = useState<boolean>(false);
  const [formData, setFormData] = useState<any>({
    enableSearch: true,
    modelConfig: {
      chatModel: {
        timeOut: 60,
        provider: 'OPEN_AI',
        temperature: 0,
      },
      embeddingModel: {
        provider: 'OPEN_AI',
      },
    },
    // embeddingStore: {
    //   provider: 'MILVUS',
    //   timeOut: 60,
    // },
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

      const config = jsonParse(editAgent.agentConfig, {});
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
  }, [editAgent]);

  const layout = {
    labelCol: { span: 4 },
    wrapperCol: { span: 16 },
    // layout: 'vertical',
  };

  const onOk = async () => {
    const values = await form.validateFields();
    setSaveLoading(true);
    const config = jsonParse(editAgent?.agentConfig, {});
    await onSaveAgent?.({
      id: editAgent?.id,
      ...(editAgent || {}),
      ...values,
      agentConfig: JSON.stringify({
        ...config,
        ...values.agentConfig,
        debugMode: values.agentConfig?.simpleMode === true ? false : values.agentConfig?.debugMode,
      }) as any,
      examples: examples.map((example) => example.question),
      enableSearch: values.enableSearch ? 1 : 0,
    });
    setSaveLoading(false);
  };

  const testLLMConnect = async (params: any) => {
    setLlmTestLoading(true);
    const { code, data } = await testLLMConn(params);
    setLlmTestLoading(false);
    if (code === 200 && data) {
      message.success('连接成功');
    } else {
      message.error('模型连接失败');
    }
  };

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
          {/* <FormItem name={['visualConfig', 'defaultShowType']} label="问答默认格式">
            <Select
              placeholder=""
              options={[
                {
                  label: '文本',
                  value: 'TEXT',
                },
                {
                  label: '表格',
                  value: 'TABLE',
                },
                {
                  label: '图表',
                  value: 'WIDGET',
                },
              ]}
            />
          </FormItem> */}
          <FormItem name="enableSearch" label="支持联想" valuePropName="checked">
            <Switch />
          </FormItem>
          <FormItem
            name={['multiTurnConfig', 'enableMultiTurn']}
            label="开启多轮"
            valuePropName="checked"
          >
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
            label="显示调试信息"
            hidden={formData?.agentConfig?.simpleMode === true}
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
              对话模型 <MainTitleMark />
            </Space>
          </div>
          <FormItem name={['modelConfig', 'chatModel', 'provider']} label="接口协议">
            <Select placeholder="">
              {['OPEN_AI', 'OLLAMA'].map((item) => (
                <Select.Option key={item} value={item}>
                  {item}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
          <FormItem name={['modelConfig', 'chatModel', 'modelName']} label="Model Name">
            <Input placeholder="请输入语言模型名称" />
          </FormItem>
          <FormItem name={['modelConfig', 'chatModel', 'baseUrl']} label="Base URL">
            <Input placeholder="请输入Base URL" />
          </FormItem>
          <FormItem
            name={['modelConfig', 'chatModel', 'apiKey']}
            label="API Key"
            hidden={formData?.modelConfig?.chatModel?.provider === 'OLLAMA'}
            getValueFromEvent={(event) => {
              const value = event.target.value;
              return encryptPassword(value);
            }}
            getValueProps={(value) => {
              return {
                value: value ? decryptPassword(value) : '',
              };
            }}
          >
            <Input.Password placeholder="请输入API Key" visibilityToggle />
          </FormItem>

          <FormItem name={['modelConfig', 'chatModel', 'temperature']} label="Temperature">
            <Slider
              min={0}
              max={1}
              step={0.1}
              marks={{
                0: '精准',
                1: '随机',
              }}
            />
          </FormItem>
          <FormItem name={['modelConfig', 'chatModel', 'timeOut']} label="超时时间(秒)">
            <InputNumber />
          </FormItem>

          {/* <div className={styles.agentFormTitle}>
            <Space>
              向量模型 <MainTitleMark />
            </Space>
          </div>

          <FormItem name={['modelConfig', 'embeddingModel', 'provider']} label="接口协议">
            <Select placeholder="">
              {[
                'OPEN_AI',
                'OLLAMA',
                'LOCAL_AI',
                'IN_MEMORY',
                'ZHIPU',
                'AZURE',
                'QIANFAN',
                'DASHSCOPE',
              ].map((item) => (
                <Select.Option key={item} value={item}>
                  {item}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
          <FormItem name={['modelConfig', 'embeddingModel', 'modelName']} label="Model Name">
            <Input placeholder="请输入向量模型名称" />
          </FormItem>
          {formData?.modelConfig?.embeddingModel?.provider === 'IN_MEMORY' ? (
            <>
              <FormItem name={['modelConfig', 'embeddingModel', 'modelPath']} label="模型路径">
                <Input placeholder="请输入模型路径" />
              </FormItem>
              <FormItem
                name={['modelConfig', 'embeddingModel', 'vocabularyPath']}
                label="词汇表路径"
              >
                <Input placeholder="请输入模型路径" />
              </FormItem>
            </>
          ) : (
            <>
              <FormItem name={['modelConfig', 'embeddingModel', 'baseUrl']} label="Base URL">
                <Input placeholder="请输入Base URL" />
              </FormItem>
              <FormItem
                name={['modelConfig', 'embeddingModel', 'apiKey']}
                label="API Key"
                getValueFromEvent={(event) => {
                  const value = event.target.value;
                  return encryptPassword(value);
                }}
                getValueProps={(value) => {
                  return {
                    value: value ? decryptPassword(value) : '',
                  };
                }}
              >
                <Input.Password placeholder="请输入API Key" visibilityToggle />
              </FormItem>
            </>
          )} */}
        </div>
      ),
    },
    // {
    //   label: '向量库配置',
    //   key: 'embeddingStore',
    //   children: (
    //     <div className={styles.agentFormContainer}>
    //       <FormItem name={['embeddingStore', 'provider']} label="接口协议">
    //         <Select placeholder="">
    //           {['MILVUS', 'CHROMA', 'IN_MEMORY'].map((item) => (
    //             <Select.Option key={item} value={item}>
    //               {item}
    //             </Select.Option>
    //           ))}
    //         </Select>
    //       </FormItem>
    //       {formData?.embeddingStore?.provider === 'IN_MEMORY' ? (
    //         <>
    //           <FormItem name={['embeddingStore', 'persistPath']} label="持久化路径">
    //             <Input placeholder="请输入持久化路径" />
    //           </FormItem>
    //         </>
    //       ) : (
    //         <>
    //           <FormItem name={['embeddingStore', 'baseUrl']} label="Base URL">
    //             <Input placeholder="请输入Base URL" />
    //           </FormItem>
    //           <FormItem
    //             name={['embeddingStore', 'apiKey']}
    //             label="API Key"
    //             getValueFromEvent={(event) => {
    //               const value = event.target.value;
    //               return encryptPassword(value);
    //             }}
    //             getValueProps={(value) => {
    //               return {
    //                 value: value ? decryptPassword(value) : '',
    //               };
    //             }}
    //           >
    //             <Input.Password placeholder="请输入API Key" visibilityToggle />
    //           </FormItem>
    //           <FormItem name={['embeddingStore', 'timeOut']} label="超时时间(秒)">
    //             <InputNumber />
    //           </FormItem>
    //         </>
    //       )}
    //     </div>
    //   ),
    // },
    {
      label: '提示词配置',
      key: 'promptConfig',
      children: (
        <div className={styles.agentFormContainer}>
          <FormItem name={['promptConfig', 'promptTemplate']} label="提示词模板">
            <Input.TextArea
              style={{ minHeight: 600 }}
              placeholder=" &nbsp;自定义提示词模板可嵌入以下变量，将由系统自动进行替换：&#13;&#10;
                    -&nbsp;{{exemplar}} &nbsp;:替换成few-shot示例，示例个数由系统配置&#13;&#10;
                    -&nbsp;{{question}} &nbsp;:替换成用户问题，拼接了一定的补充信息&#13;&#10;
                    -&nbsp;{{schema}} &nbsp;:替换成数据语义信息，根据用户问题映射而来"
            />
          </FormItem>
        </div>
      ),
    },
    {
      label: '工具管理',
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
            {activeKey === 'modelConfig' && (
              <Button
                type="primary"
                loading={llmTestLoading}
                onClick={() => {
                  testLLMConnect(formData.modelConfig.chatModel);
                }}
              >
                大模型连接测试
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
