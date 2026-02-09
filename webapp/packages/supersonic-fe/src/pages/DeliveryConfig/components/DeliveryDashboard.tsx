import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Progress, Tag, Table, Select, Spin, Empty } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SendOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import {
  getStatistics,
  getDailyStats,
  DeliveryStatistics,
  DailyDeliveryStats,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';

const DeliveryDashboard: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [days, setDays] = useState(7);
  const [stats, setStats] = useState<DeliveryStatistics | null>(null);
  const [dailyStats, setDailyStats] = useState<DailyDeliveryStats[]>([]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [statsRes, dailyRes] = await Promise.all([
        getStatistics(days),
        getDailyStats(days),
      ]);
      // Handle response format: could be direct data or wrapped in { code, data }
      const statsData = statsRes?.data ?? statsRes;
      const dailyData = dailyRes?.data ?? dailyRes;
      setStats(statsData || null);
      setDailyStats(Array.isArray(dailyData) ? dailyData : []);
    } catch (error) {
      console.error('Failed to load statistics', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [days]);

  const getSuccessRateColor = (rate: number) => {
    if (rate >= 95) return '#52c41a';
    if (rate >= 80) return '#faad14';
    return '#ff4d4f';
  };

  const dailyColumns = [
    {
      title: '日期',
      dataIndex: 'date',
      width: 120,
    },
    {
      title: '总数',
      dataIndex: 'total',
      width: 80,
    },
    {
      title: '成功',
      dataIndex: 'success',
      width: 80,
      render: (val: number) => <span style={{ color: '#52c41a' }}>{val}</span>,
    },
    {
      title: '失败',
      dataIndex: 'failed',
      width: 80,
      render: (val: number) => <span style={{ color: val > 0 ? '#ff4d4f' : '#999' }}>{val}</span>,
    },
    {
      title: '成功率',
      dataIndex: 'successRate',
      width: 120,
      render: (rate: number) => (
        <Progress
          percent={Math.round(rate)}
          size="small"
          strokeColor={getSuccessRateColor(rate)}
          format={(p) => `${p?.toFixed(1)}%`}
        />
      ),
    },
  ];

  if (loading && !stats) {
    return (
      <div style={{ textAlign: 'center', padding: 40 }}>
        <Spin size="large" />
      </div>
    );
  }

  // Check if there's any actual data
  const hasData = stats && stats.totalDeliveries > 0;

  // Show empty state when no data
  if (!hasData) {
    return (
      <div>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>推送统计</h3>
          <Select
            value={days}
            onChange={setDays}
            style={{ width: 120 }}
            options={[
              { label: '最近7天', value: 7 },
              { label: '最近14天', value: 14 },
              { label: '最近30天', value: 30 },
            ]}
          />
        </div>
        <Empty
          description="暂无推送记录"
          style={{ padding: 60 }}
        />
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0 }}>推送统计</h3>
        <Select
          value={days}
          onChange={setDays}
          style={{ width: 120 }}
          options={[
            { label: '最近7天', value: 7 },
            { label: '最近14天', value: 14 },
            { label: '最近30天', value: 30 },
          ]}
        />
      </div>

      {/* Overview Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="总推送数"
              value={stats?.totalDeliveries || 0}
              prefix={<SendOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="成功"
              value={stats?.successCount || 0}
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="失败"
              value={stats?.failedCount || 0}
              prefix={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: stats?.failedCount ? '#ff4d4f' : undefined }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="进行中"
              value={stats?.pendingCount || 0}
              prefix={<ClockCircleOutlined style={{ color: '#1890ff' }} />}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {/* Overall Success Rate */}
        <Col span={8}>
          <Card size="small" title="总体成功率">
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="circle"
                percent={Math.round(stats?.successRate || 0)}
                strokeColor={getSuccessRateColor(stats?.successRate || 0)}
                format={(p) => `${p?.toFixed(1)}%`}
              />
            </div>
          </Card>
        </Col>

        {/* Success Rate by Channel */}
        <Col span={8}>
          <Card size="small" title="各渠道成功率">
            {stats?.successRateByType && Object.keys(stats.successRateByType).length > 0 ? (
              <div>
                {Object.entries(stats.successRateByType).map(([type, rate]) => {
                  const typeInfo = DELIVERY_TYPE_MAP[type as keyof typeof DELIVERY_TYPE_MAP];
                  return (
                    <div key={type} style={{ marginBottom: 8 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Tag color={typeInfo?.color}>{typeInfo?.text || type}</Tag>
                        <span>{rate.toFixed(1)}%</span>
                      </div>
                      <Progress
                        percent={Math.round(rate)}
                        size="small"
                        strokeColor={getSuccessRateColor(rate)}
                        showInfo={false}
                      />
                    </div>
                  );
                })}
              </div>
            ) : (
              <div style={{ color: '#999', textAlign: 'center', padding: 20 }}>暂无数据</div>
            )}
          </Card>
        </Col>

        {/* Average Delivery Time */}
        <Col span={8}>
          <Card size="small" title="平均推送耗时">
            <div style={{ textAlign: 'center', padding: 20 }}>
              <Statistic
                value={stats?.avgDeliveryTimeMs ? Math.round(stats.avgDeliveryTimeMs) : 0}
                suffix="ms"
                prefix={<ThunderboltOutlined style={{ color: '#1890ff' }} />}
              />
            </div>
          </Card>
        </Col>
      </Row>

      {/* Daily Stats Table */}
      <Card size="small" title="每日趋势">
        <Table
          rowKey="date"
          columns={dailyColumns}
          dataSource={dailyStats}
          size="small"
          pagination={false}
          loading={loading}
        />
      </Card>
    </div>
  );
};

export default DeliveryDashboard;
