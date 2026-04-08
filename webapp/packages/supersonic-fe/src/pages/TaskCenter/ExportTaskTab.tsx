import React, { useEffect, useState, useRef } from 'react';
import { Button, Table, Tag, Space, Popconfirm, message, Empty } from 'antd';
import dayjs from 'dayjs';
import {
  getExportTaskList,
  downloadExportFile,
  cancelExportTask,
  ExportTask,
} from '@/services/exportTask';
import styles from './style.less';

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
  const paginationRef = useRef({ current: 1, pageSize: 20 });

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res: any = await getExportTaskList({ current, pageSize });
      // Handle both envelope shapes: {code, data: {records, total}} and {records, total}
      const body = (res?.code === 200 && res?.data) ? res.data : res;
      setData(Array.isArray(body) ? body : (body?.records || []));
      setPagination({ current, pageSize, total: body?.total || 0 });
      paginationRef.current = { current, pageSize };
    } catch (error) {
      message.error('加载导出任务失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const timer = setInterval(() => {
      fetchData(paginationRef.current.current, paginationRef.current.pageSize);
    }, 10000);
    return () => clearInterval(timer);
  }, []);

  const handleCancel = async (id: number) => {
    try {
      await cancelExportTask(id);
      message.success('已取消');
      fetchData(paginationRef.current.current, paginationRef.current.pageSize);
    } catch (error) {
      message.error('取消失败');
    }
  };

  const columns = [
    {
      title: '任务名称',
      dataIndex: 'taskName',
      width: 200,
      ellipsis: true,
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
      width: 120,
      fixed: 'right' as const,
      render: (_: any, record: ExportTask) => (
        <Space size={4} wrap>
          <Button
            type="link"
            size="small"
            disabled={record.status !== 'SUCCESS'}
            onClick={() => downloadExportFile(record.id)}
          >
            下载
          </Button>
          {(record.status === 'PENDING' || record.status === 'RUNNING') && (
            <Popconfirm title="确认取消?" onConfirm={() => handleCancel(record.id)} okText="确认" cancelText="取消">
              <Button type="link" size="small" danger>
                取消
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div className={styles.sectionHeader}>
        <div>
          <div className={styles.sectionTitle}>导出任务</div>
        </div>
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
            emptyText: <Empty description="暂无导出任务" />,
          }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => fetchData(page, size),
          }}
        />
      </div>
    </div>
  );
};

export default ExportTaskTab;
