import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  message,
  Space,
  Popconfirm,
  Card,
  Tag,
  Select,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  getSubscriptionPlans,
  createSubscriptionPlan,
  updateSubscriptionPlan,
  deleteSubscriptionPlan,
} from '@/services/platform';
import { MSG } from '@/common/messages';
import styles from './style.less';

interface SubscriptionPlan {
  id: number;
  code: string;
  name: string;
  description?: string;
  maxUsers: number;
  maxApiCallsPerDay: number;
  maxDatasets: number;
  maxModels: number;
  maxAgents: number;
  priceMonthly: number;
  priceYearly: number;
  status: string;
  createdAt: string;
}

const SubscriptionManagement: React.FC = () => {
  const [plans, setPlans] = useState<SubscriptionPlan[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingPlan, setEditingPlan] = useState<SubscriptionPlan | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    setLoading(true);
    try {
      const { code, data } = await getSubscriptionPlans();
      if (code === 200 && data) {
        setPlans(data);
      }
    } catch (error) {
      message.error('加载订阅计划失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingPlan(null);
    form.resetFields();
    form.setFieldsValue({
      status: 'ACTIVE',
      maxUsers: 10,
      maxApiCallsPerDay: 1000,
      maxDatasets: 5,
      maxModels: 10,
      maxAgents: 5,
      priceMonthly: 0,
      priceYearly: 0,
    });
    setModalVisible(true);
  };

  const handleEdit = (plan: SubscriptionPlan) => {
    setEditingPlan(plan);
    form.setFieldsValue(plan);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const { code } = await deleteSubscriptionPlan(id);
      if (code === 200) {
        message.success(MSG.DELETE_SUCCESS);
        loadPlans();
      }
    } catch (error) {
      message.error(MSG.DELETE_FAILED);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      let result;
      if (editingPlan) {
        result = await updateSubscriptionPlan(editingPlan.id, values);
      } else {
        result = await createSubscriptionPlan(values);
      }

      if (result.code === 200) {
        message.success(editingPlan ? MSG.UPDATE_SUCCESS : MSG.CREATE_SUCCESS);
        setModalVisible(false);
        loadPlans();
      } else {
        message.error(result.msg || MSG.OPERATION_FAILED);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '计划标识',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '计划名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '最大用户数',
      dataIndex: 'maxUsers',
      key: 'maxUsers',
      render: (val: number) => (val === -1 ? '不限' : val),
    },
    {
      title: '每日API调用',
      dataIndex: 'maxApiCallsPerDay',
      key: 'maxApiCallsPerDay',
      render: (val: number) => (val === -1 ? '不限' : val),
    },
    {
      title: '月价格',
      dataIndex: 'priceMonthly',
      key: 'priceMonthly',
      render: (price: number) => (price === 0 ? '免费' : `¥${price ?? 0}/月`),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'ACTIVE' ? 'green' : 'red'}>
          {status === 'ACTIVE' ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: SubscriptionPlan) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除该订阅计划?"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <Card
        title="订阅计划管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建计划
          </Button>
        }
      >
        <Table
          dataSource={plans}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingPlan ? '编辑订阅计划' : '新建订阅计划'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
        width={600}
      >
        <Form form={form} layout="vertical" name="subscriptionPlanForm">
          <Form.Item
            name="code"
            label="计划标识"
            rules={[{ required: true, message: '请输入计划标识' }]}
          >
            <Input placeholder="如: FREE, BASIC, PRO, ENTERPRISE" disabled={!!editingPlan} />
          </Form.Item>
          <Form.Item
            name="name"
            label="计划名称"
            rules={[{ required: true, message: '请输入计划名称' }]}
          >
            <Input placeholder="如: 免费版, 基础版, 专业版, 企业版" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={2} />
          </Form.Item>
          <Space size="large">
            <Form.Item name="maxUsers" label="最大用户数" tooltip="-1表示不限">
              <InputNumber min={-1} placeholder="10" />
            </Form.Item>
            <Form.Item name="maxApiCallsPerDay" label="每日API调用" tooltip="-1表示不限">
              <InputNumber min={-1} placeholder="1000" />
            </Form.Item>
          </Space>
          <Space size="large">
            <Form.Item name="maxDatasets" label="最大数据集数" tooltip="-1表示不限">
              <InputNumber min={-1} placeholder="5" />
            </Form.Item>
            <Form.Item name="maxModels" label="最大模型数" tooltip="-1表示不限">
              <InputNumber min={-1} placeholder="10" />
            </Form.Item>
          </Space>
          <Space size="large">
            <Form.Item name="maxAgents" label="最大Agent数" tooltip="-1表示不限">
              <InputNumber min={-1} placeholder="5" />
            </Form.Item>
          </Space>
          <Space size="large">
            <Form.Item name="priceMonthly" label="月价格 (¥)">
              <InputNumber min={0} precision={2} placeholder="0.00" style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="priceYearly" label="年价格 (¥)">
              <InputNumber min={0} precision={2} placeholder="0.00" style={{ width: 150 }} />
            </Form.Item>
          </Space>
          <Form.Item name="status" label="状态">
            <Select style={{ width: 120 }}>
              <Select.Option value="ACTIVE">启用</Select.Option>
              <Select.Option value="INACTIVE">禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SubscriptionManagement;
