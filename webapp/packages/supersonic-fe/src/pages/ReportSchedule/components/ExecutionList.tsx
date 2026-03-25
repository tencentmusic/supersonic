import React, { useEffect, useState } from 'react';
import { Drawer, Table, Tag, Button, Space, Typography, Tooltip, message } from 'antd';
import { DownloadOutlined, FileSearchOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { ReportExecution } from '@/services/reportSchedule';
import { getExecutionList, downloadExecutionResult } from '@/services/reportSchedule';
import ExecutionSnapshotDrawer from './ExecutionSnapshotDrawer';

const { Text } = Typography;

interface ExecutionListProps {
  visible: boolean;
  scheduleId?: number;
  scheduleName?: string;
  onClose: () => void;
}

const STATUS_MAP: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'default', text: '等待中' },
  RUNNING: { color: 'blue', text: '执行中' },
  SUCCESS: { color: 'green', text: '成功' },
  FAILED: { color: 'red', text: '失败' },
};

const ExecutionList: React.FC<ExecutionListProps> = ({ visible, scheduleId, scheduleName, onClose }) => {
  const [data, setData] = useState<ReportExecution[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [snapshotDrawer, setSnapshotDrawer] = useState<{ visible: boolean; executionId?: number }>({ visible: false });

  const fetchData = async (current = 1, pageSize = 10) => {
    if (!scheduleId) return;
    setLoading(true);
    try {
      const res = await getExecutionList(scheduleId, { current, pageSize });
      setData(res?.records || []);
      setPagination({ current, pageSize, total: res?.total || 0 });
    } catch (error) {
      message.error('加载执行记录失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && scheduleId) {
      fetchData();
    }
  }, [visible, scheduleId]);

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
      render: (ms: number) => ms != null ? `${(ms / 1000).toFixed(1)}s` : '-',
    },
    {
      title: '尝试',
      dataIndex: 'attempt',
      width: 80,
      render: (attempt: number) => `第${attempt}次`,
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
      render: (val: number) => val ?? '-',
    },
    {
      title: '操作',
      width: 120,
      render: (_: any, record: ReportExecution) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<FileSearchOutlined />}
              onClick={() => setSnapshotDrawer({ visible: true, executionId: record.id })}
            />
          </Tooltip>
          {record.status === 'SUCCESS' && record.resultLocation && (
            <Tooltip title="下载结果">
              <Button
                type="link"
                size="small"
                icon={<DownloadOutlined />}
                onClick={() => downloadExecutionResult(scheduleId!, record.id)}
              />
            </Tooltip>
          )}
          {record.status === 'FAILED' && record.errorMessage && (
            <Tooltip title={record.errorMessage}>
              <Text type="danger" style={{ fontSize: 12 }} ellipsis>
                {record.errorMessage}
              </Text>
            </Tooltip>
          )}
        </Space>
      ),
    },
  ];

  return (
    <>
      <Drawer
        title={`执行记录 - ${scheduleName || ''}`}
        open={visible}
        onClose={onClose}
        width={900}
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
      <ExecutionSnapshotDrawer
        visible={snapshotDrawer.visible}
        executionId={snapshotDrawer.executionId}
        onClose={() => setSnapshotDrawer({ visible: false })}
      />
    </>
  );
};

export default ExecutionList;
