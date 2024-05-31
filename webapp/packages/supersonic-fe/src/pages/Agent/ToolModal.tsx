import { Form, Modal, Input, Select, Button, TreeSelect, message } from 'antd';
import {
  AgentToolType,
  AgentToolTypeEnum,
  AGENT_TOOL_TYPE_LIST,
  MetricOptionType,
  QUERY_MODE_LIST,
} from './type';
import { useEffect, useState } from 'react';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import styles from './style.less';
import { traverseTree, uuid } from '@/utils/utils';
import { getModelList } from './service';
import { PluginType } from '../ChatPlugin/type';
import { getPluginList } from '../ChatPlugin/service';

const FormItem = Form.Item;

type Props = {
  editTool?: AgentToolType;
  onSaveTool: (tool: AgentToolType) => Promise<void>;
  onCancel: () => void;
};

const ToolModal: React.FC<Props> = ({ editTool, onSaveTool, onCancel }) => {
  const [toolType, setToolType] = useState<AgentToolTypeEnum>();
  const [modelList, setModelList] = useState<any[]>([]);
  const [saveLoading, setSaveLoading] = useState(false);
  const [examples, setExamples] = useState<{ id: string; question?: string }[]>([]);
  const [metricOptions, setMetricOptions] = useState<MetricOptionType[]>([]);
  const [plugins, setPlugins] = useState<PluginType[]>([]);
  const [form] = Form.useForm();

  // const filterTree = (treeData: any[]) => {
  //   treeData.forEach((node) => {
  //     if (Array.isArray(node.children) && node.children?.length > 0) {
  //       node.children = node.children.filter((item: any) => item.type !== 'DOMAIN');
  //       filterTree(node.children);
  //     }
  //   });
  //   return treeData;
  // };

  const initModelList = async () => {
    const res = await getModelList();
    const treeData = traverseTree(res.data, (node: any) => {
      node.title = node.name;
      node.value = node.type === 'DOMAIN' ? `DOMAIN_${node.id}` : node.id;
      node.checkable =
        node.type === 'DATASET' || (node.type === 'DOMAIN' && node.children?.length > 0);
    });
    setModelList([{ title: '默认', value: -1, type: 'DATASET' }, ...treeData]);
  };

  const initPluginList = async () => {
    const res = await getPluginList({});
    setPlugins(res.data || []);
  };

  useEffect(() => {
    initModelList();
    initPluginList();
  }, []);

  useEffect(() => {
    if (editTool) {
      form.setFieldsValue({ ...editTool, plugins: editTool.plugins?.[0] });
      setToolType(editTool.type);
      setExamples(
        (editTool.exampleQuestions || []).map((item) => ({ id: uuid(), question: item })),
      );
      setMetricOptions(editTool.metricOptions || []);
    } else {
      form.resetFields();
    }
  }, [editTool]);

  const layout = {
    labelCol: { span: 6 },
    wrapperCol: { span: 14 },
  };

  const onOk = async () => {
    const values = await form.validateFields();
    setSaveLoading(true);
    await onSaveTool({
      id: editTool?.id,
      ...values,
      exampleQuestions: examples.map((item) => item.question).filter((item) => item),
      plugins: values.plugins ? [values.plugins] : undefined,
      metricOptions: metricOptions.map((item) => ({ ...item, modelId: values.modelId })),
    });
    setSaveLoading(false);
  };

  return (
    <Modal
      open
      title={editTool ? '编辑工具' : '新建工具'}
      confirmLoading={saveLoading}
      width={800}
      onOk={onOk}
      onCancel={onCancel}
    >
      <Form {...layout} form={form}>
        <FormItem name="type" label="类型" rules={[{ required: true, message: '请选择工具类型' }]}>
          <Select
            options={AGENT_TOOL_TYPE_LIST}
            placeholder="请选择工具类型"
            onChange={setToolType}
          />
        </FormItem>
        <FormItem name="name" label="名称">
          <Input placeholder="请输入工具名称" />
        </FormItem>
        {(toolType === AgentToolTypeEnum.NL2SQL_RULE ||
          toolType === AgentToolTypeEnum.NL2SQL_LLM) && (
          <FormItem name="dataSetIds" label="数据集">
            <TreeSelect
              treeData={modelList}
              placeholder="请选择数据集"
              multiple
              treeCheckable
              allowClear
            />
          </FormItem>
        )}
        {toolType === AgentToolTypeEnum.NL2SQL_LLM && (
          <FormItem name="exampleQuestions" label="示例问题">
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
        )}
        {toolType === AgentToolTypeEnum.PLUGIN && (
          <FormItem name="plugins" label="插件">
            <Select
              placeholder="请选择插件"
              options={plugins.map((plugin) => ({ label: plugin.name, value: plugin.id }))}
              showSearch
              filterOption={(input, option) =>
                ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
              }
              onChange={(value) => {
                const plugin = plugins.find((item) => item.id === value);
                if (plugin) {
                  form.setFieldsValue({ name: plugin.name });
                }
              }}
            />
          </FormItem>
        )}
        {toolType === AgentToolTypeEnum.NL2SQL_RULE && (
          <FormItem name="queryTypes" label="查询模式">
            <Select
              placeholder="请选择查询模式"
              options={QUERY_MODE_LIST}
              showSearch
              mode="multiple"
              filterOption={(input, option) =>
                ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
              }
            />
          </FormItem>
        )}
      </Form>
    </Modal>
  );
};

export default ToolModal;
