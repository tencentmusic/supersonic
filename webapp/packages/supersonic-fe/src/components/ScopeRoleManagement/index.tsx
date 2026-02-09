import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  message,
  Space,
  Popconfirm,
  Card,
  Tag,
  Transfer,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons';
import styles from './style.less';

interface Role {
  id: number;
  code: string;
  name: string;
  description?: string;
  scope: string;
  isSystem?: boolean;
  status?: boolean;
  createdAt?: string;
  createdBy?: string;
}

interface Permission {
  id: number;
  code: string;
  name: string;
  description?: string;
}

interface ScopeConfig {
  scope: 'PLATFORM' | 'TENANT';
  title: string;
  scopeTag: { color: string; text: string };
  codePlaceholder: string;
  namePlaceholder: string;
  api: {
    getRoles: () => Promise<any>;
    createRole: (data: any) => Promise<any>;
    updateRole: (id: number, data: any) => Promise<any>;
    deleteRole: (id: number) => Promise<any>;
    getPermissions: () => Promise<any>;
    assignPermissions: (roleId: number, permissionIds: number[]) => Promise<any>;
    getRolePermissionIds: (roleId: number) => Promise<any>;
  };
}

const ScopeRoleManagement: React.FC<ScopeConfig> = ({
  scope,
  title,
  scopeTag,
  codePlaceholder,
  namePlaceholder,
  api,
}) => {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [permModalVisible, setPermModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [selectedPermKeys, setSelectedPermKeys] = useState<string[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    loadRoles();
    loadPermissions();
  }, []);

  const loadRoles = async () => {
    setLoading(true);
    try {
      const { code, data } = await api.getRoles();
      if (code === 200 && data) {
        setRoles(data);
      }
    } catch (error) {
      message.error('加载角色列表失败');
    } finally {
      setLoading(false);
    }
  };

  const loadPermissions = async () => {
    try {
      const { code, data } = await api.getPermissions();
      if (code === 200 && data) {
        setPermissions(data);
      }
    } catch (error) {
      console.error('加载权限列表失败', error);
    }
  };

  const handleAdd = () => {
    setEditingRole(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (role: Role) => {
    setEditingRole(role);
    form.setFieldsValue(role);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      const { code } = await api.deleteRole(id);
      if (code === 200) {
        message.success('删除成功');
        loadRoles();
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
      if (editingRole) {
        result = await api.updateRole(editingRole.id, data);
      } else {
        result = await api.createRole(data);
      }

      if (result.code === 200) {
        message.success(editingRole ? '更新成功' : '创建成功');
        setModalVisible(false);
        loadRoles();
      } else {
        message.error(result.msg || '操作失败');
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleConfigPermissions = async (role: Role) => {
    setEditingRole(role);
    setLoading(true);
    try {
      const { code, data } = await api.getRolePermissionIds(role.id);
      if (code === 200 && data) {
        setSelectedPermKeys(data.map(String));
      } else {
        setSelectedPermKeys([]);
      }
    } catch (error) {
      console.error('加载角色权限失败', error);
      setSelectedPermKeys([]);
    } finally {
      setLoading(false);
    }
    setPermModalVisible(true);
  };

  const handleSavePermissions = async () => {
    if (!editingRole) return;
    try {
      setLoading(true);
      const { code } = await api.assignPermissions(editingRole.id, selectedPermKeys.map(Number));
      if (code === 200) {
        message.success('权限配置成功');
        setPermModalVisible(false);
      }
    } catch (error) {
      message.error('权限配置失败');
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
      title: '角色编码',
      dataIndex: 'code',
      key: 'code',
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '作用域',
      dataIndex: 'scope',
      key: 'scope',
      render: () => <Tag color={scopeTag.color}>{scopeTag.text}</Tag>,
    },
    {
      title: '类型',
      dataIndex: 'isSystem',
      key: 'isSystem',
      render: (isSystem: boolean) => isSystem ? <Tag color="blue">系统</Tag> : <Tag color="green">自定义</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: boolean) => status ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: Role) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<SettingOutlined />}
            onClick={() => handleConfigPermissions(record)}
          >
            权限
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除该角色?"
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
            新建角色
          </Button>
        }
      >
        <Table
          dataSource={roles}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={editingRole ? '编辑角色' : '新建角色'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" name={`${scope.toLowerCase()}RoleForm`} initialValues={{ status: true }}>
          <Form.Item
            name="code"
            label="角色编码"
            rules={[{ required: true, message: '请输入角色编码' }]}
          >
            <Input placeholder={codePlaceholder} disabled={!!editingRole} />
          </Form.Item>
          <Form.Item
            name="name"
            label="角色名称"
            rules={[{ required: true, message: '请输入角色名称' }]}
          >
            <Input placeholder={namePlaceholder} />
          </Form.Item>
          <Form.Item label="作用域">
            <Select value={scope} disabled>
              <Select.Option value={scope}>{scopeTag.text}</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="isSystem" label="类型" valuePropName="checked">
            <Switch
              checkedChildren="系统"
              unCheckedChildren="自定义"
              disabled={editingRole?.isSystem}
            />
          </Form.Item>
          <Form.Item name="status" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`配置权限 - ${editingRole?.name}`}
        open={permModalVisible}
        onOk={handleSavePermissions}
        onCancel={() => setPermModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
        width={600}
      >
        <Transfer
          dataSource={permissions.map((p) => ({ key: String(p.id), title: p.name, description: p.description }))}
          titles={['可选权限', '已选权限']}
          targetKeys={selectedPermKeys}
          onChange={setSelectedPermKeys}
          render={(item) => item.title}
          listStyle={{ width: 250, height: 300 }}
        />
      </Modal>
    </div>
  );
};

export default ScopeRoleManagement;
