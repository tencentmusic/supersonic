import React, { useEffect, useRef, useState } from 'react';
import { Drawer, Table, Tag, Button, Space, Typography, Tooltip, Spin, Empty, message } from 'antd';
import dayjs from 'dayjs';
import type { ReportExecution } from '@/services/reportSchedule';
import { getExecutionList, downloadExecutionResult } from '@/services/reportSchedule';
import ExecutionSnapshotDrawer from './ExecutionSnapshotDrawer';
import { getExecutionSnapshot } from './ExecutionSnapshotDrawer/service';
import type { ExecutionSnapshot } from './ExecutionSnapshotDrawer/service';
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

const TRIGGER_TYPE_MAP: Record<string, { text: string; color: string }> = {
  MANUAL: { text: '手动', color: 'blue' },
  SCHEDULE: { text: '定时', color: 'green' },
  WEB: { text: '页面', color: 'cyan' },
  AGENT: { text: 'Agent', color: 'purple' },
  API: { text: 'API', color: 'orange' },
};

const ExecutionList: React.FC<ExecutionListProps> = ({ visible, scheduleId, scheduleName, onClose }) => {
  const [data, setData] = useState<ReportExecution[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const listLoadSucceededRef = useRef(false);
  const [snapshotDrawer, setSnapshotDrawer] = useState<{ visible: boolean; executionId?: number }>({ visible: false });

  // Expandable row preview state
  const [expandedKeys, setExpandedKeys] = useState<number[]>([]);
  const [previewData, setPreviewData] = useState<Record<number, ExecutionSnapshot>>({});
  const [previewLoading, setPreviewLoading] = useState<Record<number, boolean>>({});

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
      setExpandedKeys([]);
      setPreviewData({});
      setPreviewLoading({});
      fetchData();
    }
  }, [visible, scheduleId]);

  const handleExpand = async (expanded: boolean, record: ReportExecution) => {
    if (expanded && !previewData[record.id]) {
      setPreviewLoading((prev) => ({ ...prev, [record.id]: true }));
      try {
        const snapshot = await getExecutionSnapshot(record.id);
        setPreviewData((prev) => ({ ...prev, [record.id]: snapshot }));
      } catch (e) {
        message.error('加载预览失败');
      } finally {
        setPreviewLoading((prev) => ({ ...prev, [record.id]: false }));
      }
    }
    setExpandedKeys(
      expanded ? [...expandedKeys, record.id] : expandedKeys.filter((k) => k !== record.id),
    );
  };

  const columns = [
    {
      title: '模板/任务',
      dataIndex: 'templateName',
      width: 160,
      ellipsis: true,
      render: (val: string) => (
        <Tooltip title={val || scheduleName || '-'}>
          <Text ellipsis style={{ maxWidth: 140 }}>{val || scheduleName || '-'}</Text>
        </Tooltip>
      ),
    },
    {
      title: '触发方式',
      dataIndex: 'triggerType',
      width: 80,
      render: (val: string) => {
        const info = TRIGGER_TYPE_MAP[val];
        if (!info) return val ? <Tag>{val}</Tag> : '-';
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '执行时间',
      dataIndex: 'startTime',
      width: 170,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '耗时',
      dataIndex: 'executionTimeMs',
      width: 80,
      render: (ms: number) => (ms != null ? `${(ms / 1000).toFixed(1)}s` : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: string) => {
        const info = STATUS_MAP[status] || { color: 'default', text: status };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '数据行数',
      dataIndex: 'rowCount',
      width: 90,
      render: (val: number) => val ?? '-',
    },
    {
      title: '操作',
      width: 160,
      render: (_: any, record: ReportExecution) => (
        <Space size={4} wrap>
          {record.hasPreview && (
            <Button
              type="link"
              size="small"
              onClick={() => {
                const isExpanded = expandedKeys.includes(record.id);
                handleExpand(!isExpanded, record);
              }}
            >
              {expandedKeys.includes(record.id) ? '收起' : '预览'}
            </Button>
          )}
          {record.status === 'SUCCESS' && record.resultLocation && (
            <Button
              type="link"
              size="small"
              onClick={() => downloadExecutionResult(scheduleId!, record.id)}
            >
              下载
            </Button>
          )}
          <Button
            type="link"
            size="small"
            onClick={() => setSnapshotDrawer({ visible: true, executionId: record.id })}
          >
            详情
          </Button>
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
        width={1000}
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
          expandable={{
            expandedRowKeys: expandedKeys,
            onExpand: handleExpand,
            expandedRowRender: (record: ReportExecution) => {
              if (previewLoading[record.id]) {
                return <Spin size="small" />;
              }
              const snapshot = previewData[record.id];
              if (!snapshot?.resultPreview?.length) {
                return <Empty description="无预览数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
              }

              const firstRow = snapshot.resultPreview[0];
              if (!firstRow || typeof firstRow !== 'object') {
                return <Empty description="无预览数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
              }
              const cols = Object.keys(firstRow).map((key: string) => ({
                title: key,
                dataIndex: key,
                key,
                ellipsis: true,
                render: (val: any) => (val == null ? '-' : String(val)),
              }));

              return (
                <div style={{ padding: '8px 0' }}>
                  <Table
                    rowKey={(_: any, i?: number) => String(i)}
                    size="small"
                    bordered
                    dataSource={snapshot.resultPreview}
                    columns={cols}
                    pagination={false}
                    scroll={{ x: 'max-content', y: 300 }}
                  />
                  <Text
                    type="secondary"
                    style={{ fontSize: 12, marginTop: 4, display: 'block' }}
                  >
                    共 {snapshot.resultRowCount ?? '-'} 行（预览前 20 行）
                  </Text>
                </div>
              );
            },
            rowExpandable: (record: ReportExecution) => !!record.hasPreview,
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
