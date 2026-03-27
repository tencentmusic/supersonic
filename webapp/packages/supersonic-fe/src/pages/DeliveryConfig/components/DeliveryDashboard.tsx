import React, { useEffect, useState } from 'react';
import { Table, Select, Spin, Empty, Typography, Space, Divider } from 'antd';
import {
  getStatistics,
  getDailyStats,
  DeliveryStatistics,
  DailyDeliveryStats,
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
      render: (val: number) => <span style={{ color: '#12b76a' }}>{val}</span>,
    },
    {
      title: '失败',
      dataIndex: 'failed',
      width: 80,
      render: (val: number) => <span style={{ color: val > 0 ? '#f04438' : '#667085' }}>{val}</span>,
    },
    {
      title: '成功率',
      dataIndex: 'successRate',
      width: 120,
      render: (rate: number) => (
        <span style={{ color: '#667085' }}>{Number.isFinite(rate) ? `${rate.toFixed(1)}%` : '-'}</span>
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

  const kpiText = (label: string, value: number | string) => (
    <span>
      <Typography.Text type="secondary">{label}</Typography.Text>
      <Typography.Text style={{ marginLeft: 6 }}>{value}</Typography.Text>
    </span>
  );

  const header = (
    <div
      style={{
        marginBottom: 12,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}
    >
      <Typography.Title level={5} style={{ margin: 0, fontWeight: 600 }}>
        推送统计
      </Typography.Title>
      <Select
        value={days}
        onChange={setDays}
        size="small"
        variant="borderless"
        style={{ width: 120 }}
        options={[
          { label: '最近7天', value: 7 },
          { label: '最近14天', value: 14 },
          { label: '最近30天', value: 30 },
        ]}
      />
    </div>
  );

  // Show empty state when no data
  if (!hasData) {
    return (
      <div>
        {header}
        <Empty
          description={null}
          style={{ padding: 48 }}
        />
      </div>
    );
  }

  return (
    <div>
      {header}
      <Space size={10} wrap style={{ marginBottom: 12 }}>
        {kpiText('总数', stats?.totalDeliveries || 0)}
        <Typography.Text type="secondary">·</Typography.Text>
        {kpiText('成功', stats?.successCount || 0)}
        <Typography.Text type="secondary">·</Typography.Text>
        {kpiText('失败', stats?.failedCount || 0)}
        <Typography.Text type="secondary">·</Typography.Text>
        {kpiText('处理中', stats?.pendingCount || 0)}
        <Typography.Text type="secondary">·</Typography.Text>
        {kpiText('成功率', `${(stats?.successRate || 0).toFixed(1)}%`)}
        <Typography.Text type="secondary">·</Typography.Text>
        {kpiText(
          '平均耗时',
          `${stats?.avgDeliveryTimeMs ? Math.round(stats.avgDeliveryTimeMs) : 0}ms`,
        )}
      </Space>
      <Divider style={{ margin: '8px 0 12px' }} />
      <Table
        rowKey="date"
        columns={dailyColumns}
        dataSource={dailyStats}
        size="small"
        pagination={false}
        loading={loading}
        bordered={false}
      />
    </div>
  );
};

export default DeliveryDashboard;
