import React, { useEffect, useState } from 'react';
import {
  Drawer,
  Descriptions,
  Tag,
  Table,
  Spin,
  Typography,
  Divider,
  Empty,
  Space,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { getExecutionSnapshot, ExecutionSnapshot, DeliveryRecord } from './service';

const { Text } = Typography;

const STATUS_COLOR: Record<string, string> = {
  SUCCESS: 'green',
  FAILED: 'red',
  PENDING: 'default',
  RUNNING: 'blue',
};

const STATUS_TEXT: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
  PENDING: '等待中',
  RUNNING: '执行中',
};

const TRIGGER_TYPE_TEXT: Record<string, string> = {
  MANUAL: '手动触发',
  SCHEDULE: '定时调度',
  AGENT: 'Agent',
  API: 'API',
};

const CHANNEL_TYPE_TEXT: Record<string, string> = {
  EMAIL: '邮件',
  FEISHU: '飞书',
  DINGTALK: '钉钉',
  WECHAT_WORK: '企业微信',
  WEBHOOK: 'Webhook',
};

const CHANNEL_TYPE_COLOR: Record<string, string> = {
  EMAIL: 'blue',
  FEISHU: 'cyan',
  DINGTALK: 'orange',
  WECHAT_WORK: 'green',
  WEBHOOK: 'purple',
};

interface ExecutionSnapshotDrawerProps {
  visible: boolean;
  executionId?: number;
  onClose: () => void;
}

const ExecutionSnapshotDrawer: React.FC<ExecutionSnapshotDrawerProps> = ({
  visible,
  executionId,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [snapshot, setSnapshot] = useState<ExecutionSnapshot | null>(null);

  useEffect(() => {
    if (visible && executionId) {
      fetchSnapshot(executionId);
    } else {
      setSnapshot(null);
    }
  }, [visible, executionId]);

  const fetchSnapshot = async (id: number) => {
    setLoading(true);
    try {
      const data = await getExecutionSnapshot(id);
      setSnapshot(data);
    } catch (e) {
      message.error('加载执行快照失败');
    } finally {
      setLoading(false);
    }
  };

  // Build result preview columns dynamically from first row
  const buildPreviewColumns = (rows: Record<string, any>[]): ColumnsType<Record<string, any>> => {
    if (!rows || rows.length === 0) return [];
    return Object.keys(rows[0]).map((key) => ({
      title: key,
      dataIndex: key,
      key,
      ellipsis: true,
      render: (val: any) => (val == null ? '-' : String(val)),
    }));
  };

  // Build delivery records columns
  const deliveryColumns: ColumnsType<DeliveryRecord> = [
    {
      title: '渠道',
      dataIndex: 'channelType',
      width: 100,
      render: (type: string) => (
        <Tag color={CHANNEL_TYPE_COLOR[type] || 'default'}>
          {CHANNEL_TYPE_TEXT[type] || type}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: string) => (
        <Tag color={STATUS_COLOR[status] || 'default'}>
          {STATUS_TEXT[status] || status}
        </Tag>
      ),
    },
    {
      title: '推送时间',
      dataIndex: 'deliveredAt',
      width: 180,
      render: (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
  ];

  const renderContent = () => {
    if (loading) {
      return (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <Spin tip="加载中..." />
        </div>
      );
    }

    if (!snapshot) {
      return <Empty description="暂无数据" />;
    }

    const paramsRows = Object.entries(snapshot.params || {}).map(([key, value]) => ({
      key,
      value,
    }));

    const previewColumns = buildPreviewColumns(snapshot.resultPreview || []);

    return (
      <>
        {/* 基本信息 */}
        <Descriptions column={2} bordered size="small" style={{ marginBottom: 20 }}>
          <Descriptions.Item label="模板名称" span={2}>
            <Space>
              <Text strong>{snapshot.templateName}</Text>
              <Tag>v{snapshot.templateVersion}</Tag>
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="触发方式">
            {TRIGGER_TYPE_TEXT[snapshot.triggerType] || snapshot.triggerType}
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={STATUS_COLOR[snapshot.status] || 'default'}>
              {STATUS_TEXT[snapshot.status] || snapshot.status}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="执行时间">
            {snapshot.executedAt ? dayjs(snapshot.executedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="耗时">
            {snapshot.durationMs != null ? `${(snapshot.durationMs / 1000).toFixed(1)} 秒` : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="结果行数" span={2}>
            {snapshot.resultRowCount != null ? `${snapshot.resultRowCount} 行` : '-'}
          </Descriptions.Item>
        </Descriptions>

        {/* 执行参数 */}
        {paramsRows.length > 0 && (
          <>
            <Divider orientation="left" plain>
              执行参数
            </Divider>
            <Table
              rowKey="key"
              size="small"
              dataSource={paramsRows}
              pagination={false}
              style={{ marginBottom: 20 }}
              columns={[
                { title: '参数名', dataIndex: 'key', width: 200 },
                { title: '参数值', dataIndex: 'value' },
              ]}
            />
          </>
        )}

        {/* 执行 SQL */}
        {snapshot.sql && (
          <>
            <Divider orientation="left" plain>
              执行 SQL
            </Divider>
            <pre
              style={{
                background: '#f5f5f5',
                border: '1px solid #e8e8e8',
                borderRadius: 4,
                padding: '12px 16px',
                fontSize: 12,
                lineHeight: 1.6,
                overflowX: 'auto',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
                marginBottom: 20,
              }}
            >
              {snapshot.sql}
            </pre>
          </>
        )}

        {/* 结果预览 */}
        <Divider orientation="left" plain>
          结果预览（前 20 行）
        </Divider>
        {snapshot.resultPreview && snapshot.resultPreview.length > 0 ? (
          <>
            <Table
              rowKey={(_, index) => String(index)}
              size="small"
              dataSource={snapshot.resultPreview}
              columns={previewColumns}
              pagination={false}
              scroll={{ x: 'max-content' }}
              style={{ marginBottom: 8 }}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              共 {snapshot.resultRowCount} 行
            </Text>
          </>
        ) : (
          <Empty description="无结果数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        )}

        {/* 推送记录 */}
        {snapshot.deliveryRecords && snapshot.deliveryRecords.length > 0 && (
          <>
            <Divider orientation="left" plain>
              推送记录
            </Divider>
            <Table
              rowKey={(_, index) => String(index)}
              size="small"
              dataSource={snapshot.deliveryRecords}
              columns={deliveryColumns}
              pagination={false}
            />
          </>
        )}
      </>
    );
  };

  return (
    <Drawer
      title="执行详情"
      open={visible}
      onClose={onClose}
      width={800}
      destroyOnClose
    >
      {renderContent()}
    </Drawer>
  );
};

export default ExecutionSnapshotDrawer;
