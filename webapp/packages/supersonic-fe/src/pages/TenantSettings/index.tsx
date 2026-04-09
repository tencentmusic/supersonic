import React, { useState, useEffect } from 'react';
import {
  Card,
  Form,
  Input,
  Button,
  message,
  Descriptions,
  Progress,
  Row,
  Col,
  Statistic,
  Tabs,
  Avatar,
  Spin,
  Modal,
  Radio,
  Space,
  Popconfirm,
  Tag,
} from 'antd';
import {
  TeamOutlined,
  DatabaseOutlined,
  RobotOutlined,
  ApiOutlined,
  CloudOutlined,
  MailOutlined,
  PhoneOutlined,
  UserOutlined,
  SwapOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import {
  getCurrentTenant,
  updateCurrentTenant,
  getTenantUsageToday,
  getTenantUsageMonthly,
  Tenant,
  TenantUsage,
} from '@/services/tenant';
import {
  getCurrentSubscription,
  getSubscriptionPlans,
  changeSubscription,
  cancelSubscription,
} from '@/services/subscription';
import type { TenantSubscription, SubscriptionPlan } from '@/services/tenant';
import { MSG } from '@/common/messages';
import styles from './style.less';

const { TabPane } = Tabs;
const { TextArea } = Input;

const TenantSettings: React.FC = () => {
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [usage, setUsage] = useState<TenantUsage | null>(null);
  const [monthlyUsage, setMonthlyUsage] = useState<TenantUsage | null>(null);
  const [subscription, setSubscription] = useState<TenantSubscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();
  const [changePlanVisible, setChangePlanVisible] = useState(false);
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);
  const [changingPlan, setChangingPlan] = useState(false);

  useEffect(() => {
    loadTenantData();
  }, []);

  const loadTenantData = async () => {
    setLoading(true);
    try {
      const [tenantRes, usageRes, subscriptionRes, plansRes] = await Promise.all([
        getCurrentTenant(),
        getTenantUsageToday(),
        getCurrentSubscription(),
        getSubscriptionPlans(),
      ]);

      if (tenantRes.code === 200 && tenantRes.data) {
        setTenant(tenantRes.data);
        form.setFieldsValue(tenantRes.data);
      }

      if (usageRes.code === 200 && usageRes.data) {
        setUsage(usageRes.data);
      }

      if (subscriptionRes.code === 200 && subscriptionRes.data) {
        setSubscription(subscriptionRes.data);
      }

      if (plansRes.code === 200 && plansRes.data) {
        setPlans(plansRes.data);
      }

      // Get monthly usage
      const now = new Date();
      const monthlyRes = await getTenantUsageMonthly(now.getFullYear(), now.getMonth() + 1);
      if (monthlyRes.code === 200 && monthlyRes.data) {
        setMonthlyUsage(monthlyRes.data);
      }
    } catch (error) {
      message.error('加载租户信息失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const res = await updateCurrentTenant(values);
      if (res.code === 200) {
        message.success(MSG.SAVE_SUCCESS);
        setTenant(res.data);
      } else {
        message.error(res.msg || MSG.SAVE_FAILED);
      }
    } catch (error) {
      message.error(MSG.SAVE_FAILED);
    } finally {
      setSaving(false);
    }
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
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

  const getUsagePercent = (used: number, max: number) => {
    if (max === 0) return 0;
    return Math.min(Math.round((used / max) * 100), 100);
  };

  const getStatusColor = (percent: number) => {
    if (percent >= 90) return '#ff4d4f';
    if (percent >= 70) return '#faad14';
    return '#52c41a';
  };

  const handleOpenChangePlan = () => {
    setSelectedPlanId(subscription?.planId || null);
    setChangePlanVisible(true);
  };

  const handleChangePlan = async () => {
    if (!selectedPlanId) {
      message.warning('请选择一个计划');
      return;
    }
    if (selectedPlanId === subscription?.planId) {
      message.warning('请选择不同的计划');
      return;
    }

    setChangingPlan(true);
    try {
      const res = await changeSubscription({ planId: selectedPlanId });
      if (res.code === 200) {
        message.success('计划更改成功');
        setChangePlanVisible(false);
        loadTenantData();
      } else {
        message.error(res.msg || '更改计划失败');
      }
    } catch (error) {
      message.error('更改计划失败');
    } finally {
      setChangingPlan(false);
    }
  };

  const handleCancelSubscription = async () => {
    try {
      const res = await cancelSubscription();
      if (res.code === 200) {
        message.success('订阅已取消');
        loadTenantData();
      } else {
        message.error(res.msg || '取消订阅失败');
      }
    } catch (error) {
      message.error('取消订阅失败');
    }
  };

  const formatPrice = (price?: number) => {
    if (price === undefined || price === null) return '-';
    return `¥${price.toFixed(2)}`;
  };

  const currentPlan = subscription
    ? plans.find((p) => p.id === subscription.planId) || null
    : null;

  if (loading) {
    return (
      <div className={styles.loading}>
        <Spin size="large" tip="加载中..." />
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <Tabs defaultActiveKey="info">
        <TabPane tab="基本信息" key="info">
          <Row gutter={24}>
            <Col span={16}>
              <ProCard title="租户信息" className={styles.card}>
                <Form form={form} layout="vertical" name="tenantSettingsForm">
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                        name="name"
                        label="租户名称"
                        rules={[{ required: true, message: '请输入租户名称' }]}
                      >
                        <Input placeholder="请输入租户名称" />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item name="code" label="租户代码">
                        <Input disabled placeholder="租户代码" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item name="description" label="描述">
                    <TextArea rows={3} placeholder="请输入租户描述" />
                  </Form.Item>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name="contactName" label="联系人">
                        <Input prefix={<UserOutlined />} placeholder="联系人姓名" />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item
                        name="contactEmail"
                        label="联系邮箱"
                        rules={[{ type: 'email', message: '请输入有效邮箱' }]}
                      >
                        <Input prefix={<MailOutlined />} placeholder="联系邮箱" />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name="contactPhone" label="联系电话">
                        <Input prefix={<PhoneOutlined />} placeholder="联系电话" />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item>
                    <Button type="primary" onClick={handleSave} loading={saving}>
                      保存修改
                    </Button>
                  </Form.Item>
                </Form>
              </ProCard>
            </Col>
            <Col span={8}>
              <ProCard title="租户状态" className={styles.card}>
                <div className={styles.tenantStatus}>
                  <Avatar size={64} src={tenant?.logoUrl} icon={<TeamOutlined />} />
                  <div className={styles.tenantInfo}>
                    <h3>{tenant?.name}</h3>
                    <p>代码: {tenant?.code}</p>
                    <p>
                      状态:{' '}
                      <span
                        className={
                          tenant?.status === 'ACTIVE' ? styles.statusActive : styles.statusInactive
                        }
                      >
                        {tenant?.status === 'ACTIVE' ? '正常' : '已暂停'}
                      </span>
                    </p>
                  </div>
                </div>
                {subscription && (
                  <div className={styles.subscriptionInfo}>
                    <Descriptions column={1} size="small">
                      <Descriptions.Item label="订阅计划">
                        {subscription.planName || '未知计划'}
                      </Descriptions.Item>
                      <Descriptions.Item label="订阅状态">
                        <Tag color={subscription.status === 'ACTIVE' ? 'green' : 'orange'}>
                          {subscription.status === 'ACTIVE' ? '生效中' : subscription.status === 'CANCELLED' ? '已取消' : subscription.status}
                        </Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="计费周期">
                        {subscription.billingCycle === 'MONTHLY' ? '月付' : '年付'}
                      </Descriptions.Item>
                      <Descriptions.Item label="到期日期">
                        {subscription.endDate || '-'}
                      </Descriptions.Item>
                    </Descriptions>
                    {subscription.status === 'ACTIVE' && (
                      <Space style={{ marginTop: 16 }}>
                        <Button
                          type="primary"
                          icon={<SwapOutlined />}
                          onClick={handleOpenChangePlan}
                        >
                          更改计划
                        </Button>
                        <Popconfirm
                          title="确认取消订阅"
                          description="取消订阅后，您将无法使用付费功能。确定要取消吗？"
                          onConfirm={handleCancelSubscription}
                          okText="确认取消"
                          cancelText="暂不取消"
                          okButtonProps={{ danger: true }}
                        >
                          <Button danger icon={<CloseCircleOutlined />}>
                            取消订阅
                          </Button>
                        </Popconfirm>
                      </Space>
                    )}
                  </div>
                )}
                {!subscription && (
                  <div style={{ textAlign: 'center', padding: '20px 0' }}>
                    <p style={{ color: '#999' }}>暂无订阅</p>
                    <Button type="primary" onClick={handleOpenChangePlan}>
                      选择订阅计划
                    </Button>
                  </div>
                )}
              </ProCard>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="使用量统计" key="usage">
          <Row gutter={24}>
            <Col span={24}>
              <ProCard title="今日使用量" className={styles.card}>
                <Row gutter={16}>
                  <Col span={6}>
                    <Statistic
                      title="API 调用次数"
                      value={usage?.apiCalls || 0}
                      prefix={<ApiOutlined />}
                      suffix={currentPlan ? `/ ${formatNumber(currentPlan.maxApiCallsPerDay)}` : ''}
                    />
                    {currentPlan && currentPlan.maxApiCallsPerDay > 0 && (
                      <Progress
                        percent={getUsagePercent(
                          usage?.apiCalls || 0,
                          currentPlan.maxApiCallsPerDay,
                        )}
                        strokeColor={getStatusColor(
                          getUsagePercent(usage?.apiCalls || 0, currentPlan.maxApiCallsPerDay),
                        )}
                        size="small"
                      />
                    )}
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="Token 消耗"
                      value={formatNumber(usage?.tokensUsed || 0)}
                      prefix={<CloudOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="查询次数"
                      value={usage?.queryCount || 0}
                      prefix={<DatabaseOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="活跃用户"
                      value={usage?.activeUsers || 0}
                      prefix={<TeamOutlined />}
                      suffix={currentPlan ? `/ ${currentPlan.maxUsers === -1 ? '不限' : currentPlan.maxUsers}` : ''}
                    />
                  </Col>
                </Row>
              </ProCard>
            </Col>
          </Row>

          <Row gutter={24} style={{ marginTop: 16 }}>
            <Col span={24}>
              <ProCard title="本月使用量" className={styles.card}>
                <Row gutter={16}>
                  <Col span={6}>
                    <Statistic
                      title="Token 消耗"
                      value={formatNumber(monthlyUsage?.tokensUsed || 0)}
                      prefix={<CloudOutlined />}
                      suffix={currentPlan ? `/ ${formatNumber(currentPlan.maxTokensPerMonth || 0)}` : ''}
                    />
                    {currentPlan && currentPlan.maxTokensPerMonth && currentPlan.maxTokensPerMonth > 0 && (
                      <Progress
                        percent={getUsagePercent(
                          monthlyUsage?.tokensUsed || 0,
                          currentPlan.maxTokensPerMonth,
                        )}
                        strokeColor={getStatusColor(
                          getUsagePercent(monthlyUsage?.tokensUsed || 0, currentPlan.maxTokensPerMonth),
                        )}
                        size="small"
                      />
                    )}
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="累计 API 调用"
                      value={formatNumber(monthlyUsage?.apiCalls || 0)}
                      prefix={<ApiOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="累计查询"
                      value={formatNumber(monthlyUsage?.queryCount || 0)}
                      prefix={<DatabaseOutlined />}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="存储使用"
                      value={formatBytes(monthlyUsage?.storageBytes || 0)}
                      prefix={<CloudOutlined />}
                    />
                  </Col>
                </Row>
              </ProCard>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="资源配额" key="quota">
          {currentPlan ? (
            <ProCard title={`资源限额（来自订阅计划: ${currentPlan.name}）`} className={styles.card}>
              <Row gutter={[24, 24]}>
                <Col span={8}>
                  <Card size="small">
                    <Statistic
                      title="最大用户数"
                      value={currentPlan.maxUsers === -1 ? '不限' : currentPlan.maxUsers}
                      prefix={<TeamOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card size="small">
                    <Statistic
                      title="最大数据集数"
                      value={currentPlan.maxDatasets === -1 ? '不限' : currentPlan.maxDatasets}
                      prefix={<DatabaseOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card size="small">
                    <Statistic
                      title="最大模型数"
                      value={currentPlan.maxModels === -1 ? '不限' : currentPlan.maxModels}
                      prefix={<RobotOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card size="small">
                    <Statistic
                      title="最大Agent数"
                      value={currentPlan.maxAgents === -1 ? '不限' : currentPlan.maxAgents}
                      prefix={<RobotOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card size="small">
                    <Statistic
                      title="每日API调用上限"
                      value={currentPlan.maxApiCallsPerDay === -1 ? '不限' : formatNumber(currentPlan.maxApiCallsPerDay)}
                      prefix={<ApiOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card size="small">
                    <Statistic
                      title="每月Token上限"
                      value={currentPlan.maxTokensPerMonth === -1 ? '不限' : formatNumber(currentPlan.maxTokensPerMonth || 0)}
                      prefix={<CloudOutlined />}
                    />
                  </Card>
                </Col>
              </Row>
            </ProCard>
          ) : (
            <ProCard title="资源限额" className={styles.card}>
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                暂无订阅计划，请先选择订阅计划以查看资源配额。
              </div>
            </ProCard>
          )}
        </TabPane>
      </Tabs>

      <Modal
        title="选择订阅计划"
        open={changePlanVisible}
        onOk={handleChangePlan}
        onCancel={() => setChangePlanVisible(false)}
        confirmLoading={changingPlan}
        okText="确认更改"
        cancelText="取消"
        width={700}
      >
        <div style={{ marginBottom: 16 }}>
          <p style={{ color: '#666' }}>请选择您需要的订阅计划，更改后新计划将立即生效。</p>
        </div>
        <Radio.Group
          value={selectedPlanId}
          onChange={(e) => setSelectedPlanId(e.target.value)}
          style={{ width: '100%' }}
        >
          <Row gutter={[16, 16]}>
            {plans.map((plan) => (
              <Col span={12} key={plan.id}>
                <Radio.Button
                  value={plan.id}
                  style={{
                    width: '100%',
                    height: 'auto',
                    padding: 16,
                    textAlign: 'left',
                    display: 'block',
                    borderRadius: 8,
                  }}
                >
                  <div>
                    <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 8 }}>
                      {plan.name}
                      {plan.id === subscription?.planId && (
                        <Tag color="blue" style={{ marginLeft: 8 }}>当前</Tag>
                      )}
                    </div>
                    <div style={{ color: '#1890ff', fontSize: 18, fontWeight: 600 }}>
                      {formatPrice(plan.priceMonthly)}<span style={{ fontSize: 12, fontWeight: 400 }}>/月</span>
                    </div>
                    <div style={{ color: '#999', fontSize: 12, marginTop: 8 }}>
                      <div>最多 {plan.maxUsers} 用户</div>
                      <div>最多 {plan.maxDatasets} 数据集</div>
                      <div>每月 {formatNumber(plan.maxTokensPerMonth || 0)} Token</div>
                    </div>
                  </div>
                </Radio.Button>
              </Col>
            ))}
          </Row>
        </Radio.Group>
      </Modal>
    </div>
  );
};

export default TenantSettings;
