import React, { useEffect, useRef, useState } from 'react';
import {
  Drawer,
  Table,
  Tag,
  Button,
  Space,
  Popconfirm,
  Input,
  message,
  Empty,
  Descriptions,
  Typography,
  InputNumber,
} from 'antd';
import dayjs from 'dayjs';
import type { AlertEvent, ResolutionStatus } from '@/services/alertRule';
import { getEvents, transitionEvent } from '@/services/alertRule';

const { Text } = Typography;
const { TextArea } = Input;

const RESOLUTION_STATUS_MAP: Record<string, { color: string; text: string }> = {
  OPEN: { color: 'red', text: '待确认' },
  CONFIRMED: { color: 'orange', text: '已确认' },
  ASSIGNED: { color: 'blue', text: '已接手' },
  RESOLVED: { color: 'green', text: '已恢复' },
  CLOSED: { color: 'default', text: '已关闭' },
};

const SEVERITY_MAP: Record<string, { color: string; text: string }> = {
  CRITICAL: { color: 'red', text: '严重' },
  WARNING: { color: 'orange', text: '警告' },
};

const ALLOWED_TRANSITIONS: Record<string, { label: string; target: ResolutionStatus; danger?: boolean }[]> = {
  OPEN: [
    { label: '确认已知', target: 'CONFIRMED' },
    { label: '接手处理', target: 'ASSIGNED' },
  ],
  CONFIRMED: [
    { label: '接手处理', target: 'ASSIGNED' },
    { label: '标记恢复', target: 'RESOLVED' },
  ],
  ASSIGNED: [
    { label: '标记恢复', target: 'RESOLVED' },
  ],
  RESOLVED: [
    { label: '关闭', target: 'CLOSED' },
  ],
  CLOSED: [],
};

interface AlertEventDrawerProps {
  visible: boolean;
  ruleId?: number;
  ruleName?: string;
  onClose: () => void;
}

interface TransitionDraft {
  notes: string;
  assigneeId?: number;
}

