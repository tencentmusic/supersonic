import React, { useEffect, useState } from 'react';
import { useParams, history } from '@umijs/max';
import { Card, Tabs, Table, Tag, Descriptions, Button, Space, Spin, message, Typography } from 'antd';
import dayjs from 'dayjs';
import { getTopicDetail, type BusinessTopic, type TopicItem } from '@/services/businessTopic';
import { getFixedReportStatusInfo } from '@/utils/fixedReportStatus';
import taskStyles from '@/pages/TaskCenter/style.less';

const { Title, Text } = Typography;

const TopicWorkspacePage: React.FC = () => {
  const { topicId } = useParams<{ topicId: string }>();
  const id = topicId ? parseInt(topicId, 10) : NaN;
  const [topic, setTopic] = useState<BusinessTopic | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id || Number.isNaN(id)) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const res: any = await getTopicDetail(id);
        const data = res?.data ?? res;
        if (!cancelled) setTopic(data);
      } catch {
        if (!cancelled) message.error('加载主题失败');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!topic) {
    return (
      <div style={{ padding: 24 }}>
        <Text type="danger">主题不存在</Text>
      </div>
    );
  }

  const items = topic.items || [];
  const reports = items.filter((i) => i.itemType === 'FIXED_REPORT');
  const rules = items.filter((i) => i.itemType === 'ALERT_RULE');
  const schedules = items.filter((i) => i.itemType === 'SCHEDULE');

  const reportColumns = [
    {
      title: '名称',
      dataIndex: 'itemName',
      ellipsis: true,
      render: (name: string, r: TopicItem) => (
        <Button
          type="link"
          style={{ padding: 0 }}
          onClick={() => history.push(`/reports?highlightDataset=${r.itemId}`)}
        >
          {name}
        </Button>
      ),
    },
    {
      title: '消费状态',
      dataIndex: 'consumptionStatus',
      width: 160,
      render: (s: string | undefined) => {
        const info = getFixedReportStatusInfo(s);
        return info ? <Tag color={info.color}>{info.text}</Tag> : <Text type="secondary">-</Text>;
      },
    },
  ];

  const ruleColumns = [
    {
      title: '名称',
      dataIndex: 'itemName',
      render: (name: string, r: TopicItem) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => history.push(`/task-center?tab=alert&ruleId=${r.itemId}`)}>
          {name}
        </Button>
      ),
    },
  ];

  const schedColumns = [
    {
      title: '名称',
      dataIndex: 'itemName',
      render: (name: string, r: TopicItem) => (
        <Button type="link" style={{ padding: 0 }} onClick={() => history.push('/task-center?tab=schedule')}>
          {name}
        </Button>
      ),
    },
    {
      title: '启用',
      dataIndex: 'scheduleEnabled',
      width: 80,
      render: (e: boolean | undefined) => (e === undefined ? '-' : e ? '是' : '否'),
    },
  ];

  return (
    <div className={taskStyles.taskCenterPage}>
      <Space align="start" style={{ marginBottom: 16 }}>
        <Button onClick={() => history.push('/business-topics')}>返回列表</Button>
        <Title level={4} style={{ margin: 0 }}>
          {topic.name}
        </Title>
      </Space>

      <Card className={taskStyles.contentCard} style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="描述">{topic.description || '-'}</Descriptions.Item>
          <Descriptions.Item label="优先级">{topic.priority ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="默认投递配置 ID">
            {topic.defaultDeliveryConfigIds || <Text type="secondary">未配置</Text>}
          </Descriptions.Item>
          <Descriptions.Item label="更新">
            {topic.updatedAt ? dayjs(topic.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card className={taskStyles.contentCard}>
        <Tabs
          items={[
            {
              key: 'reports',
              label: `固定报表 (${reports.length})`,
              children: (
                <Table
                  size="middle"
                  rowKey={(r) => `${r.itemType}-${r.itemId}`}
                  pagination={false}
                  columns={reportColumns}
                  dataSource={reports}
                />
              ),
            },
            {
              key: 'alerts',
              label: `告警规则 (${rules.length})`,
              children: (
                <Table
                  size="middle"
                  rowKey={(r) => `${r.itemType}-${r.itemId}`}
                  pagination={false}
                  columns={ruleColumns}
                  dataSource={rules}
                />
              ),
            },
            {
              key: 'schedules',
              label: `定时任务 (${schedules.length})`,
              children: (
                <Table
                  size="middle"
                  rowKey={(r) => `${r.itemType}-${r.itemId}`}
                  pagination={false}
                  columns={schedColumns}
                  dataSource={schedules}
                />
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
};

export default TopicWorkspacePage;
