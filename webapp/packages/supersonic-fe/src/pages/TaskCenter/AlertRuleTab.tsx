import React, { useEffect, useRef, useState } from 'react';
import dayjs from 'dayjs';
import { Button, Table, Switch, Space, Popconfirm, message, Tooltip, Empty, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import AlertRuleForm from './components/AlertRuleForm';
import AlertExecutionDrawer from './components/AlertExecutionDrawer';
import AlertEventDrawer from './components/AlertEventDrawer';
import {
  getRuleList,
  createRule,
  updateRule,
  deleteRule,
  pauseRule,
  resumeRule,
  triggerRule,
  getPendingEventCounts,
} from '@/services/alertRule';
import type { AlertRule } from '@/services/alertRule';
import { getValidDataSetList } from '@/services/reportSchedule';
import { MSG } from '@/common/messages';
import styles from './style.less';

const AlertRuleTab: React.FC = () => {
  const [data, setData] = useState<AlertRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<AlertRule | undefined>();
  const [executionDrawer, setExecutionDrawer] = useState<{
    visible: boolean;
    ruleId?: number;
    name?: string;
  }>({ visible: false });
  const [eventDrawer, setEventDrawer] = useState<{
    visible: boolean;
    ruleId?: number;
    name?: string;
  }>({ visible: false });
  const [pendingCounts, setPendingCounts] = useState<Record<number, number>>({});
  const [datasetNameMap, setDatasetNameMap] = useState<Record<number, string>>({});
  const triggeringRuleIdsRef = useRef<Set<number>>(new Set());
  const [triggeringRuleIds, setTriggeringRuleIds] = useState<Record<number, boolean>>({});

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res = await getRuleList({ current, pageSize });
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch (error) {
      message.error('加载告警规则失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchPendingCounts = async () => {
    try {
      const res: any = await getPendingEventCounts();
      const counts = res?.data ?? res;
      setPendingCounts(counts || {});
    } catch {
      // silent — badge counts are non-critical
    }
  };

  const fetchDatasetNames = async () => {
    try {
      const res = await getValidDataSetList();
      const list = res?.data ?? res;
      const map: Record<number, string> = {};
      (Array.isArray(list) ? list : []).forEach((d: { id: number; name: string }) => {
        map[d.id] = d.name;
      });
      setDatasetNameMap(map);
    } catch (error) {
      console.error('Failed to load dataset names', error);
    }
  };

  useEffect(() => {
    fetchData();
    fetchDatasetNames();
    fetchPendingCounts();
  }, []);

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: AlertRule) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleFormSubmit = async (values: Partial<AlertRule>) => {
    try {
      if (editRecord?.id) {
        await updateRule(editRecord.id, values);
        message.success(MSG.UPDATE_SUCCESS);
      } else {
        await createRule(values);
        message.success(MSG.CREATE_SUCCESS);
      }
      setFormVisible(false);
      fetchData(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteRule(id);
      message.success(MSG.DELETE_SUCCESS);
      fetchData(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error(MSG.OPERATION_FAILED);
    }
  };

  const handleToggle = async (record: AlertRule, checked: boolean) => {
    try {
      if (checked) {
        await resumeRule(record.id!);
      } else {
        await pauseRule(record.id!);
      }
      fetchData(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error(MSG.OPERATION_FAILED);
    }
  };

  const handleTrigger = async (id: number) => {
    if (triggeringRuleIdsRef.current.has(id)) {
      return;
    }
    triggeringRuleIdsRef.current.add(id);
    setTriggeringRuleIds((prev) => ({ ...prev, [id]: true }));
    try {
      await triggerRule(id);
      message.success('已触发检查');
    } catch (error) {
      message.error('触发失败');
    } finally {
      triggeringRuleIdsRef.current.delete(id);
      setTriggeringRuleIds((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    }
  };

  const columns = [
    { title: '规则名称', dataIndex: 'name', width: 200 },
    {
      title: '关联数据集',
      dataIndex: 'datasetId',
      width: 180,
      render: (id: number) =>
        datasetNameMap[id] != null ? `${datasetNameMap[id]} (${id})` : id ?? '-',
    },
    {
      title: 'Cron 频率',
      dataIndex: 'cronExpression',
      width: 150,
      render: (cron: string) =>
        cron ? (
          <Tooltip title={cron}>
            <code>{cron}</code>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (enabled: number, record: AlertRule) => (
        <Switch
          checked={enabled === 1}
          size="small"
          onChange={(checked) => handleToggle(record, checked)}
        />
      ),
    },
    {
      title: '上次检查',
      dataIndex: 'lastCheckTime',
      width: 180,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '下次检查',
      dataIndex: 'nextCheckTime',
      width: 180,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: AlertRule) => (
        <Space size={4} wrap>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            loading={!!triggeringRuleIds[record.id!]}
            disabled={!!triggeringRuleIds[record.id!]}
            onClick={() => handleTrigger(record.id!)}
          >
            立即触发
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => setExecutionDrawer({ visible: true, ruleId: record.id, name: record.name })}
          >
            执行记录
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() =>
              setEventDrawer({ visible: true, ruleId: record.id, name: record.name })
            }
          >
            异常事件
            {pendingCounts[record.id!] ? (
              <Tag color="red" style={{ marginLeft: 4, fontSize: 11 }}>
                {pendingCounts[record.id!]}
              </Tag>
            ) : null}
          </Button>
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id!)} okText="确认" cancelText="取消">
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className={styles.sectionHeader}>
        <div>
          <div className={styles.sectionTitle}>异常提醒</div>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          创建规则
        </Button>
      </div>
      <div className={styles.tableShell}>
        <Table
          rowKey="id"
          size="middle"
          bordered={false}
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{
            emptyText: (
              <Empty description="暂无告警规则">
                <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                  创建规则
                </Button>
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
      </div>
      <AlertRuleForm
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />
      <AlertExecutionDrawer
        visible={executionDrawer.visible}
        ruleId={executionDrawer.ruleId}
        ruleName={executionDrawer.name}
        onClose={() => setExecutionDrawer({ visible: false })}
      />
      <AlertEventDrawer
        visible={eventDrawer.visible}
        ruleId={eventDrawer.ruleId}
        ruleName={eventDrawer.name}
        onClose={() => {
          setEventDrawer({ visible: false });
          fetchPendingCounts();
        }}
      />
    </div>
  );
};

export default AlertRuleTab;
