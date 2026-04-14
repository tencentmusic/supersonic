import React, { useRef, useState } from 'react';
import {
  Alert,
  Button,
  Modal,
  Table,
  Tag,
  Space,
  Typography,
  Collapse,
  Descriptions,
  message,
  Spin,
} from 'antd';
import {
  WarningOutlined,
  PlusOutlined,
  MinusOutlined,
  SwapOutlined,
  ExclamationCircleOutlined,
  CheckOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import {
  SchemaChange,
  StreamChange,
  getSchemaChanges,
  discoverSchema,
  applySchemaChanges,
  ConnectionDO,
} from '@/services/connection';

interface SchemaChangeAlertProps {
  connection: ConnectionDO;
  onRefresh: () => void;
}

const CHANGE_TYPE_CONFIG: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
  ADDED: { color: 'green', icon: <PlusOutlined />, text: '新增' },
  REMOVED: { color: 'red', icon: <MinusOutlined />, text: '删除' },
  TYPE_CHANGED: { color: 'orange', icon: <SwapOutlined />, text: '类型变更' },
  NULLABLE_CHANGED: { color: 'blue', icon: <SwapOutlined />, text: '可空性变更' },
  PRIMARY_KEY_CHANGED: { color: 'purple', icon: <ExclamationCircleOutlined />, text: '主键变更' },
};

