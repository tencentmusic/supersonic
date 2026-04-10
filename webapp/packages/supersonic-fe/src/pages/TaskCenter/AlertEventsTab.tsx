import React, { useEffect, useState } from 'react';
import { Table, Tag, Button, Space, Select, message, Modal, Input } from 'antd';
import dayjs from 'dayjs';
import {
  getEvents,
  transitionEvent,
  type AlertEvent,
  type ResolutionStatus,
} from '@/services/alertRule';
import styles from './style.less';

const RES_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'OPEN', label: 'OPEN（待确认）' },
  { value: 'CONFIRMED', label: 'CONFIRMED' },
  { value: 'ASSIGNED', label: 'ASSIGNED' },
  { value: 'RESOLVED', label: 'RESOLVED' },
  { value: 'CLOSED', label: 'CLOSED' },
];

const AlertEventsTab: React.FC<{ initialResolutionStatus?: string }> = ({
  initialResolutionStatus = 'OPEN',
}) => {
  const [data, setData] = useState<AlertEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [resolutionFilter, setResolutionFilter] = useState<string>(
    initialResolutionStatus ?? 'OPEN',
  );

  useEffect(() => {
    setResolutionFilter(initialResolutionStatus ?? 'OPEN');
  }, [initialResolutionStatus]);

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const params: Record<string, unknown> = { current, pageSize };
      if (resolutionFilter) {
        params.resolutionStatus = resolutionFilter;
      }
      const res: any = await getEvents(params);
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({
        current,
        pageSize,
        total: pageData?.total || 0,
      });
    } catch {
      message.error('加载异常事件失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [resolutionFilter]);

  const openTransition = (record: AlertEvent, target: ResolutionStatus) => {
    let notes = '';
    Modal.confirm({
      title: `将事件 #${record.id} 置为 ${target}`,
      content:
        target === 'CONFIRMED' || target === 'ASSIGNED' || target === 'RESOLVED' ? (
          <Input.TextArea
            rows={3}
            placeholder="备注（可选）"
            onChange={(e) => {
              notes = e.target.value;
            }}
          />
        ) : null,
      onOk: async () => {
        await transitionEvent(record.id, { targetStatus: target, notes: notes || undefined });
        message.success('已更新');
        fetchData(pagination.current, pagination.pageSize);
      },
    });
  };

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm:ss'),
    },
    { title: '规则ID', dataIndex: 'ruleId', width: 90 },
    {
      title: '严重级别',
      dataIndex: 'severity',
      width: 90,
      render: (s: string) => <Tag>{s}</Tag>,
    },
    {
      title: '处置状态',
      dataIndex: 'resolutionStatus',
      width: 120,
      render: (s: string) => <Tag color="blue">{s}</Tag>,
    },
    {
      title: '摘要',
      dataIndex: 'message',
      ellipsis: true,
    },
    {
      title: '操作',
      width: 220,
      fixed: 'right' as const,
      render: (_: unknown, record: AlertEvent) => (
        <Space wrap size={4}>
          {record.resolutionStatus === 'OPEN' && (
            <>
              <Button type="link" size="small" onClick={() => openTransition(record, 'CONFIRMED')}>
                确认
              </Button>
              <Button type="link" size="small" onClick={() => openTransition(record, 'ASSIGNED')}>
                接手
              </Button>
            </>
          )}
          {record.resolutionStatus === 'CONFIRMED' && (
            <Button type="link" size="small" onClick={() => openTransition(record, 'ASSIGNED')}>
              接手
            </Button>
          )}
          {(record.resolutionStatus === 'ASSIGNED' || record.resolutionStatus === 'CONFIRMED') && (
            <Button type="link" size="small" onClick={() => openTransition(record, 'RESOLVED')}>
              已恢复
            </Button>
          )}
          {record.resolutionStatus === 'RESOLVED' && (
            <Button type="link" size="small" onClick={() => openTransition(record, 'CLOSED')}>
              关闭
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.scheduleTab}>
      <div style={{ marginBottom: 12 }}>
        <Space>
          <span>处置状态</span>
          <Select
            style={{ width: 260 }}
            value={resolutionFilter}
            onChange={(v) => setResolutionFilter(v)}
            options={RES_OPTIONS}
          />
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={data}
        scroll={{ x: 'max-content' }}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (p, ps) => fetchData(p, ps),
        }}
      />
    </div>
  );
};

export default AlertEventsTab;
