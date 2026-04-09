import React, { useState, useEffect } from 'react';
import { Drawer, Steps, Button, Space, Form, Select, Input, message, Spin, Tabs, Table, Tag, Typography, Alert } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import { ISemantic } from '../../data';
import { getDatabaseList } from '../../service';
import {
  listConnections,
  createConnection,
  updateConnection,
  getJobHistory,
  type ConnectionDO,
  type DataSyncExecutionDO,
  type DiscoveredSchema,
} from '@/services/connection';
import { discoverSchemaByDatabase, type TableSyncConfig } from '@/services/dataSync';
import CronInput from '../../../ReportSchedule/components/CronInput';
import StreamConfigStep from './StreamConfigStep';
import { MSG } from '@/common/messages';

const { Text } = Typography;

const STATUS_MAP: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  RUNNING: { color: 'blue', text: '执行中' },
  SUCCESS: { color: 'green', text: '成功' },
  FAILED: { color: 'red', text: '失败' },
};

const SCHEMA_CHANGE_STATUS: Record<string, { color: string; text: string }> = {
  NO_CHANGE: { color: 'default', text: '无变更' },
  NON_BREAKING: { color: 'blue', text: '有新增' },
  BREAKING: { color: 'red', text: '破坏性变更' },
};

interface SyncConfigWizardProps {
  visible: boolean;
  sourceDatabase: ISemantic.IDatabaseItem;
  onClose: () => void;
  onSuccess: () => void;
}

