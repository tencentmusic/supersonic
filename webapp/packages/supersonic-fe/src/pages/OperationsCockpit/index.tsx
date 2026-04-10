import React, { useEffect, useState } from 'react';
import { Card, Col, Row, Table, Tag, Typography, Button, Space, Spin, Empty } from 'antd';
import { history } from '@umijs/max';
import dayjs from 'dayjs';
import {
  getOperationsCockpit,
  type OperationsCockpitData,
  type CockpitFixedReportSummary,
} from '@/services/operationsCockpit';
import styles from './index.less';

const { Title, Paragraph, Text } = Typography;

const STATUS_COLORS: Record<string, string> = {
  AVAILABLE: 'green',
  NO_RESULT: 'default',
  EXPIRED: 'orange',
  RECENTLY_FAILED: 'red',
  NO_DELIVERY: 'volcano',
  PARTIAL_CHANNEL_ERROR: 'orange',
};

const OperationsCockpitPage: React.FC = () => {
  const [data, setData] = useState<OperationsCockpitData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const res: any = await getOperationsCockpit();
        const cockpitData = Number(res?.code) === 200 ? res.data : (res?.topics !== undefined ? res : null);
        if (!cancelled) setData(cockpitData);
      } catch {
        if (!cancelled) setData(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className={styles.wrap}>
        <Spin size="large" />
      </div>
    );
  }

  if (!data) {
    return (
      <div className={styles.wrap}>
        <Empty description="无法加载驾驶舱数据" />
      </div>
    );
  }

  const reportColumns = [
    {
      title: '报表',
      dataIndex: 'reportName',
      ellipsis: true,
      render: (_: string, r: CockpitFixedReportSummary) => (
        <Button
          type="link"
          style={{ padding: 0 }}
          onClick={() => history.push(`/reports?highlightDataset=${r.datasetId}`)}
        >
          {r.reportName}
        </Button>
      ),
    },
    {
      title: '状态',
      dataIndex: 'consumptionStatus',
      width: 140,
      render: (s: string) =>
        s ? (
          <Tag color={STATUS_COLORS[s] || 'default'}>{s}</Tag>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
    {
      title: '最近结果',
      dataIndex: 'latestResultTime',
      width: 180,
      render: (t: string) =>
        t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : <Text type="secondary">-</Text>,
    },
  ];

  const eventColumns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '规则',
      dataIndex: 'ruleId',
      width: 90,
      render: (id: number) => (
        <Button type="link" size="small" onClick={() => history.push(`/task-center?tab=alert&ruleId=${id}`)}>
          #{id}
        </Button>
      ),
    },
    {
      title: '状态',
      dataIndex: 'resolutionStatus',
      width: 120,
      render: (s: string) => <Tag>{s}</Tag>,
    },
    {
      title: '摘要',
      dataIndex: 'message',
      ellipsis: true,
    },
  ];

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <Space wrap>
          <Title level={4} style={{ marginBottom: 4 }}>
            每日经营驾驶舱
          </Title>
          <Button type="link" onClick={() => history.push('/responsibility-ledger')}>
            查看运行总览
          </Button>
        </Space>
        <Paragraph type="secondary" style={{ marginBottom: 0 }}>
          今日优先主题、关键报表状态、待处置异常与可靠性风险摘要
        </Paragraph>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="经营主题（按优先级）" bordered={false} className={styles.card}>
            {!data.topics?.length ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无启用主题" />
            ) : (
              <ul className={styles.topicList}>
                {data.topics.map((t) => (
                  <li key={t.id} className={styles.topicItem}>
                    <Space wrap>
                      <Button
                        type="link"
                        onClick={() => history.push(`/business-topics/workspace/${t.id}`)}
                      >
                        {t.name}
                      </Button>
                      <Text type="secondary">
                        固定报表 {t.fixedReportCount} · 告警 {t.alertRuleCount} · 定时任务{' '}
                        {t.scheduleCount}
                      </Text>
                    </Space>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <span>待处置异常</span>
                <Tag color="red">{data.pendingAlertEventCount}</Tag>
              </Space>
            }
            bordered={false}
            className={styles.card}
            extra={
              <Button type="link" size="small" onClick={() => history.push('/task-center?tab=alertEvents')}>
                去处置
              </Button>
            }
          >
            {!data.pendingAlertEvents?.length ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无待处置事件" />
            ) : (
              <Table
                size="small"
                rowKey="id"
                pagination={false}
                columns={eventColumns}
                dataSource={data.pendingAlertEvents}
              />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col span={24}>
          <Card
            title="关键固定报表"
            bordered={false}
            className={styles.card}
            extra={
              <Button type="link" onClick={() => history.push('/reports')}>
                全部固定报表
              </Button>
            }
          >
            <Table
              size="small"
              rowKey="datasetId"
              pagination={false}
              columns={reportColumns}
              dataSource={data.keyReports}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col span={24}>
          <Card
            title="可靠性风险"
            bordered={false}
            className={styles.card}
            extra={
              <Button type="link" onClick={() => history.push('/task-center?tab=schedule')}>
                任务中心
              </Button>
            }
          >
            {!data.reliabilityRisks?.length ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前无突出风险报表" />
            ) : (
              <Table
                size="small"
                rowKey="datasetId"
                pagination={false}
                columns={reportColumns}
                dataSource={data.reliabilityRisks}
              />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default OperationsCockpitPage;
