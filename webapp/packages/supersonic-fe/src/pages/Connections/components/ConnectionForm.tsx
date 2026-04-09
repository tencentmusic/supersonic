import React, { useEffect, useState } from 'react';
import {
  Drawer,
  Form,
  Input,
  Select,
  InputNumber,
  Steps,
  Button,
  Space,
  message,
  Spin,
  Alert,
} from 'antd';
import CronInput from '@/pages/ReportSchedule/components/CronInput';
import { getDatabaseList } from '@/pages/SemanticModel/service';
import {
  ConnectionDO,
  createConnection,
  updateConnection,
  discoverSchema,
  DiscoveredSchema,
} from '@/services/connection';
import StreamConfigStep from '@/pages/SemanticModel/components/Database/StreamConfigStep';
import { MSG } from '@/common/messages';

interface ConnectionFormProps {
  visible: boolean;
  record?: ConnectionDO;
  onCancel: () => void;
  onSuccess: () => void;
}

const SCHEDULE_TYPES = [
  { label: '手动触发', value: 'MANUAL' },
  { label: '按计划执行', value: 'CRON' },
];

const ConnectionForm: React.FC<ConnectionFormProps> = ({
  visible,
  record,
  onCancel,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [databases, setDatabases] = useState<any[]>([]);
  const [loadingDatabases, setLoadingDatabases] = useState(false);
  const [schema, setSchema] = useState<DiscoveredSchema | null>(null);
  const [discoveringSchema, setDiscoveringSchema] = useState(false);
  const [streamConfig, setStreamConfig] = useState<any[]>([]);
  const isEdit = !!record?.id;

  // Fetch database list
  useEffect(() => {
    if (visible) {
      fetchDatabases();
    }
  }, [visible]);

  const fetchDatabases = async () => {
    setLoadingDatabases(true);
    try {
      const { code, data } = await getDatabaseList();
      if (code === 200) {
        setDatabases(data || []);
      }
    } finally {
      setLoadingDatabases(false);
    }
  };

  // Initialize form when visible
  useEffect(() => {
    if (visible) {
      setCurrentStep(0);
      if (record) {
        form.setFieldsValue({
          name: record.name,
          description: record.description,
          sourceDatabaseId: record.sourceDatabaseId,
          destinationDatabaseId: record.destinationDatabaseId,
          scheduleType: record.scheduleType || 'MANUAL',
          cronExpression: record.cronExpression,
          retryCount: record.retryCount || 3,
        });
        // Parse configured catalog
        if (record.configuredCatalog) {
          try {
            const catalog = JSON.parse(record.configuredCatalog);
            setStreamConfig(catalog.streams || []);
          } catch {
            setStreamConfig([]);
          }
        }
        // Parse discovered catalog
        if (record.discoveredCatalog) {
          try {
            setSchema(JSON.parse(record.discoveredCatalog));
          } catch {
            setSchema(null);
          }
        }
      } else {
        form.resetFields();
        form.setFieldsValue({
          scheduleType: 'MANUAL',
          retryCount: 3,
        });
        setStreamConfig([]);
        setSchema(null);
      }
    }
  }, [visible, record, form]);

  const handleDiscoverSchema = async () => {
    const sourceDatabaseId = form.getFieldValue('sourceDatabaseId');
    if (!sourceDatabaseId) {
      message.error('请先选择源数据库');
      return;
    }

    // For new connections, we need to create first or use database ID directly
    // For simplicity, we use a temporary approach with existing connection
    if (record?.id) {
      setDiscoveringSchema(true);
      try {
        const discoveredSchema = await discoverSchema(record.id);
        setSchema(discoveredSchema);
        // Initialize stream config from discovered schema
        const defaultStreams = (discoveredSchema.tables || []).map((table) => ({
          streamName: table.tableName,
          selected: true,
          syncMode: 'FULL',
        }));
        setStreamConfig(defaultStreams);
        message.success(`发现 ${discoveredSchema.tables?.length || 0} 个表`);
      } catch (e: any) {
        message.error(e.message || 'Schema 发现失败');
      } finally {
        setDiscoveringSchema(false);
      }
    } else {
      message.info('请先保存连接后再发现 Schema');
    }
  };

  const handleNext = async () => {
    if (currentStep === 0) {
      await form.validateFields(['name', 'sourceDatabaseId', 'destinationDatabaseId']);
    }
    setCurrentStep(currentStep + 1);
  };

  const handlePrev = () => {
    setCurrentStep(currentStep - 1);
  };

  const handleSubmit = async () => {
    setLoading(true);
    try {
      const values = await form.validateFields();

      const connectionData: ConnectionDO = {
        name: values.name,
        description: values.description,
        sourceDatabaseId: values.sourceDatabaseId,
        destinationDatabaseId: values.destinationDatabaseId,
        scheduleType: values.scheduleType,
        cronExpression: values.scheduleType === 'CRON' ? values.cronExpression : undefined,
        retryCount: values.retryCount,
        configuredCatalog: JSON.stringify({ streams: streamConfig }),
        status: 'ACTIVE',
      };

      if (isEdit && record?.id) {
        await updateConnection(record.id, connectionData);
        message.success(MSG.UPDATE_SUCCESS);
      } else {
        await createConnection(connectionData);
        message.success(MSG.CREATE_SUCCESS);
      }

      onSuccess();
    } catch (e: any) {
      if (e.errorFields) {
        message.error('请检查表单填写是否正确');
      } else {
        message.error(e.message || MSG.OPERATION_FAILED);
      }
    } finally {
      setLoading(false);
    }
  };

  const scheduleType = Form.useWatch('scheduleType', form);

  const steps = [
    {
      title: '基本信息',
      content: (
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="连接名称"
            rules={[{ required: true, message: '请输入连接名称' }]}
          >
            <Input placeholder="如: 用户数据同步" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="连接描述 (可选)" />
          </Form.Item>
          <Form.Item
            name="sourceDatabaseId"
            label="源数据库"
            rules={[{ required: true, message: '请选择源数据库' }]}
          >
            <Select
              placeholder="选择源数据库"
              loading={loadingDatabases}
              showSearch
              optionFilterProp="label"
              options={databases.map((db) => ({
                label: `${db.name} (${db.type})`,
                value: db.id,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="destinationDatabaseId"
            label="目标数据库"
            rules={[{ required: true, message: '请选择目标数据库' }]}
          >
            <Select
              placeholder="选择目标数据库"
              loading={loadingDatabases}
              showSearch
              optionFilterProp="label"
              options={databases.map((db) => ({
                label: `${db.name} (${db.type})`,
                value: db.id,
              }))}
            />
          </Form.Item>
        </Form>
      ),
    },
    {
      title: '数据流配置',
      content: (
        <div>
          {!schema && !isEdit && (
            <Alert
              message="提示"
              description="请先保存基本信息后再配置数据流。或者您可以跳过此步骤，稍后在连接详情中配置。"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}
          {isEdit && (
            <div style={{ marginBottom: 16 }}>
              <Button
                type="primary"
                ghost
                onClick={handleDiscoverSchema}
                loading={discoveringSchema}
              >
                发现 Schema
              </Button>
            </div>
          )}
          {schema && schema.tables && schema.tables.length > 0 && (
            <StreamConfigStep
              schema={schema}
              tableConfigs={streamConfig}
              onChange={setStreamConfig}
            />
          )}
          {!schema && isEdit && (
            <Alert message="暂无 Schema 信息，请点击上方按钮发现 Schema" type="warning" />
          )}
        </div>
      ),
    },
    {
      title: '调度配置',
      content: (
        <Form form={form} layout="vertical">
          <Form.Item name="scheduleType" label="调度方式">
            <Select options={SCHEDULE_TYPES} />
          </Form.Item>
          {scheduleType === 'CRON' && (
            <Form.Item
              name="cronExpression"
              label="Cron 表达式"
              rules={[{ required: true, message: '请设置 Cron 表达式' }]}
            >
              <CronInput />
            </Form.Item>
          )}
          <Form.Item name="retryCount" label="失败重试次数">
            <InputNumber min={0} max={10} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <Drawer
      title={isEdit ? '编辑连接' : '创建连接'}
      open={visible}
      onClose={onCancel}
      width={700}
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <div>
            {currentStep > 0 && (
              <Button onClick={handlePrev} disabled={loading}>
                上一步
              </Button>
            )}
          </div>
          <Space>
            <Button onClick={onCancel}>取消</Button>
            {currentStep < steps.length - 1 ? (
              <Button type="primary" onClick={handleNext}>
                下一步
              </Button>
            ) : (
              <Button type="primary" onClick={handleSubmit} loading={loading}>
                {isEdit ? '更新' : '创建'}
              </Button>
            )}
          </Space>
        </div>
      }
    >
      <Spin spinning={loading}>
        <Steps current={currentStep} style={{ marginBottom: 24 }}>
          {steps.map((step) => (
            <Steps.Step key={step.title} title={step.title} />
          ))}
        </Steps>
        {steps[currentStep].content}
      </Spin>
    </Drawer>
  );
};

export default ConnectionForm;
