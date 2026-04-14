import React, { useRef, useState, useEffect } from 'react';
import { Button, Table, Tag, Space, Popconfirm, message, Switch, Tooltip, Tabs, Badge, Empty } from 'antd';
import dayjs from 'dayjs';
import { useModel } from '@umijs/max';
import {
  PlusOutlined,
  DeleteOutlined,
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
import styles from './index.less';

const DeliveryConfigPage: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser as API.CurrentUser | undefined;
  const canManageConfig = Boolean(currentUser?.superAdmin || currentUser?.isAdmin === 1);
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
      message.error('加载推送配置失败');
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
      message.success('删除成功');
      fetchData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleToggleEnabled = async (record: DeliveryConfig, enabled: boolean) => {
    try {
      await updateConfig(record.id, { ...record, enabled });
      message.success(enabled ? '已启用' : '已禁用');
      fetchData();
    } catch (error) {
      message.error('更新失败');
    }
  };

  const [testingId, setTestingId] = useState<number | null>(null);
  const testingIdsRef = useRef<Set<number>>(new Set());

  const getErrorMessage = (error: any, fallback: string) => {
    return (
      error?.response?.data?.msg ||
      error?.data?.msg ||
      error?.msg ||
      error?.message ||
      fallback
    );
  };

  const handleTest = async (id: number) => {
    if (testingIdsRef.current.has(id)) {
      return;
    }
    testingIdsRef.current.add(id);
    setTestingId(id);
    try {
      const res = await testConfig(id);
      if (res?.code && res.code !== 200) {
        message.error(res.msg || '测试推送失败');
      } else {
        message.success('测试推送已发送');
      }
    } catch (error) {
      message.error(getErrorMessage(error, '测试推送失败'));
    } finally {
      testingIdsRef.current.delete(id);
      setTestingId(null);
      fetchData();
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
        if (!canManageConfig) {
          return <Tag color={enabled ? 'success' : 'default'}>{enabled ? '已启用' : '已禁用'}</Tag>;
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
      render: (createdAt?: string) =>
        createdAt ? dayjs(createdAt).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '创建者',
      dataIndex: 'createdBy',
      width: 100,
      ellipsis: true,
    },
    {
      title: '操作',
      width: 220,
      render: (_: any, record: DeliveryConfig) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleShowRecords(record)}>
            记录
          </Button>
          {canManageConfig && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              编辑
            </Button>
          )}
          {canManageConfig && (
            <Button
              type="link"
              size="small"
              loading={testingId === record.id}
              onClick={() => handleTest(record.id)}
            >
              测试
            </Button>
          )}
          {canManageConfig && (
            <Popconfirm
              title="确认删除此推送配置?"
              onConfirm={() => handleDelete(record.id)}
              okText="确认"
              cancelText="取消"
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const tabItems = [
    {
      key: 'dashboard',
      label: '推送统计',
      children: <DeliveryDashboard />,
    },
    {
      key: 'configs',
      label: (
        <span>
          渠道配置
          {autoDisabledCount > 0 && (
            <Badge count={autoDisabledCount} size="small" style={{ marginLeft: 8 }} />
          )}
        </span>
      ),
      children: (
        <>
          {canManageConfig && (
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                新建配置
              </Button>
            </div>
          )}

          <Table
            rowKey="id"
            size="middle"
            columns={columns}
            dataSource={data}
            loading={loading}
            scroll={{ x: 'max-content' }}
            locale={{
              emptyText: (
                <Empty description="暂无推送配置">
                  {canManageConfig && (
                    <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                      新建配置
                    </Button>
                  )}
                </Empty>
              ),
            }}
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
      <h2 style={{ marginBottom: 16 }}>推送渠道</h2>

      <Tabs
        className={styles.deliveryTabs}
        activeKey={activeTab}
        onChange={setActiveTab}
        items={tabItems}
      />

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
