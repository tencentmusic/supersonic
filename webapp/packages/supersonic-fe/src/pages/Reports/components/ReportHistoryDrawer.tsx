import React, { useEffect, useState } from 'react';
import { Drawer, Table, Tag, Typography, message, Empty } from 'antd';
import dayjs from 'dayjs';
import { getReportExecutions } from '@/services/fixedReport';

const { Text } = Typography;

const STATUS_MAP: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  RUNNING: { color: 'blue', text: '执行中' },
  SUCCESS: { color: 'green', text: '成功' },
  FAILED: { color: 'red', text: '失败' },
};

interface ReportHistoryDrawerProps {
  visible: boolean;
  datasetId?: number;
  reportName?: string;
  onClose: () => void;
}

const ReportHistoryDrawer: React.FC<ReportHistoryDrawerProps> = ({
  visible,
  datasetId,
  reportName,
  onClose,
}) => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  const fetchData = async (current = 1, pageSize = 20) => {
    if (!datasetId) return;
    setLoading(true);
    try {
      const res: any = await getReportExecutions(datasetId, { current, pageSize });
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch {
      message.error('加载执行历史失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && datasetId) {
      fetchData();
    }
  }, [visible, datasetId]);

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
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => {
        const info = STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '数据行数',
      dataIndex: 'rowCount',
      width: 100,
      render: (val: number) => (val != null ? val : '-'),
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
      title={`执行历史 - ${reportName || ''}`}
      open={visible}
      onClose={onClose}
      width={800}
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        bordered={false}
        scroll={{ x: 'max-content' }}
        size="middle"
        locale={{
          emptyText: <Empty description="暂无执行记录" />,
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

export default ReportHistoryDrawer;
