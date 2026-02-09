import React, { useEffect, useState } from 'react';
import { Drawer, Table, Tag, Button, Space, Popconfirm, message } from 'antd';
import { DownloadOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  getExportTaskList,
  downloadExportFile,
  cancelExportTask,
} from '@/services/exportTask';
import type { ExportTask } from '@/services/exportTask';

interface ExportTaskCenterProps {
  visible: boolean;
  onClose: () => void;
}

const STATUS_MAP: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  RUNNING: { color: 'blue', text: '执行中' },
  SUCCESS: { color: 'green', text: '完成' },
  FAILED: { color: 'red', text: '失败' },
  EXPIRED: { color: 'default', text: '已过期' },
};

const formatFileSize = (bytes?: number): string => {
  if (!bytes) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

const ExportTaskCenter: React.FC<ExportTaskCenterProps> = ({ visible, onClose }) => {
  const [data, setData] = useState<ExportTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  const fetchData = async (current = 1, pageSize = 10) => {
    setLoading(true);
    try {
      const res = await getExportTaskList({ current, pageSize });
      setData(res?.records || []);
      setPagination({ current, pageSize, total: res?.total || 0 });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible) {
      fetchData();
    }
  }, [visible]);

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
      ellipsis: true,
    },
    {
      title: '格式',
      dataIndex: 'outputFormat',
      width: 80,
      render: (fmt: string) => <Tag>{fmt}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (status: string) => {
        const info = STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      width: 100,
      render: formatFileSize,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 180,
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      width: 180,
    },
    {
      title: '操作',
      width: 120,
      render: (_: any, record: ExportTask) => (
        <Space>
          {record.status === 'SUCCESS' && (
            <Button
              type="link"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => downloadExportFile(record.id)}
            >
              下载
            </Button>
          )}
          {record.status === 'PENDING' && (
            <Popconfirm title="确认取消?" onConfirm={() => handleCancel(record.id)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                取消
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Drawer
      title="导出任务中心"
      open={visible}
      onClose={onClose}
      width={900}
      extra={
        <Button
          icon={<ReloadOutlined />}
          onClick={() => fetchData(pagination.current, pagination.pageSize)}
        >
          刷新
        </Button>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          ...pagination,
          onChange: (page, size) => fetchData(page, size),
        }}
        size="small"
      />
    </Drawer>
  );
};

export default ExportTaskCenter;