const SchemaChangeAlert: React.FC<SchemaChangeAlertProps> = ({ connection, onRefresh }) => {
  const [detailVisible, setDetailVisible] = useState(false);
  const [schemaChange, setSchemaChange] = useState<SchemaChange | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [applying, setApplying] = useState(false);
  const refreshingRef = useRef(false);
  const applyingRef = useRef(false);

  const status = connection.schemaChangeStatus;
  const isBreaking = status === 'BREAKING';
  const hasChanges = status === 'BREAKING' || status === 'NON_BREAKING';

  if (!hasChanges) {
    return null;
  }

  const handleViewDetails = async () => {
    setLoading(true);
    setDetailVisible(true);
    try {
      const changes = await getSchemaChanges(connection.id!);
      setSchemaChange(changes);
    } catch (e: any) {
      message.error(e.message || '获取 Schema 变更详情失败');
    } finally {
      setLoading(false);
    }
  };

  const handleRefreshSchema = async () => {
    if (refreshingRef.current) {
      return;
    }
    refreshingRef.current = true;
    setRefreshing(true);
    try {
      await discoverSchema(connection.id!);
      const changes = await getSchemaChanges(connection.id!);
      setSchemaChange(changes);
      message.success('Schema 已刷新');
      onRefresh();
    } catch (e: any) {
      message.error(e.message || 'Schema 刷新失败');
    } finally {
      refreshingRef.current = false;
      setRefreshing(false);
    }
  };

  const handleApplyChanges = async () => {
    if (applyingRef.current) {
      return;
    }
    if (!connection.discoveredCatalog) {
      message.error('请先刷新 Schema');
      return;
    }

    applyingRef.current = true;
    Modal.confirm({
      title: '确认应用 Schema 变更',
      content: isBreaking
        ? '检测到破坏性变更，应用后可能导致数据同步失败。确定要继续吗？'
        : '确定要将新发现的 Schema 应用到当前配置吗？',
      icon: <ExclamationCircleOutlined />,
      okType: isBreaking ? 'danger' : 'primary',
      onCancel: () => {
        applyingRef.current = false;
      },
      onOk: async () => {
        setApplying(true);
        try {
          // Parse discovered catalog and convert to ConfiguredCatalog format
          const discovered = JSON.parse(connection.discoveredCatalog!);
          const streams =
            discovered.tables?.map((table: any) => ({
              streamName: table.tableName,
              selected: true,
              syncMode: 'FULL',
            })) || [];

          await applySchemaChanges(connection.id!, { streams });
          message.success('Schema 变更已应用');
          setDetailVisible(false);
          onRefresh();
        } catch (e: any) {
          message.error(e.message || '应用 Schema 变更失败');
        } finally {
          applyingRef.current = false;
          setApplying(false);
        }
      },
    });
  };

  const renderChangeTag = (changeType: string) => {
    const config = CHANGE_TYPE_CONFIG[changeType] || CHANGE_TYPE_CONFIG.TYPE_CHANGED;
    return (
      <Tag color={config.color} icon={config.icon}>
        {config.text}
      </Tag>
    );
  };

  const renderStreamChanges = () => {
    if (!schemaChange || !schemaChange.changes || schemaChange.changes.length === 0) {
      return <Typography.Text type="secondary">无变更</Typography.Text>;
    }

    const tableChanges = schemaChange.changes.filter(
      (c) => c.changeType === 'ADDED' || c.changeType === 'REMOVED',
    );
    const columnChanges = schemaChange.changes.filter(
      (c) => c.changeType !== 'ADDED' && c.changeType !== 'REMOVED',
    );

    return (
      <div>
        {tableChanges.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <Typography.Title level={5}>表级变更</Typography.Title>
            <Table
              dataSource={tableChanges}
              rowKey="streamName"
              size="small"
              pagination={false}
              columns={[
                {
                  title: '表名',
                  dataIndex: 'streamName',
                  width: 300,
                },
                {
                  title: '变更类型',
                  dataIndex: 'changeType',
                  width: 120,
                  render: (type: string) => renderChangeTag(type),
                },
                {
                  title: '影响',
                  render: (_: any, record: StreamChange) =>
                    record.changeType === 'REMOVED' ? (
                      <Typography.Text type="danger">破坏性变更</Typography.Text>
                    ) : (
                      <Typography.Text type="success">非破坏性</Typography.Text>
                    ),
                },
              ]}
            />
          </div>
        )}

        {columnChanges.length > 0 && (
          <div>
            <Typography.Title level={5}>列级变更</Typography.Title>
            <Collapse>
              {columnChanges.map((stream) => (
                <Collapse.Panel
                  key={stream.streamName}
                  header={
                    <Space>
                      <span>{stream.streamName}</span>
                      <Tag>{stream.columnChanges?.length || 0} 列变更</Tag>
                    </Space>
                  }
                >
                  {stream.columnChanges && stream.columnChanges.length > 0 ? (
                    <Table
                      dataSource={stream.columnChanges}
                      rowKey="columnName"
                      size="small"
                      pagination={false}
                      columns={[
                        {
                          title: '列名',
                          dataIndex: 'columnName',
                          width: 200,
                        },
                        {
                          title: '变更类型',
                          dataIndex: 'changeType',
                          width: 120,
                          render: (type: string) => renderChangeTag(type),
                        },
                        {
                          title: '原类型',
                          dataIndex: 'previousType',
                          width: 150,
                          render: (val: string) => val || '-',
                        },
                        {
                          title: '新类型',
                          dataIndex: 'currentType',
                          width: 150,
                          render: (val: string) => val || '-',
                        },
                      ]}
                    />
                  ) : (
                    <Typography.Text type="secondary">无列变更</Typography.Text>
                  )}
                </Collapse.Panel>
              ))}
            </Collapse>
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      <Alert
        message={isBreaking ? 'Schema 破坏性变更警告' : 'Schema 变更提示'}
        description={
          isBreaking
            ? '检测到破坏性 Schema 变更（表删除、列删除或类型变更），可能导致同步失败。请及时处理。'
            : '检测到新增的表或列，您可以选择应用这些变更到同步配置中。'
        }
        type={isBreaking ? 'error' : 'warning'}
        showIcon
        icon={<WarningOutlined />}
        action={
          <Space direction="vertical" size="small">
            <Button size="small" type="primary" onClick={handleViewDetails}>
              查看详情
            </Button>
          </Space>
        }
        style={{ marginBottom: 16 }}
      />

      <Modal
        title={
          <Space>
            {isBreaking ? (
              <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />
            ) : (
              <WarningOutlined style={{ color: '#faad14' }} />
            )}
            <span>Schema 变更详情</span>
          </Space>
        }
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        width={800}
        footer={
          <Space>
            <Button onClick={() => setDetailVisible(false)}>关闭</Button>
            <Button icon={<SyncOutlined />} loading={refreshing} onClick={handleRefreshSchema}>
              刷新 Schema
            </Button>
            <Button
              type="primary"
              icon={<CheckOutlined />}
              loading={applying}
              onClick={handleApplyChanges}
              danger={isBreaking}
            >
              应用变更
            </Button>
          </Space>
        }
      >
        <Spin spinning={loading}>
          <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
            <Descriptions.Item label="连接名称">{connection.name}</Descriptions.Item>
            <Descriptions.Item label="变更状态">
              <Tag color={isBreaking ? 'red' : 'orange'}>
                {isBreaking ? '破坏性变更' : '非破坏性变更'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="发现时间" span={2}>
              {connection.discoveredCatalogAt || '-'}
            </Descriptions.Item>
          </Descriptions>

          {renderStreamChanges()}
        </Spin>
      </Modal>
    </>
  );
};

export default SchemaChangeAlert;