const SyncConfigWizard: React.FC<SyncConfigWizardProps> = ({
  visible,
  sourceDatabase,
  onClose,
  onSuccess,
}) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [form] = Form.useForm();
  const [allDatabases, setAllDatabases] = useState<ISemantic.IDatabaseItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [schema, setSchema] = useState<DiscoveredSchema | null>(null);
  const [tableConfigs, setTableConfigs] = useState<TableSyncConfig[]>([]);
  const [existingConnection, setExistingConnection] = useState<ConnectionDO | null>(null);
  const [activeTab, setActiveTab] = useState<string>('config');
  const [executions, setExecutions] = useState<DataSyncExecutionDO[]>([]);
  const [executionLoading, setExecutionLoading] = useState(false);
  const [executionPagination, setExecutionPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  useEffect(() => {
    if (visible) {
      loadDatabases();
      loadExistingConnection();
      setCurrentStep(0);
      setSchema(null);
      setTableConfigs([]);
      setActiveTab('config');
    }
  }, [visible, sourceDatabase?.id]);

  const loadDatabases = async () => {
    const { code, data } = await getDatabaseList();
    if (code === 200) {
      setAllDatabases(data || []);
    }
  };

  const loadExistingConnection = async () => {
    try {
      const res = await listConnections({ current: 1, pageSize: 100, sourceDbId: sourceDatabase.id });
      const connections: ConnectionDO[] = res?.records || [];
      const found = connections.find((c) => c.sourceDatabaseId === sourceDatabase.id);
      if (found) {
        setExistingConnection(found);
        form.setFieldsValue({
          destinationDatabaseId: found.destinationDatabaseId,
          cronExpression: found.cronExpression,
          retryCount: found.retryCount ?? 3,
        });
        // Parse existing configured catalog
        if (found.configuredCatalog) {
          try {
            const parsed = JSON.parse(found.configuredCatalog);
            if (parsed.streams) {
              setTableConfigs(
                parsed.streams.map((s: any) => ({
                  sourceTable: s.streamName,
                  targetTable: s.destinationTable || s.streamName,
                  syncMode: s.syncMode || 'FULL',
                  cursorField: s.cursorField,
                  primaryKey: s.primaryKey?.join(','),
                  batchSize: s.batchSize || 5000,
                })),
              );
            } else if (parsed.tables) {
              // Legacy format support
              setTableConfigs(
                parsed.tables.map((t: any) => ({
                  sourceTable: t.source_table,
                  targetTable: t.target_table || t.source_table,
                  syncMode: t.sync_mode || 'FULL',
                  cursorField: t.cursor_field || t.watermark_column,
                  primaryKey: t.primary_key,
                  batchSize: t.batch_size || 5000,
                })),
              );
            }
          } catch (e) {
            // ignore parse error
          }
        }
      }
    } catch (e) {
      // non-blocking
    }
  };

  const loadExecutions = async (current = 1, pageSize = 10) => {
    if (!existingConnection?.id) return;
    setExecutionLoading(true);
    try {
      const res = await getJobHistory(existingConnection.id, { current, pageSize });
      setExecutions(res?.records || []);
      setExecutionPagination({ current, pageSize, total: res?.total || 0 });
    } finally {
      setExecutionLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'history' && existingConnection?.id) {
      loadExecutions();
    }
  }, [activeTab, existingConnection?.id]);

  const handleNext = async () => {
    if (currentStep === 0) {
      await form.validateFields(['destinationDatabaseId']);
      // Discover schema
      setLoading(true);
      try {
        const result = await discoverSchemaByDatabase(sourceDatabase.id);
        setSchema(result);
        // Initialize table configs if not already set
        if (tableConfigs.length === 0 && result?.tables) {
          setTableConfigs(
            result.tables.map((t: any) => ({
              sourceTable: t.tableName,
              targetTable: t.tableName,
              syncMode: 'FULL' as const,
            })),
          );
        }
        setCurrentStep(1);
      } catch (e: any) {
        message.error(e?.message || 'Schema 发现失败');
      } finally {
        setLoading(false);
      }
    } else if (currentStep === 1) {
      // Validate at least one table is selected
      const selectedTables = tableConfigs.filter((t) => t.sourceTable);
      if (selectedTables.length === 0) {
        message.warning('请至少选择一张表进行同步');
        return;
      }
      setCurrentStep(2);
    }
  };

  const handlePrev = () => {
    setCurrentStep(currentStep - 1);
  };

  const handleSave = async () => {
    await form.validateFields();
    const values = form.getFieldsValue();

    const selectedTables = tableConfigs.filter((t) => t.sourceTable);
    // Use Airbyte-style configuredCatalog format
    const configuredCatalog = JSON.stringify({
      streams: selectedTables.map((t) => ({
        streamName: t.sourceTable,
        destinationTable: t.targetTable || t.sourceTable,
        syncMode: t.syncMode || 'FULL',
        cursorField: t.cursorField,
        primaryKey: t.primaryKey ? t.primaryKey.split(',').map((k) => k.trim()) : undefined,
        batchSize: t.batchSize || 5000,
        selected: true,
      })),
    });

    const connectionData: Partial<ConnectionDO> = {
      name: `${sourceDatabase.name} → 同步`,
      sourceDatabaseId: sourceDatabase.id,
      destinationDatabaseId: values.destinationDatabaseId,
      scheduleType: 'CRON',
      cronExpression: values.cronExpression,
      retryCount: values.retryCount ?? 3,
      configuredCatalog,
    };

    setLoading(true);
    try {
      if (existingConnection?.id) {
        await updateConnection(existingConnection.id, connectionData);
        message.success('同步配置已更新');
      } else {
        await createConnection(connectionData as ConnectionDO);
        message.success('同步配置已创建');
      }
      onSuccess();
      onClose();
    } catch (e: any) {
      message.error(e?.message || MSG.SAVE_FAILED);
    } finally {
      setLoading(false);
    }
  };

  const targetDatabaseOptions = allDatabases
    .filter((db) => db.id !== sourceDatabase?.id)
    .map((db) => ({
      value: db.id,
      label: `${db.name} (${db.type})`,
    }));

  const steps = [
    { title: '选择目标', description: '选择目标数据源' },
    { title: '配置表', description: 'Schema 发现与配置' },
    { title: '调度设置', description: '定时与高级配置' },
  ];

  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return (
          <Form form={form} layout="vertical" style={{ marginTop: 24 }}>
            {existingConnection?.schemaChangeStatus === 'BREAKING' && (
              <Alert
                message="检测到 Schema 破坏性变更"
                description="源数据库结构发生了变化，可能导致同步失败。请检查表配置。"
                type="error"
                showIcon
                icon={<WarningOutlined />}
                style={{ marginBottom: 16 }}
              />
            )}
            <Form.Item label="源数据源">
              <Input value={`${sourceDatabase?.name} (${sourceDatabase?.type})`} disabled />
            </Form.Item>
            <Form.Item
              name="destinationDatabaseId"
              label="目标数据源"
              rules={[{ required: true, message: '请选择目标数据源' }]}
            >
              <Select
                placeholder="选择目标数据源（分析库）"
                options={targetDatabaseOptions}
                showSearch
                optionFilterProp="label"
              />
            </Form.Item>
          </Form>
        );
      case 1:
        return (
          <StreamConfigStep
            schema={schema}
            tableConfigs={tableConfigs}
            onChange={setTableConfigs}
          />
        );
      case 2:
        return (
          <Form form={form} layout="vertical" style={{ marginTop: 24 }}>
            <Form.Item
              name="cronExpression"
              label="调度频率"
              rules={[{ required: true, message: '请设置调度频率' }]}
            >
              <CronInput />
            </Form.Item>
            <Form.Item name="retryCount" label="重试次数" initialValue={3}>
              <Select
                options={[
                  { value: 0, label: '不重试' },
                  { value: 1, label: '1 次' },
                  { value: 2, label: '2 次' },
                  { value: 3, label: '3 次' },
                ]}
              />
            </Form.Item>
          </Form>
        );
      default:
        return null;
    }
  };

  const renderFooter = () => {
    return (
      <Space>
        {currentStep > 0 && (
          <Button onClick={handlePrev} disabled={loading}>
            上一步
          </Button>
        )}
        {currentStep < 2 && (
          <Button type="primary" onClick={handleNext} loading={loading}>
            下一步
          </Button>
        )}
        {currentStep === 2 && (
          <Button type="primary" onClick={handleSave} loading={loading}>
            保存配置
          </Button>
        )}
      </Space>
    );
  };

  const executionColumns = [
    {
      title: '开始时间',
      dataIndex: 'startTime',
      width: 160,
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      width: 160,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (status: string) => {
        const info = STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '读取行数',
      dataIndex: 'rowsRead',
      width: 90,
      render: (val: number) => val ?? '-',
    },
    {
      title: '写入行数',
      dataIndex: 'rowsWritten',
      width: 90,
      render: (val: number) => val ?? '-',
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      ellipsis: true,
      render: (val: string) => val ? <Text type="danger" ellipsis={{ tooltip: val }}>{val}</Text> : '-',
    },
  ];

  const renderContent = () => {
    if (existingConnection) {
      const schemaStatus = existingConnection.schemaChangeStatus;
      const schemaInfo = SCHEMA_CHANGE_STATUS[schemaStatus || 'NO_CHANGE'];

      return (
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'config',
              label: (
                <Space>
                  同步配置
                  {schemaStatus && schemaStatus !== 'NO_CHANGE' && (
                    <Tag color={schemaInfo.color}>{schemaInfo.text}</Tag>
                  )}
                </Space>
              ),
              children: (
                <>
                  <Steps current={currentStep} items={steps} style={{ marginBottom: 24 }} />
                  <Spin spinning={loading}>{renderStepContent()}</Spin>
                  <div style={{ marginTop: 24, textAlign: 'right' }}>{renderFooter()}</div>
                </>
              ),
            },
            {
              key: 'history',
              label: '执行记录',
              children: (
                <Table
                  rowKey="id"
                  columns={executionColumns}
                  dataSource={executions}
                  loading={executionLoading}
                  pagination={{
                    ...executionPagination,
                    onChange: (page, size) => loadExecutions(page, size),
                  }}
                  size="small"
                />
              ),
            },
          ]}
        />
      );
    }

    return (
      <>
        <Steps current={currentStep} items={steps} style={{ marginBottom: 24 }} />
        <Spin spinning={loading}>{renderStepContent()}</Spin>
        <div style={{ marginTop: 24, textAlign: 'right' }}>{renderFooter()}</div>
      </>
    );
  };

  return (
    <Drawer
      title={`配置数据同步 - ${sourceDatabase?.name || ''}`}
      open={visible}
      onClose={onClose}
      width={800}
      destroyOnClose
    >
      {renderContent()}
    </Drawer>
  );
};

export default SyncConfigWizard;
