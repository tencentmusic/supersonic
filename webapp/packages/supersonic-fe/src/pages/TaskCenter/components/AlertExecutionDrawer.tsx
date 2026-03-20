import React, { useEffect, useState } from 'react';
import { Drawer, Table, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import type { AlertExecution } from '@/services/alertRule';
import { getExecutions } from '@/services/alertRule';

const { Text } = Typography;

const STATUS_MAP: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  RUNNING: { color: 'blue', text: '执行中' },
  SUCCESS: { color: 'green', text: '成功' },
  FAILED: { color: 'red', text: '失败' },
};

interface AlertExecutionDrawerProps {
  visible: boolean;
  ruleId?: number;
  ruleName?: string;
  onClose: () => void;
}

const AlertExecutionDrawer: React.FC<AlertExecutionDrawerProps> = ({
  visible,
  ruleId,
  ruleName,
  onClose,
}) => {
  const [data, setData] = useState<AlertExecution[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const fetchData = async (current = 1, pageSize = 10) => {
    if (!ruleId) return;
    setLoading(true);
    try {
      const res = await getExecutions(ruleId, { current, pageSize });
      setData(res?.records || []);
      setPagination({ current, pageSize, total: res?.total || 0 });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && ruleId) {
      fetchData();
    }
  }, [visible, ruleId]);

  const columns = [
    {
      title: '执行时间',
      dataIndex: 'startTime',
      width: 180,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '耗时',
      dataIndex: 'executionTimeMs',
      width: 100,
      render: (ms: number) => (ms != null ? `${(ms / 1000).toFixed(1)}s` : '-'),
    },
    {
      title: '总行数',
      dataIndex: 'totalRows',
      width: 100,
      render: (val: number) => (val != null ? val : '-'),
    },
    {
      title: '触发行数',
      dataIndex: 'alertedRows',
      width: 100,
      render: (val: number) => (val != null ? val : '-'),
    },
    {
      title: '静默行数',
      dataIndex: 'silencedRows',
      width: 100,
      render: (val: number) => (val != null ? val : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => {
        const info = STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      render: (msg: string) =>
        msg ? (
          <Text type="danger" ellipsis={{ tooltip: msg }} style={{ maxWidth: 200 }}>
            {msg}
          </Text>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <Drawer
      title={`执行记录 - ${ruleName || ''}`}
      open={visible}
      onClose={onClose}
      width={800}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          ...pagination,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => fetchData(page, size),
        }}
        size="small"
      />
    </Drawer>
  );
};

export default AlertExecutionDrawer;
