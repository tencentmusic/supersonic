import React, { useState, useEffect } from 'react';
import {
  Row,
  Col,
  DatePicker,
  Spin,
  message,
  Statistic,
  Progress,
  Card,
  Table,
  Empty,
  Typography,
} from 'antd';
import {
  ApiOutlined,
  CloudOutlined,
  DatabaseOutlined,
  TeamOutlined,
  RiseOutlined,
  FallOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  getCurrentTenant,
  getTenantUsageToday,
  getTenantUsageRange,
  getTenantUsageMonthly,
  Tenant,
  TenantUsage,
} from '@/services/tenant';
import type { SubscriptionPlan, TenantSubscription } from '@/services/tenant';
import {
  getCurrentSubscription,
  getSubscriptionPlans,
} from '@/services/subscription';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import styles from './style.less';

const { RangePicker } = DatePicker;
const { Title } = Typography;

const UsageDashboard: React.FC = () => {
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [todayUsage, setTodayUsage] = useState<TenantUsage | null>(null);
  const [monthlyUsage, setMonthlyUsage] = useState<TenantUsage | null>(null);
  const [rangeUsage, setRangeUsage] = useState<TenantUsage[]>([]);
  const [subscription, setSubscription] = useState<TenantSubscription | null>(null);
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(7, 'day'),
    dayjs(),
  ]);

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (dateRange[0] && dateRange[1]) {
      loadRangeUsage();
    }
  }, [dateRange]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [tenantRes, todayRes, monthlyRes, subscriptionRes, plansRes] = await Promise.all([
        getCurrentTenant(),
        getTenantUsageToday(),
        getTenantUsageMonthly(dayjs().year(), dayjs().month() + 1),
        getCurrentSubscription(),
        getSubscriptionPlans(),
      ]);

      if (tenantRes.code === 200 && tenantRes.data) {
        setTenant(tenantRes.data);
      }

      if (todayRes.code === 200 && todayRes.data) {
        setTodayUsage(todayRes.data);
      }

      if (monthlyRes.code === 200 && monthlyRes.data) {
        setMonthlyUsage(monthlyRes.data);
      }

      if (subscriptionRes.code === 200 && subscriptionRes.data) {
        setSubscription(subscriptionRes.data);
      }

      if (plansRes.code === 200 && plansRes.data) {
        setPlans(plansRes.data);
      }

      await loadRangeUsage();
    } catch (error) {
      message.error('加载使用量数据失败');
    } finally {
      setLoading(false);
    }
  };

  const loadRangeUsage = async () => {
    try {
      const res = await getTenantUsageRange(
        dateRange[0].format('YYYY-MM-DD'),
        dateRange[1].format('YYYY-MM-DD'),
      );
      if (res.code === 200 && res.data) {
        setRangeUsage(res.data);
      }
    } catch (error) {
      console.error('Load range usage failed:', error);
    }
  };

  const formatNumber = (num: number) => {
    if (num >= 1000000) {
      return (num / 1000000).toFixed(1) + 'M';
    }
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getUsagePercent = (used: number, max: number) => {
    if (max === 0) return 0;
    return Math.min(Math.round((used / max) * 100), 100);
  };

  const getStatusColor = (percent: number) => {
    if (percent >= 90) return '#ff4d4f';
    if (percent >= 70) return '#faad14';
    return '#52c41a';
  };

  const getTrend = (current: number, previous: number) => {
    if (previous === 0) return { percent: 0, isUp: true };
    const percent = Math.round(((current - previous) / previous) * 100);
    return { percent: Math.abs(percent), isUp: percent >= 0 };
  };

  const usageColumns = [
    {
      title: '日期',
      dataIndex: 'usageDate',
      key: 'usageDate',
    },
    {
      title: 'API调用次数',
      dataIndex: 'apiCalls',
      key: 'apiCalls',
      render: (val: number) => formatNumber(val),
    },
    {
      title: 'Token消耗',
      dataIndex: 'tokensUsed',
      key: 'tokensUsed',
      render: (val: number) => formatNumber(val),
    },
    {
      title: '查询次数',
      dataIndex: 'queryCount',
      key: 'queryCount',
      render: (val: number) => formatNumber(val),
    },
    {
      title: '活跃用户',
      dataIndex: 'activeUsers',
      key: 'activeUsers',
    },
    {
      title: '存储使用',
      dataIndex: 'storageBytes',
      key: 'storageBytes',
      render: (val: number) => formatBytes(val),
    },
  ];

  if (loading) {
    return (
      <div className={styles.loading}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  const currentPlan = subscription
    ? plans.find((p) => p.id === subscription.planId) || null
    : null;

  const maxApiCallsPerDay = currentPlan?.maxApiCallsPerDay || 0;
  const maxTokensPerMonth = currentPlan?.maxTokensPerMonth || 0;
  const maxUsers = currentPlan?.maxUsers || 0;

  const apiCallsPercent = getUsagePercent(
    todayUsage?.apiCalls || 0,
    maxApiCallsPerDay,
  );
  const tokensPercent = getUsagePercent(
    monthlyUsage?.tokensUsed || 0,
    maxTokensPerMonth,
  );

  return (
    <div className={styles.container}>
      <Card className={styles.heroCard}>
        <Title level={3} className={styles.heroTitle}>
          租户用量概览
        </Title>
      </Card>
      <Row gutter={[24, 24]}>
        {/* Today's Usage Overview */}
        <Col span={24}>
          <ProCard title="今日使用概览" className={styles.card}>
            <Row gutter={24}>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="API调用次数"
                    value={todayUsage?.apiCalls || 0}
                    prefix={<ApiOutlined className={styles.iconBlue} />}
                    suffix={
                      <span className={styles.limit}>/ {maxApiCallsPerDay === -1 ? '不限' : formatNumber(maxApiCallsPerDay)}</span>
                    }
                  />
                  <Progress
                    percent={apiCallsPercent}
                    strokeColor={getStatusColor(apiCallsPercent)}
                    size="small"
                    showInfo={false}
                  />
                  <div className={styles.percentText}>{apiCallsPercent}% 已使用</div>
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="Token消耗"
                    value={formatNumber(todayUsage?.tokensUsed || 0)}
                    prefix={<CloudOutlined className={styles.iconPurple} />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="查询次数"
                    value={todayUsage?.queryCount || 0}
                    prefix={<DatabaseOutlined className={styles.iconGreen} />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="活跃用户"
                    value={todayUsage?.activeUsers || 0}
                    prefix={<TeamOutlined className={styles.iconOrange} />}
                    suffix={<span className={styles.limit}>/ {maxUsers === -1 ? '不限' : maxUsers}</span>}
                  />
                </Card>
              </Col>
            </Row>
          </ProCard>
        </Col>

        {/* Monthly Usage */}
        <Col span={24}>
          <ProCard title="本月使用统计" className={styles.card}>
            <Row gutter={24}>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="累计Token消耗"
                    value={formatNumber(monthlyUsage?.tokensUsed || 0)}
                    prefix={<CloudOutlined className={styles.iconPurple} />}
                    suffix={
                      <span className={styles.limit}>
                        / {maxTokensPerMonth === -1 ? '不限' : formatNumber(maxTokensPerMonth)}
                      </span>
                    }
                  />
                  <Progress
                    percent={tokensPercent}
                    strokeColor={getStatusColor(tokensPercent)}
                    size="small"
                    showInfo={false}
                  />
                  <div className={styles.percentText}>{tokensPercent}% 已使用</div>
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="累计API调用"
                    value={formatNumber(monthlyUsage?.apiCalls || 0)}
                    prefix={<ApiOutlined className={styles.iconBlue} />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="累计查询次数"
                    value={formatNumber(monthlyUsage?.queryCount || 0)}
                    prefix={<DatabaseOutlined className={styles.iconGreen} />}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.statCard}>
                  <Statistic
                    title="存储使用量"
                    value={formatBytes(monthlyUsage?.storageBytes || 0)}
                    prefix={<CloudOutlined className={styles.iconOrange} />}
                  />
                </Card>
              </Col>
            </Row>
          </ProCard>
        </Col>

        {/* Historical Usage Table */}
        <Col span={24}>
          <ProCard
            title="历史使用记录"
            className={styles.card}
            extra={
              <RangePicker
                value={dateRange}
                onChange={(dates) => {
                  if (dates && dates[0] && dates[1]) {
                    setDateRange([dates[0], dates[1]]);
                  }
                }}
              />
            }
          >
            {rangeUsage.length > 0 ? (
              <Table
                dataSource={rangeUsage}
                columns={usageColumns}
                rowKey="usageDate"
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `共 ${total} 条记录`,
                }}
              />
            ) : (
              <Empty description="暂无使用记录" />
            )}
          </ProCard>
        </Col>
      </Row>
    </div>
  );
};

export default UsageDashboard;
