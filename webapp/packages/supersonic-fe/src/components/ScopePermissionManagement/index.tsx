import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  message,
  Space,
  Popconfirm,
  Card,
  Tag,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import styles from './style.less';

interface Permission {
  id: number;
  code: string;
  name: string;
  description?: string;
  type: string;
  scope: string;
  createdAt: string;
}

const PERMISSION_TYPES = [
  { value: 'MENU', label: '菜单权限' },
  { value: 'BUTTON', label: '按钮权限' },
  { value: 'DATA', label: '数据权限' },
  { value: 'API', label: 'API权限' },
];

interface ScopeConfig {
  scope: 'PLATFORM' | 'TENANT';
  title: string;
  scopeTag: { color: string; text: string };
  codePlaceholder: string;
  namePlaceholder: string;
  api: {
    getList: () => Promise<any>;
    create: (data: any) => Promise<any>;
    update: (id: number, data: any) => Promise<any>;
    delete: (id: number) => Promise<any>;
  };
}

const ScopePermissionManagement: React.FC<ScopeConfig> = ({
  scope,
  title,
  scopeTag,
  codePlaceholder,
  namePlaceholder,
  api,
}) => {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingPerm, setEditingPerm] = useState<Permission | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    loadPermissions();
  }, []);

  const loadPermissions = async () => {
    setLoading(true);
    try {
      const { code, data } = await api.getList();
      if (code === 200 && data) {
        setPermissions(data);
      }
    } catch (error) {
      message.error('加载权限列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingPerm(null);
    form.resetFields();
    form.setFieldsValue({ type: 'MENU', scope });
    setModalVisible(true);
  };

  const handleEdit = (perm: Permission) => {
    setEditingPerm(perm);
    form.setFieldsValue(perm);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const { code } = await api.delete(id);
      if (code === 200) {
        message.success('删除成功');
        loadPermissions();
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const data = { ...values, scope };
      let result;
      if (editingPerm) {
        result = await api.update(editingPerm.id, data);
      } else {
        result = await api.create(data);
      }

      if (result.code === 200) {
        message.success(editingPerm ? '更新成功' : '创建成功');
        setModalVisible(false);
        loadPermissions();
      } else {
        message.error(result.msg || '操作失败');
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
      title: '权限编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '权限名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => {
        const typeInfo = PERMISSION_TYPES.find((t) => t.value === type);
        return <Tag>{typeInfo?.label || type}</Tag>;
      },
    },
    {
      title: '作用域',
      dataIndex: 'scope',
      key: 'scope',
      render: () => <Tag color={scopeTag.color}>{scopeTag.text}</Tag>,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: Permission) => (
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
            title="确认删除该权限?"
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
        title={title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新建权限
          </Button>
        }
      >
        <Table
          dataSource={permissions}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingPerm ? '编辑权限' : '新建权限'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" name={`${scope.toLowerCase()}PermissionForm`}>
          <Form.Item
            name="code"
            label="权限编码"
            rules={[{ required: true, message: '请输入权限编码' }]}
          >
            <Input placeholder={codePlaceholder} disabled={!!editingPerm} />
          </Form.Item>
          <Form.Item
            name="name"
            label="权限名称"
            rules={[{ required: true, message: '请输入权限名称' }]}
          >
            <Input placeholder={namePlaceholder} />
          </Form.Item>
          <Form.Item name="type" label="权限类型">
            <Select options={PERMISSION_TYPES} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ScopePermissionManagement;
