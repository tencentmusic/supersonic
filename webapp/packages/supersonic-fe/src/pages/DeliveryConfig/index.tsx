import React, { useState, useEffect } from 'react';
import { Button, Table, Tag, Space, Popconfirm, message, Switch, Tooltip, Tabs, Badge } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UnorderedListOutlined,
  SendOutlined,
  BarChartOutlined,
  SettingOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getConfigList,
  deleteConfig,
  updateConfig,
  testConfig,
  DeliveryConfig,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';
import ConfigForm from './components/ConfigForm';
import RecordList from './components/RecordList';
import DeliveryDashboard from './components/DeliveryDashboard';

const DeliveryConfigPage: React.FC = () => {
  const [data, setData] = useState<DeliveryConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [activeTab, setActiveTab] = useState('dashboard');

  // Modal state
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<DeliveryConfig | undefined>();

  // Drawer state
  const [drawerState, setDrawerState] = useState<{
    visible: boolean;
    configId?: number;
    configName?: string;
  }>({ visible: false });

  const fetchData = async (page = pagination.current, size = pagination.pageSize) => {
    setLoading(true);
    try {
      const res = await getConfigList({ pageNum: page, pageSize: size });
      // Handle response format: could be direct data or wrapped in { code, data }
      const responseData = res?.data ?? res;
      const records = Array.isArray(responseData) ? responseData : (responseData?.records || []);
      const total = responseData?.total || records.length;
      setData(records);
      setPagination((prev) => ({ ...prev, current: page, pageSize: size, total }));
    } catch (error) {
      message.error('Failed to load delivery configs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: DeliveryConfig) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteConfig(id);
      message.success('Deleted successfully');
      fetchData();
    } catch (error) {
      message.error('Failed to delete');
    }
  };

  const handleToggleEnabled = async (record: DeliveryConfig, enabled: boolean) => {
    try {
      await updateConfig(record.id, { ...record, enabled });
      message.success(enabled ? 'Enabled' : 'Disabled');
      fetchData();
    } catch (error) {
      message.error('Failed to update');
    }
  };

  const handleTest = async (id: number) => {
    try {
      await testConfig(id);
      message.success('Test delivery sent successfully');
    } catch (error) {
      message.error('Test delivery failed');
    }
  };

  const handleFormSubmit = () => {
    setFormVisible(false);
    fetchData();
  };

  const handleShowRecords = (record: DeliveryConfig) => {
    setDrawerState({
      visible: true,
      configId: record.id,
      configName: record.name,
    });
  };

  // Count disabled configs that were auto-disabled
  const autoDisabledCount = data.filter(
    (c) => !c.enabled && (c as any).consecutiveFailures > 0
  ).length;

  const columns: ColumnsType<DeliveryConfig> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 60,
    },
    {
      title: '名称',
      dataIndex: 'name',
      width: 180,
      ellipsis: true,
    },
    {
      title: '渠道类型',
      dataIndex: 'deliveryType',
      width: 100,
      render: (type: string) => {
        const info = DELIVERY_TYPE_MAP[type as keyof typeof DELIVERY_TYPE_MAP];
        return info ? <Tag color={info.color}>{info.text}</Tag> : type;
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      width: 200,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 100,
      render: (enabled: boolean, record: any) => {
        if (!enabled && record.consecutiveFailures > 0) {
          return (
            <Tooltip title={`连续失败 ${record.consecutiveFailures} 次，已自动禁用`}>
              <Tag icon={<WarningOutlined />} color="error">
                已禁用
              </Tag>
            </Tooltip>
          );
        }
        return (
          <Switch
            size="small"
            checked={enabled}
            onChange={(checked) => handleToggleEnabled(record, checked)}
          />
        );
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 160,
    },
    {
      title: '创建者',
      dataIndex: 'createdBy',
      width: 100,
      ellipsis: true,
    },
    {
      title: '操作',
      width: 200,
      render: (_: any, record: DeliveryConfig) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            />
          </Tooltip>
          <Tooltip title="测试发送">
            <Button
              type="link"
              size="small"
              icon={<SendOutlined />}
              onClick={() => handleTest(record.id)}
            />
          </Tooltip>
          <Tooltip title="推送记录">
            <Button
              type="link"
              size="small"
              icon={<UnorderedListOutlined />}
              onClick={() => handleShowRecords(record)}
            />
          </Tooltip>
          <Popconfirm
            title="确认删除此推送配置?"
            onConfirm={() => handleDelete(record.id)}
            okText="确认"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button type="link" size="small" danger icon={<DeleteOutlined />} />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const tabItems = [
    {
      key: 'dashboard',
      label: (
        <span>
          <BarChartOutlined />
          推送统计
        </span>
      ),
      children: <DeliveryDashboard />,
    },
    {
      key: 'configs',
      label: (
        <span>
          <SettingOutlined />
          渠道配置
          {autoDisabledCount > 0 && (
            <Badge count={autoDisabledCount} size="small" style={{ marginLeft: 8 }} />
          )}
        </span>
      ),
      children: (
        <>
          <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新建配置
            </Button>
          </div>

          <Table
            rowKey="id"
            columns={columns}
            dataSource={data}
            loading={loading}
            pagination={{
              ...pagination,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (page, size) => fetchData(page, size),
            }}
          />
        </>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 16 }}>推送渠道管理</h2>

      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />

      <ConfigForm
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />

      <RecordList
        visible={drawerState.visible}
        configId={drawerState.configId}
        configName={drawerState.configName}
        onClose={() => setDrawerState({ visible: false })}
      />
    </div>
  );
};

export default DeliveryConfigPage;
