import React, { useEffect, useState } from 'react';
import { Drawer, Table, Tag, Tooltip, Typography } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import moment from 'moment';
import { getJobHistory, DataSyncExecutionDO } from '@/services/connection';

interface JobHistoryTableProps {
  visible: boolean;
  connectionId?: number;
  connectionName?: string;
  onClose: () => void;
}

const STATUS_CONFIG: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
  PENDING: { color: 'default', icon: <ClockCircleOutlined />, text: '等待中' },
  RUNNING: { color: 'processing', icon: <SyncOutlined spin />, text: '运行中' },
  SUCCESS: { color: 'success', icon: <CheckCircleOutlined />, text: '成功' },
  FAILED: { color: 'error', icon: <CloseCircleOutlined />, text: '失败' },
};

const formatDuration = (start?: string, end?: string): string => {
  if (!start || !end) return '-';
  const startTime = moment(start);
  const endTime = moment(end);
  const durationMs = endTime.diff(startTime);
  if (durationMs < 1000) return `${durationMs}ms`;
  if (durationMs < 60000) return `${(durationMs / 1000).toFixed(1)}s`;
  return `${(durationMs / 60000).toFixed(1)}min`;
};

const formatNumber = (num?: number): string => {
  if (num === undefined || num === null) return '-';
  return num.toLocaleString();
};

const JobHistoryTable: React.FC<JobHistoryTableProps> = ({
  visible,
  connectionId,
  connectionName,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [jobs, setJobs] = useState<DataSyncExecutionDO[]>([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const fetchJobs = async (current = 1, pageSize = 10) => {
    if (!connectionId) return;
    setLoading(true);
    try {
      const res = await getJobHistory(connectionId, { current, pageSize });
      setJobs(res?.records || []);
      setPagination({ current, pageSize, total: res?.total || 0 });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && connectionId) {
      fetchJobs();
    }
  }, [visible, connectionId]);

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => {
        const config = STATUS_CONFIG[status] || STATUS_CONFIG.PENDING;
        return (
          <Tag color={config.color} icon={config.icon}>
            {config.text}
          </Tag>
        );
      },
    },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      width: 160,
      render: (val: string) => (val ? moment(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '耗时',
      width: 80,
      render: (_: any, record: DataSyncExecutionDO) =>
        formatDuration(record.startTime, record.endTime),
    },
    {
      title: '读取行数',
      dataIndex: 'rowsRead',
      width: 100,
      render: (val: number) => formatNumber(val),
    },
    {
      title: '写入行数',
      dataIndex: 'rowsWritten',
      width: 100,
      render: (val: number) => formatNumber(val),
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      ellipsis: true,
      render: (val: string) =>
        val ? (
          <Tooltip title={val}>
            <Typography.Text type="danger" ellipsis style={{ maxWidth: 200 }}>
              {val}
            </Typography.Text>
          </Tooltip>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <Drawer
      title={`执行历史 - ${connectionName || ''}`}
      open={visible}
      onClose={onClose}
      width={900}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={jobs}
        loading={loading}
        pagination={{
          ...pagination,
          onChange: (page, size) => fetchJobs(page, size),
        }}
        size="small"
      />
    </Drawer>
  );
};

export default JobHistoryTable;
