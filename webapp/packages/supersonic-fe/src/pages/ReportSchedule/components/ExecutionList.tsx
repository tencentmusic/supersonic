import React, { useEffect, useRef, useState } from 'react';
import { Drawer, Table, Tag, Button, Space, Typography, Tooltip, message } from 'antd';
import dayjs from 'dayjs';
import type { ReportExecution } from '@/services/reportSchedule';
import { getExecutionList, downloadExecutionResult } from '@/services/reportSchedule';
import ExecutionSnapshotDrawer from './ExecutionSnapshotDrawer';
import PageEmpty from '@/components/PageEmpty';

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
  const listLoadSucceededRef = useRef(false);
  const [snapshotDrawer, setSnapshotDrawer] = useState<{ visible: boolean; executionId?: number }>({ visible: false });

  const fetchData = async (current = 1, pageSize = 10) => {
    if (!scheduleId) return;
    setLoading(true);
    try {
      const res = await getExecutionList(scheduleId, { current, pageSize });
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
      listLoadSucceededRef.current = true;
    } catch (error) {
      message.error('加载执行记录失败');
      if (!listLoadSucceededRef.current) {
        setData([]);
        setPagination((p) => ({ ...p, current, pageSize, total: 0 }));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && scheduleId) {
      listLoadSucceededRef.current = false;
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
      width: 140,
      render: (_: any, record: ReportExecution) => (
        <Space size={4} wrap>
          <Button
            type="link"
            size="small"
            onClick={() => setSnapshotDrawer({ visible: true, executionId: record.id })}
          >
            详情
          </Button>
          {record.status === 'SUCCESS' && record.resultLocation && (
            <Button
              type="link"
              size="small"
              onClick={() => downloadExecutionResult(scheduleId!, record.id)}
            >
              下载
            </Button>
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
          bordered={false}
          scroll={{ x: 'max-content' }}
          size="middle"
          locale={{
            emptyText: <PageEmpty description="暂无执行记录，调度触发成功后将在此展示" />,
          }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => fetchData(page, size),
          }}
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