const AlertEventDrawer: React.FC<AlertEventDrawerProps> = ({
  visible,
  ruleId,
  ruleName,
  onClose,
}) => {
  const [data, setData] = useState<AlertEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [drafts, setDrafts] = useState<Record<number, TransitionDraft>>({});
  const [transitioning, setTransitioning] = useState<number | null>(null);
  const transitioningEventIdsRef = useRef<Set<number>>(new Set());

  const getDraft = (eventId: number): TransitionDraft => drafts[eventId] || { notes: '' };

  const updateDraft = (eventId: number, updater: (draft: TransitionDraft) => TransitionDraft) => {
    setDrafts((current) => ({
      ...current,
      [eventId]: updater(current[eventId] || { notes: '' }),
    }));
  };

  const resetDraft = (eventId: number) => {
    setDrafts((current) => {
      if (!(eventId in current)) {
        return current;
      }
      const next = { ...current };
      delete next[eventId];
      return next;
    });
  };

  const fetchData = async (current = 1, pageSize = 20) => {
    if (!ruleId) return;
    setLoading(true);
    try {
      const params: any = { current, pageSize, ruleId };
      const res = await getEvents(params);
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch {
      message.error('加载异常事件失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && ruleId) {
      fetchData();
    }
  }, [visible, ruleId]);

  useEffect(() => {
    if (!visible) {
      setDrafts({});
      setTransitioning(null);
      transitioningEventIdsRef.current.clear();
    }
  }, [visible]);

  const handleTransition = async (eventId: number, target: ResolutionStatus) => {
    if (transitioningEventIdsRef.current.has(eventId)) {
      return;
    }
    const draft = getDraft(eventId);
    transitioningEventIdsRef.current.add(eventId);
    setTransitioning(eventId);
    try {
      await transitionEvent(eventId, {
        targetStatus: target,
        assigneeId: target === 'ASSIGNED' ? draft.assigneeId : undefined,
        notes: draft.notes || undefined,
      });
      message.success('状态已更新');
      resetDraft(eventId);
      fetchData(pagination.current, pagination.pageSize);
    } catch (e: any) {
      message.error(e?.data?.msg || '操作失败');
    } finally {
      transitioningEventIdsRef.current.delete(eventId);
      setTransitioning(null);
    }
  };

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '严重度',
      dataIndex: 'severity',
      width: 80,
      render: (val: string) => {
        const info = SEVERITY_MAP[val] || { color: 'default', text: val };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '维度值',
      dataIndex: 'dimensionValue',
      width: 120,
      ellipsis: true,
    },
    {
      title: '指标值',
      dataIndex: 'metricValue',
      width: 100,
      render: (val?: number) => (val != null ? val.toFixed(2) : '-'),
    },
    {
      title: '处置状态',
      dataIndex: 'resolutionStatus',
      width: 100,
      render: (status: string) => {
        const info = RESOLUTION_STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '确认人',
      dataIndex: 'acknowledgedBy',
      width: 100,
      render: (val?: string) => val || '-',
    },
    {
      title: '操作',
      width: 200,
      fixed: 'right' as const,
      render: (_: any, record: AlertEvent) => {
        const actions = ALLOWED_TRANSITIONS[record.resolutionStatus] || [];
        const draft = getDraft(record.id);
        if (actions.length === 0) return <Text type="secondary">已完结</Text>;
        return (
          <Space size={4} wrap>
            {actions.map((action) => (
              <Popconfirm
                key={action.target}
                title={
                  <div>
                    <div style={{ marginBottom: 8 }}>{`确认${action.label}？`}</div>
                    {action.target === 'ASSIGNED' && (
                      <div style={{ marginBottom: 8 }}>
                        <Text type="secondary">接手人 ID：</Text>
                        <InputNumber
                          size="small"
                          value={draft.assigneeId}
                          onChange={(v) =>
                            updateDraft(record.id, (current) => ({
                              ...current,
                              assigneeId: v ?? undefined,
                            }))
                          }
                          style={{ width: 100 }}
                        />
                      </div>
                    )}
                    <TextArea
                      rows={2}
                      placeholder="备注（可选）"
                      value={draft.notes}
                      onChange={(e) =>
                        updateDraft(record.id, (current) => ({
                          ...current,
                          notes: e.target.value,
                        }))
                      }
                    />
                  </div>
                }
                onConfirm={() => handleTransition(record.id, action.target)}
                okText="确认"
                cancelText="取消"
                onCancel={() => resetDraft(record.id)}
              >
                <Button
                  type="link"
                  size="small"
                  danger={action.danger}
                  loading={transitioning === record.id}
                  disabled={transitioning === record.id}
                >
                  {action.label}
                </Button>
              </Popconfirm>
            ))}
          </Space>
        );
      },
    },
  ];

  const expandedRowRender = (record: AlertEvent) => (
    <Descriptions column={2} size="small" bordered>
      <Descriptions.Item label="消息">{record.message || '-'}</Descriptions.Item>
      <Descriptions.Item label="基线值">
        {record.baselineValue != null ? record.baselineValue.toFixed(2) : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="偏差率">
        {record.deviationPct != null ? `${record.deviationPct.toFixed(1)}%` : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="投递状态">{record.deliveryStatus}</Descriptions.Item>
      <Descriptions.Item label="确认时间">
        {record.acknowledgedAt ? dayjs(record.acknowledgedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="接手时间">
        {record.assignedAt ? dayjs(record.assignedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="恢复时间">
        {record.resolvedAt ? dayjs(record.resolvedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="关闭时间">
        {record.closedAt ? dayjs(record.closedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
      </Descriptions.Item>
      {record.notes && (
        <Descriptions.Item label="处理记录" span={2}>
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 12 }}>{record.notes}</pre>
        </Descriptions.Item>
      )}
    </Descriptions>
  );

  return (
    <Drawer
      title={`异常事件 - ${ruleName || ''}`}
      open={visible}
      onClose={onClose}
      width={960}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        bordered={false}
        scroll={{ x: 'max-content' }}
        size="middle"
        expandable={{
          expandedRowRender,
          rowExpandable: () => true,
        }}
        locale={{
          emptyText: <Empty description="暂无异常事件" />,
        }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => fetchData(page, size),
        }}
      />
    </Drawer>
  );
};

export default AlertEventDrawer;
