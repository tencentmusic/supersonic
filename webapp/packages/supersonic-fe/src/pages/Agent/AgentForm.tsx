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
  Col,
  Space,
} from 'antd';
import { AgentType } from './type';
import { useEffect, useState } from 'react';
import styles from './style.less';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { uuid, jsonParse } from '@/utils/utils';
import ToolsSection from './ToolsSection';
import globalStyles from '@/global.less';

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
  const [formData, setFormData] = useState<any>({
    enableSearch: true,
    llmConfig: {
      timeOut: 60,
      provider: 'OPEN_AI',
    },
    agentConfig: {
      ...defaultAgentConfig,
    },
  });
  const [form] = Form.useForm();

  useEffect(() => {
    if (editAgent) {
      const sourceData = { ...editAgent };
      if (!sourceData.llmConfig) {
        delete sourceData.llmConfig;
      }

      const config = jsonParse(editAgent.agentConfig, {});

      form.setFieldsValue({
        ...sourceData,
        enableSearch: editAgent.enableSearch !== 0,
        agentConfig: { ...defaultAgentConfig, ...config },
      });
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
      agentConfig: JSON.stringify({ ...config, ...values.agentConfig }) as any,
      examples: examples.map((example) => example.question),
      enableSearch: values.enableSearch ? 1 : 0,
    });
    setSaveLoading(false);
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
      key: 'llmConfig',
      children: (
        <div className={styles.agentFormContainer}>
          <FormItem name={['llmConfig', 'provider']} label="接口协议">
            <Select placeholder="">
              {['OPEN_AI'].map((item) => (
                <Select.Option key={item} value={item}>
                  {item}
                </Select.Option>
              ))}
            </Select>
          </FormItem>
          <FormItem name={['llmConfig', 'modelName']} label="Model Name">
            <Input placeholder="请输入大模型名称" />
          </FormItem>
          <FormItem name={['llmConfig', 'baseUrl']} label="Base URL">
            <Input placeholder="请输入Base URL" />
          </FormItem>
          <FormItem
            name={['llmConfig', 'apiKey']}
            label="API Key"
            // hidden={formData?.llmConfig?.provider === 'LOCAL_AI'}
          >
            <Input placeholder="请输入API Key" />
          </FormItem>

          <FormItem name={['llmConfig', 'temperature']} label="Temperature">
            <Slider
              min={0}
              max={1}
              step={0.1}
              marks={{
                0: '精准',
                1: '随机',
              }}
              defaultValue={0}
            />
          </FormItem>
          <FormItem name={['llmConfig', 'timeOut']} label="超时时间(秒)">
            <InputNumber />
          </FormItem>
        </div>
      ),
    },
    {
      label: '工具管理',
      key: 'tools',
      children: <ToolsSection currentAgent={editAgent} onSaveAgent={onSaveAgent} />,
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
            <Button
              type="primary"
              loading={saveLoading}
              onClick={() => {
                onOk();
              }}
            >
              保 存
            </Button>
            {activeKey === 'tools' && (
              <Button
                type="primary"
                onClick={() => {
                  // setEditTool(undefined);
                  // setModalVisible(true);
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
