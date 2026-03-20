import React, { useEffect, useState } from 'react';
import { Button, Table, Tag, Space, Popconfirm, message } from 'antd';
import { DownloadOutlined, StopOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  getExportTaskList,
  downloadExportFile,
  cancelExportTask,
  ExportTask,
} from '@/services/exportTask';

const STATUS_LABEL: Record<string, string> = {
  PENDING: '等待中',
  RUNNING: '执行中',
  SUCCESS: '成功',
  FAILED: '失败',
  EXPIRED: '已过期',
};

const STATUS_COLOR: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'blue',
  SUCCESS: 'green',
  FAILED: 'red',
  EXPIRED: 'orange',
};

function formatFileSize(bytes?: number): string {
  if (bytes == null) return '—';
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  }
  return `${(bytes / 1024).toFixed(2)} KB`;
}

const ExportTaskTab: React.FC = () => {
  const [data, setData] = useState<ExportTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res: any = await getExportTaskList({ current, pageSize });
      if (res?.code === 200 && res?.data) {
        setData(res.data.records || []);
        setPagination({ current, pageSize, total: res.data.total || 0 });
      } else {
        setData([]);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const timer = setInterval(() => {
      fetchData(pagination.current, pagination.pageSize);
    }, 10000);
    return () => clearInterval(timer);
  }, []);

  const handleCancel = async (id: number) => {
    await cancelExportTask(id);
    message.success('已取消');
    fetchData(pagination.current, pagination.pageSize);
  };

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'taskName',
      width: 200,
      render: (val: string) => val || '—',
    },
    {
      title: '格式',
      dataIndex: 'outputFormat',
      width: 100,
      render: (fmt: string) => <Tag>{fmt}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={STATUS_COLOR[status] || 'default'}>
          {STATUS_LABEL[status] || status}
        </Tag>
      ),
    },
    {
      title: '数据行数',
      dataIndex: 'rowCount',
      width: 100,
      render: (val?: number) => (val != null ? val : '—'),
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      width: 120,
      render: (val?: number) => formatFileSize(val),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
      render: (val?: string) =>
        val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '—',
    },
    {
      title: '操作',
      width: 160,
      render: (_: any, record: ExportTask) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            disabled={record.status !== 'SUCCESS'}
            onClick={() => downloadExportFile(record.id)}
          >
            下载
          </Button>
          {(record.status === 'PENDING' || record.status === 'RUNNING') && (
            <Popconfirm title="确认取消?" onConfirm={() => handleCancel(record.id)}>
              <Button type="link" size="small" danger icon={<StopOutlined />}>
                取消
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={data}
      loading={loading}
      pagination={{
        ...pagination,
        onChange: (page, size) => fetchData(page, size),
      }}
    />
  );
};

export default ExportTaskTab;
