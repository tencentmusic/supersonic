import React, { useState, useEffect, useRef } from 'react';
import {
  Button,
  message,
  Modal,
  Form,
  Input,
  Tag,
  Popconfirm,
  Drawer,
  Descriptions,
  Tabs,
  Row,
  Col,
  Select,
  Radio,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  StopOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  GiftOutlined,
} from '@ant-design/icons';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import {
  getAllTenants,
  createTenant,
  updateTenant,
  deleteTenant,
  suspendTenant,
  activateTenant,
  Tenant,
} from '@/services/tenant';
import {
  getSubscriptionPlans,
  getTenantSubscription,
  updateTenantSubscription,
} from '@/services/subscription';
import type { SubscriptionPlan, TenantSubscription } from '@/services/tenant';
import dayjs from 'dayjs';
import { MSG } from '@/common/messages';
import styles from './style.less';

const { TextArea } = Input;
const { TabPane } = Tabs;

const AdminTenant: React.FC = () => {
  const actionRef = useRef<ActionType>();
  const [modalVisible, setModalVisible] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [editingTenant, setEditingTenant] = useState<Tenant | null>(null);
  const [viewingTenant, setViewingTenant] = useState<Tenant | null>(null);
  const [form] = Form.useForm();
  const [subscriptionModalVisible, setSubscriptionModalVisible] = useState(false);
  const [subscriptionForm] = Form.useForm();
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [assigningTenant, setAssigningTenant] = useState<Tenant | null>(null);
  const [assigningSubscription, setAssigningSubscription] = useState(false);
  const [viewingSubscription, setViewingSubscription] = useState<TenantSubscription | null>(null);

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    const res = await getSubscriptionPlans();
    if (res.code === 200 && res.data) {
      setPlans(res.data);
    }
  };

  const loadTenants = async () => {
    const res = await getAllTenants();
    if (res.code === 200) {
      return {
        data: res.data || [],
        success: true,
      };
    }
    return {
      data: [],
      success: false,
    };
  };

  const handleCreate = () => {
    setEditingTenant(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: Tenant) => {
    setEditingTenant(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleView = async (record: Tenant) => {
    setViewingTenant(record);
    setViewingSubscription(null);
    setDrawerVisible(true);

    // Load subscription details for the viewed tenant
    try {
      const res = await getTenantSubscription(record.id);
      if (res.code === 200 && res.data) {
        setViewingSubscription(res.data);
      }
    } catch {
      // No subscription
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      let res;
      if (editingTenant) {
        res = await updateTenant(editingTenant.id, values);
      } else {
        res = await createTenant(values);
      }

      if (res.code === 200) {
        message.success(editingTenant ? MSG.UPDATE_SUCCESS : MSG.CREATE_SUCCESS);
        setModalVisible(false);
        actionRef.current?.reload();
      } else {
        message.error(res.msg || MSG.OPERATION_FAILED);
      }
    } catch (error) {
      message.error(MSG.OPERATION_FAILED);
    }
  };

  const handleDelete = async (id: number) => {
    const res = await deleteTenant(id);
    if (res.code === 200) {
      message.success(MSG.DELETE_SUCCESS);
      actionRef.current?.reload();
    } else {
      message.error(res.msg || MSG.DELETE_FAILED);
    }
  };

  const handleSuspend = async (id: number) => {
    const res = await suspendTenant(id);
    if (res.code === 200) {
      message.success('已暂停租户');
      actionRef.current?.reload();
    } else {
      message.error(res.msg || MSG.OPERATION_FAILED);
    }
  };

  const handleActivate = async (id: number) => {
    const res = await activateTenant(id);
    if (res.code === 200) {
      message.success('已激活租户');
      actionRef.current?.reload();
    } else {
      message.error(res.msg || MSG.OPERATION_FAILED);
    }
  };

  const handleAssignSubscription = async (record: Tenant) => {
    setAssigningTenant(record);
    subscriptionForm.resetFields();
    subscriptionForm.setFieldsValue({ billingCycle: 'MONTHLY' });

    // Load existing subscription for this tenant
    try {
      const res = await getTenantSubscription(record.id);
      if (res.code === 200 && res.data) {
        subscriptionForm.setFieldsValue({
          planId: res.data.planId,
          billingCycle: res.data.billingCycle || 'MONTHLY',
        });
      }
    } catch (error) {
      // No existing subscription, that's fine
    }

    setSubscriptionModalVisible(true);
  };

  const handleSubmitSubscription = async () => {
    if (!assigningTenant) return;

    try {
      const values = await subscriptionForm.validateFields();
      setAssigningSubscription(true);

      const res = await updateTenantSubscription(assigningTenant.id, {
        planId: values.planId,
        billingCycle: values.billingCycle,
      });

      if (res.code === 200) {
        message.success('订阅分配成功');
        setSubscriptionModalVisible(false);
        actionRef.current?.reload();
      } else {
        message.error(res.msg || '订阅分配失败');
      }
    } catch (error) {
      message.error('订阅分配失败');
    } finally {
      setAssigningSubscription(false);
    }
  };

  const getPlanById = (planId?: number): SubscriptionPlan | undefined => {
    if (!planId) return undefined;
    return plans.find((p) => p.id === planId);
  };

  const formatLimit = (val?: number) => {
    if (val === undefined || val === null) return '-';
    if (val === -1) return '不限';
    if (val >= 1000000) return (val / 1000000).toFixed(1) + 'M';
    if (val >= 1000) return (val / 1000).toFixed(1) + 'K';
    return val.toString();
  };

  const columns: ProColumns<Tenant>[] = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 60,
    },
    {
      title: '租户名称',
      dataIndex: 'name',
      width: 120,
      ellipsis: true,
    },
    {
      title: '租户代码',
      dataIndex: 'code',
      width: 120,
      copyable: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (_, record) => {
        const statusMap: Record<string, { color: string; text: string }> = {
          ACTIVE: { color: 'green', text: '正常' },
          SUSPENDED: { color: 'orange', text: '已暂停' },
          DELETED: { color: 'red', text: '已删除' },
        };
        const status = statusMap[record.status] || { color: 'default', text: record.status };
        return <Tag color={status.color}>{status.text}</Tag>;
      },
    },
    {
      title: '联系人',
      dataIndex: 'contactName',
      width: 100,
      ellipsis: true,
    },
    {
      title: '联系邮箱',
      dataIndex: 'contactEmail',
      width: 160,
      ellipsis: true,
    },
    {
      title: '订阅计划',
      dataIndex: 'subscriptionPlanName',
      width: 100,
      render: (_: any, record: Tenant) => {
        return record.subscriptionPlanName ? (
          <Tag color="blue">{record.subscriptionPlanName}</Tag>
        ) : (
          <Tag color="default">未订阅</Tag>
        );
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      valueType: 'dateTime',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 300,
      fixed: 'right',
      render: (_, record) => [
        <Button
          key="view"
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleView(record)}
        >
          查看
        </Button>,
        <Button
          key="edit"
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={() => handleEdit(record)}
        >
          编辑
        </Button>,
        <Button
          key="subscription"
          type="link"
          size="small"
          icon={<GiftOutlined />}
          onClick={() => handleAssignSubscription(record)}
        >
          分配订阅
        </Button>,
        record.status === 'ACTIVE' ? (
          <Popconfirm
            key="suspend"
            title="确定要暂停该租户吗？"
            onConfirm={() => handleSuspend(record.id)}
          >
            <Button type="link" size="small" icon={<StopOutlined />} danger>
              暂停
            </Button>
          </Popconfirm>
        ) : record.status === 'SUSPENDED' ? (
          <Popconfirm
            key="activate"
            title="确定要激活该租户吗？"
            onConfirm={() => handleActivate(record.id)}
          >
            <Button type="link" size="small" icon={<CheckCircleOutlined />}>
              激活
            </Button>
          </Popconfirm>
        ) : null,
        <Popconfirm
          key="delete"
          title="确定要删除该租户吗？此操作不可恢复！"
          onConfirm={() => handleDelete(record.id)}
        >
          <Button type="link" size="small" icon={<DeleteOutlined />} danger>
            删除
          </Button>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <div className={styles.container}>
      <ProTable<Tenant>
        headerTitle="租户管理"
        actionRef={actionRef}
        rowKey="id"
        search={false}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            新建租户
          </Button>,
        ]}
        request={loadTenants}
        columns={columns}
        scroll={{ x: 1200 }}
        pagination={{
          pageSize: 10,
        }}
      />

      <Modal
        title={editingTenant ? '编辑租户' : '新建租户'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={700}
      >
        <Form form={form} layout="vertical" name="tenantForm">
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
              <Form.Item
                name="code"
                label="租户代码"
                rules={[
                  { required: true, message: '请输入租户代码' },
                  { pattern: /^[a-z0-9_-]+$/, message: '只能包含小写字母、数字、下划线和连字符' },
                ]}
              >
                <Input
                  placeholder="请输入租户代码"
                  disabled={!!editingTenant}
                  onChange={(e) => {
                    form.setFieldValue('code', e.target.value?.toLowerCase());
                  }}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <TextArea rows={2} placeholder="请输入租户描述" />
          </Form.Item>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="contactName" label="联系人">
                <Input placeholder="联系人姓名" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                name="contactEmail"
                label="联系邮箱"
                rules={[{ type: 'email', message: '请输入有效邮箱' }]}
              >
                <Input placeholder="联系邮箱" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="contactPhone" label="联系电话">
                <Input placeholder="联系电话" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Drawer
        title="租户详情"
        width={600}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      >
        {viewingTenant && (
          <Tabs defaultActiveKey="info">
            <TabPane tab="基本信息" key="info">
              <Descriptions column={2} bordered size="small">
                <Descriptions.Item label="ID">{viewingTenant.id}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag
                    color={
                      viewingTenant.status === 'ACTIVE'
                        ? 'green'
                        : viewingTenant.status === 'SUSPENDED'
                          ? 'orange'
                          : 'red'
                    }
                  >
                    {viewingTenant.status === 'ACTIVE'
                      ? '正常'
                      : viewingTenant.status === 'SUSPENDED'
                        ? '已暂停'
                        : '已删除'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="名称">{viewingTenant.name}</Descriptions.Item>
                <Descriptions.Item label="代码">{viewingTenant.code}</Descriptions.Item>
                <Descriptions.Item label="描述" span={2}>
                  {viewingTenant.description || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="联系人">{viewingTenant.contactName || '-'}</Descriptions.Item>
                <Descriptions.Item label="联系邮箱">
                  {viewingTenant.contactEmail || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="联系电话">
                  {viewingTenant.contactPhone || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">{viewingTenant.createdAt ? dayjs(viewingTenant.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-'}</Descriptions.Item>
                <Descriptions.Item label="创建人">{viewingTenant.createdBy || '-'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{viewingTenant.updatedAt ? dayjs(viewingTenant.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}</Descriptions.Item>
              </Descriptions>
            </TabPane>
            <TabPane tab="订阅与配额" key="subscription">
              {viewingSubscription ? (
                <>
                  <Descriptions column={2} bordered size="small" title="订阅信息">
                    <Descriptions.Item label="订阅计划">
                      <Tag color="blue">{viewingSubscription.planName || '未知计划'}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="计费周期">
                      {viewingSubscription.billingCycle === 'MONTHLY' ? '月付' : '年付'}
                    </Descriptions.Item>
                    <Descriptions.Item label="状态">
                      <Tag color={viewingSubscription.status === 'ACTIVE' ? 'green' : 'orange'}>
                        {viewingSubscription.status === 'ACTIVE' ? '生效中' : viewingSubscription.status}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="开始日期">
                      {viewingSubscription.startDate ? dayjs(viewingSubscription.startDate).format('YYYY-MM-DD') : '-'}
                    </Descriptions.Item>
                  </Descriptions>
                  {(() => {
                    const plan = getPlanById(viewingSubscription.planId);
                    if (!plan) return null;
                    return (
                      <Descriptions column={2} bordered size="small" title="资源配额" style={{ marginTop: 16 }}>
                        <Descriptions.Item label="最大用户数">{formatLimit(plan.maxUsers)}</Descriptions.Item>
                        <Descriptions.Item label="最大数据集数">{formatLimit(plan.maxDatasets)}</Descriptions.Item>
                        <Descriptions.Item label="最大模型数">{formatLimit(plan.maxModels)}</Descriptions.Item>
                        <Descriptions.Item label="最大Agent数">{formatLimit(plan.maxAgents)}</Descriptions.Item>
                        <Descriptions.Item label="每日API调用上限">{formatLimit(plan.maxApiCallsPerDay)}</Descriptions.Item>
                        <Descriptions.Item label="每月Token上限">{formatLimit(plan.maxTokensPerMonth)}</Descriptions.Item>
                      </Descriptions>
                    );
                  })()}
                </>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px 0' }}>
                  <p style={{ color: '#999', marginBottom: 16 }}>该租户尚未分配订阅计划，无资源配额限制。</p>
                  <Button
                    type="primary"
                    icon={<GiftOutlined />}
                    onClick={() => {
                      setDrawerVisible(false);
                      handleAssignSubscription(viewingTenant);
                    }}
                  >
                    分配订阅
                  </Button>
                </div>
              )}
            </TabPane>
          </Tabs>
        )}
      </Drawer>

      <Modal
        title={`分配订阅 - ${assigningTenant?.name || ''}`}
        open={subscriptionModalVisible}
        onOk={handleSubmitSubscription}
        onCancel={() => setSubscriptionModalVisible(false)}
        confirmLoading={assigningSubscription}
        okText="确认分配"
        cancelText="取消"
      >
        <Form form={subscriptionForm} layout="vertical" name="subscriptionForm">
          <Form.Item
            name="planId"
            label="订阅计划"
            rules={[{ required: true, message: '请选择订阅计划' }]}
          >
            <Select placeholder="请选择订阅计划">
              {plans.map((plan) => (
                <Select.Option key={plan.id} value={plan.id}>
                  {plan.name} - ¥{plan.priceMonthly}/月
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="billingCycle"
            label="计费周期"
            rules={[{ required: true, message: '请选择计费周期' }]}
          >
            <Radio.Group>
              <Radio value="MONTHLY">月付</Radio>
              <Radio value="YEARLY">年付</Radio>
            </Radio.Group>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminTenant;
