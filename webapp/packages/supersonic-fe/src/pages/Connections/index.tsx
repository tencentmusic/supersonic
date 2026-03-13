import React, { useEffect, useState } from 'react';
import {
  Button,
  Table,
  Tag,
  Space,
  Popconfirm,
  message,
  Tooltip,
  Alert,
  Select,
  Input,
} from 'antd';
import {
  PlusOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  HistoryOutlined,
  UnorderedListOutlined,
  ReloadOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import StatusBadge from './components/StatusBadge';
import ConnectionForm from './components/ConnectionForm';
import TimelineDrawer from './components/TimelineDrawer';
import JobHistoryTable from './components/JobHistoryTable';
import SchemaChangeAlert from './components/SchemaChangeAlert';
import StateResetModal from './components/StateResetModal';
import {
  listConnections,
  deleteConnection,
  pauseConnection,
  resumeConnection,
  triggerSync,
  ConnectionDO,
} from '@/services/connection';
import { getDatabaseList } from '@/pages/SemanticModel/service';
import { MSG } from '@/common/messages';

const SCHEMA_CHANGE_STATUS: Record<string, { color: string; text: string }> = {
  NO_CHANGE: { color: 'default', text: '无变更' },
  NON_BREAKING: { color: 'blue', text: '非破坏性变更' },
  BREAKING: { color: 'red', text: '破坏性变更' },
};

const ConnectionsPage: React.FC = () => {
  const [data, setData] = useState<ConnectionDO[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<ConnectionDO | undefined>();
  const [databases, setDatabases] = useState<Record<number, any>>({});
  const [timelineDrawer, setTimelineDrawer] = useState<{
    visible: boolean;
    id?: number;
    name?: string;
  }>({ visible: false });
  const [jobsDrawer, setJobsDrawer] = useState<{ visible: boolean; id?: number; name?: string }>({
    visible: false,
  });
  const [resetModal, setResetModal] = useState<{ visible: boolean; connection?: ConnectionDO }>({
    visible: false,
  });

  // Filters
  const [filterSourceDb, setFilterSourceDb] = useState<number | undefined>();
  const [filterDestDb, setFilterDestDb] = useState<number | undefined>();
  const [filterStatus, setFilterStatus] = useState<string | undefined>();

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res = await listConnections({
        current,
        pageSize,
        sourceDbId: filterSourceDb,
        destDbId: filterDestDb,
        status: filterStatus,
      });
      setData(res?.records || []);
      setPagination({ current, pageSize, total: res?.total || 0 });
    } finally {
      setLoading(false);
    }
  };

  const fetchDatabases = async () => {
    try {
      const { code, data } = await getDatabaseList();
      if (code === 200) {
        const dbMap: Record<number, any> = {};
        (data || []).forEach((db: any) => {
          dbMap[db.id] = db;
        });
        setDatabases(dbMap);
      }
    } catch (e) {
      console.error('Failed to fetch databases', e);
    }
  };

  useEffect(() => {
    fetchData();
    fetchDatabases();
  }, []);

  useEffect(() => {
    fetchData(1, pagination.pageSize);
  }, [filterSourceDb, filterDestDb, filterStatus]);

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: ConnectionDO) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleDelete = async (id: number) => {
    await deleteConnection(id);
    message.success(MSG.DELETE_SUCCESS);
    fetchData(pagination.current, pagination.pageSize);
  };

  const handlePause = async (id: number) => {
    await pauseConnection(id);
    message.success('已暂停');
    fetchData(pagination.current, pagination.pageSize);
  };

  const handleResume = async (id: number) => {
    await resumeConnection(id);
    message.success('已恢复');
    fetchData(pagination.current, pagination.pageSize);
  };

  const handleTrigger = async (id: number) => {
    await triggerSync(id);
    message.success('已触发同步');
  };

  const getDatabaseName = (id: number) => {
    const db = databases[id];
    return db ? `${db.name} (${db.type})` : `ID: ${id}`;
  };

  // Check if any connection has breaking changes
  const hasBreakingChanges = data.some(
    (conn) => conn.schemaChangeStatus === 'BREAKING',
  );

  const columns = [
    {
      title: '连接名称',
      dataIndex: 'name',
      width: 180,
      render: (name: string, record: ConnectionDO) => (
        <Space>
          <span>{name}</span>
          {record.schemaChangeStatus === 'BREAKING' && (
            <Tooltip title="有破坏性 Schema 变更">
              <WarningOutlined style={{ color: '#ff4d4f' }} />
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '源数据库',
      dataIndex: 'sourceDatabaseId',
      width: 180,
      render: (id: number) => (
        <Tooltip title={getDatabaseName(id)}>
          <span>{getDatabaseName(id)}</span>
        </Tooltip>
      ),
    },
    {
      title: '目标数据库',
      dataIndex: 'destinationDatabaseId',
      width: 180,
      render: (id: number) => (
        <Tooltip title={getDatabaseName(id)}>
          <span>{getDatabaseName(id)}</span>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => <StatusBadge status={status} />,
    },
    {
      title: '调度',
      dataIndex: 'scheduleType',
      width: 120,
      render: (type: string, record: ConnectionDO) => {
        if (type === 'MANUAL') return <Tag>手动</Tag>;
        return (
          <Tooltip title={record.cronExpression}>
            <Tag color="blue">{record.cronExpression || '按计划'}</Tag>
          </Tooltip>
        );
      },
    },
    {
      title: 'Schema 状态',
      dataIndex: 'schemaChangeStatus',
      width: 120,
      render: (status: string) => {
        const config = SCHEMA_CHANGE_STATUS[status] || SCHEMA_CHANGE_STATUS.NO_CHANGE;
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 160,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '操作',
      width: 280,
      render: (_: any, record: ConnectionDO) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          {record.status !== 'DEPRECATED' && (
            <>
              <Button
                type="link"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => handleTrigger(record.id!)}
              >
                同步
              </Button>
              {record.status === 'ACTIVE' || record.status === 'BROKEN' ? (
                <Button
                  type="link"
                  size="small"
                  icon={<PauseCircleOutlined />}
                  onClick={() => handlePause(record.id!)}
                >
                  暂停
                </Button>
              ) : record.status === 'PAUSED' ? (
                <Button
                  type="link"
                  size="small"
                  icon={<ReloadOutlined />}
                  onClick={() => handleResume(record.id!)}
                >
                  恢复
                </Button>
              ) : null}
            </>
          )}
          <Button
            type="link"
            size="small"
            icon={<HistoryOutlined />}
            onClick={() =>
              setTimelineDrawer({ visible: true, id: record.id, name: record.name })
            }
          >
            Timeline
          </Button>
          <Button
            type="link"
            size="small"
            icon={<UnorderedListOutlined />}
            onClick={() => setJobsDrawer({ visible: true, id: record.id, name: record.name })}
          >
            Jobs
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ReloadOutlined />}
            onClick={() => setResetModal({ visible: true, connection: record })}
          >
            重置
          </Button>
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      {hasBreakingChanges && (
        <Alert
          message="Schema 变更警告"
          description="部分连接检测到破坏性 Schema 变更，请及时处理以避免同步失败。"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          closable
        />
      )}

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2 style={{ margin: 0 }}>数据连接</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          创建连接
        </Button>
      </div>

      <div style={{ marginBottom: 16 }}>
        <Space>
          <Select
            placeholder="源数据库"
            allowClear
            style={{ width: 200 }}
            value={filterSourceDb}
            onChange={setFilterSourceDb}
            options={Object.values(databases).map((db: any) => ({
              label: `${db.name} (${db.type})`,
              value: db.id,
            }))}
          />
          <Select
            placeholder="目标数据库"
            allowClear
            style={{ width: 200 }}
            value={filterDestDb}
            onChange={setFilterDestDb}
            options={Object.values(databases).map((db: any) => ({
              label: `${db.name} (${db.type})`,
              value: db.id,
            }))}
          />
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            value={filterStatus}
            onChange={setFilterStatus}
            options={[
              { label: '运行中', value: 'ACTIVE' },
              { label: '已暂停', value: 'PAUSED' },
              { label: '已中断', value: 'BROKEN' },
              { label: '已废弃', value: 'DEPRECATED' },
            ]}
          />
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          ...pagination,
          onChange: (page, size) => fetchData(page, size),
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        scroll={{ x: 1200 }}
        expandable={{
          expandedRowRender: (record) =>
            record.schemaChangeStatus !== 'NO_CHANGE' ? (
              <SchemaChangeAlert
                connection={record}
                onRefresh={() => fetchData(pagination.current, pagination.pageSize)}
              />
            ) : null,
          rowExpandable: (record) => record.schemaChangeStatus !== 'NO_CHANGE',
        }}
      />

      <ConnectionForm
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSuccess={() => {
          setFormVisible(false);
          fetchData(pagination.current, pagination.pageSize);
        }}
      />

      <TimelineDrawer
        visible={timelineDrawer.visible}
        connectionId={timelineDrawer.id}
        connectionName={timelineDrawer.name}
        onClose={() => setTimelineDrawer({ visible: false })}
      />

      <JobHistoryTable
        visible={jobsDrawer.visible}
        connectionId={jobsDrawer.id}
        connectionName={jobsDrawer.name}
        onClose={() => setJobsDrawer({ visible: false })}
      />

      <StateResetModal
        visible={resetModal.visible}
        connection={resetModal.connection}
        onClose={() => setResetModal({ visible: false })}
        onSuccess={() => fetchData(pagination.current, pagination.pageSize)}
      />
    </div>
  );
};

export default ConnectionsPage;
