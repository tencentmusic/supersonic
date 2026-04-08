import React from 'react';
import { Drawer, Descriptions, Tag, Button, Space, Typography, Empty } from 'antd';
import { CalendarOutlined, StarOutlined, StarFilled } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { FixedReport } from '@/services/fixedReport';
import { DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';

const { Text } = Typography;

const STATUS_CONFIG: Record<string, { color: string; text: string }> = {
  AVAILABLE: { color: 'green', text: '可查看' },
  NO_RESULT: { color: 'default', text: '暂无结果' },
  EXPIRED: { color: 'orange', text: '结果过期' },
  RECENTLY_FAILED: { color: 'red', text: '最近失败' },
  NO_DELIVERY: { color: 'volcano', text: '未配置投递' },
  PARTIAL_CHANNEL_ERROR: { color: 'orange', text: '部分渠道异常' },
};

interface ReportDetailDrawerProps {
  visible: boolean;
  report?: FixedReport;
  onClose: () => void;
  onSubscribe: (datasetId: number) => void;
  onUnsubscribe: (datasetId: number) => void;
  onCreateSchedule: (datasetId: number) => void;
  onViewHistory: (datasetId: number) => void;
}

const ReportDetailDrawer: React.FC<ReportDetailDrawerProps> = ({
  visible,
  report,
  onClose,
  onSubscribe,
  onUnsubscribe,
  onCreateSchedule,
  onViewHistory,
}) => {
  if (!report) {
    return (
      <Drawer title="报表详情" open={visible} onClose={onClose} width={520}>
        <Empty description="未选择报表" />
      </Drawer>
    );
  }

  const statusInfo = STATUS_CONFIG[report.consumptionStatus] || STATUS_CONFIG.NO_RESULT;

  return (
    <Drawer
      title={report.reportName}
      open={visible}
      onClose={onClose}
      width={520}
      extra={
        <Space>
          <Button
            icon={report.subscribed ? <StarFilled /> : <StarOutlined />}
            type={report.subscribed ? 'primary' : 'default'}
            onClick={() =>
              report.subscribed
                ? onUnsubscribe(report.datasetId)
                : onSubscribe(report.datasetId)
            }
          >
            {report.subscribed ? '已订阅' : '订阅'}
          </Button>
        </Space>
      }
    >
      <Descriptions column={1} size="small" style={{ marginBottom: 24 }}>
        <Descriptions.Item label="状态">
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="业务域">{report.domainName || '—'}</Descriptions.Item>
        <Descriptions.Item label="口径摘要">{report.description || '—'}</Descriptions.Item>
        <Descriptions.Item label="最新结果时间">
          {report.latestResultTime
            ? dayjs(report.latestResultTime).format('YYYY-MM-DD HH:mm:ss')
            : '—'}
        </Descriptions.Item>
        {report.latestResultStatus === 'FAILED' && (
          <Descriptions.Item label="失败原因">
            <Text type="danger" ellipsis={{ tooltip: report.latestErrorMessage }}>
              {report.latestErrorMessage || '—'}
            </Text>
          </Descriptions.Item>
        )}
        {report.previousSuccessTime && (
          <Descriptions.Item label="上一版成功结果">
            {dayjs(report.previousSuccessTime).format('YYYY-MM-DD HH:mm:ss')}
            <Text type="secondary" style={{ marginLeft: 8 }}>
              (仍可参考)
            </Text>
          </Descriptions.Item>
        )}
        <Descriptions.Item label="数据行数">
          {report.latestRowCount != null ? report.latestRowCount : '—'}
        </Descriptions.Item>
        <Descriptions.Item label="定时任务">
          {report.scheduleCount > 0
            ? `${report.enabledScheduleCount} / ${report.scheduleCount} 启用`
            : '未创建'}
        </Descriptions.Item>
        <Descriptions.Item label="投递渠道">
          {report.deliveryChannels.length > 0 ? (
            <Space size={4} wrap>
              {report.deliveryChannels.map((ch) => {
                const info = DELIVERY_TYPE_MAP[ch.deliveryType] || {
                  color: 'default',
                  text: ch.deliveryType,
                };
                return (
                  <Tag key={ch.configId} color={ch.enabled ? info.color : 'default'}>
                    {ch.configName || info.text}
                    {!ch.enabled && ' (已禁用)'}
                  </Tag>
                );
              })}
            </Space>
          ) : (
            <Text type="secondary">未配置</Text>
          )}
        </Descriptions.Item>
      </Descriptions>

      <Space direction="vertical" style={{ width: '100%' }}>
        <Button block onClick={() => onViewHistory(report.datasetId)}>
          查看历史结果
        </Button>
        <Button
          block
          type="primary"
          icon={<CalendarOutlined />}
          onClick={() => onCreateSchedule(report.datasetId)}
        >
          创建定时报表任务
        </Button>
      </Space>
    </Drawer>
  );
};

export default ReportDetailDrawer;
