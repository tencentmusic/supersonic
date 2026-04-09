import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  Select,
  message,
  Space,
  Card,
  Tag,
  TreeSelect,
} from 'antd';

import { getUserList } from '@/services/user';
import { getOrganizationTree, assignUserToOrganization } from '@/services/organization';
import { getTenantRoles, assignRoleToUser } from '@/services/tenant';
import { StatusEnum } from '@/common/constants';
import styles from './style.less';

interface Member {
  id: number;
  name: string;
  displayName: string;
  email?: string;
  organizationId?: number;
  organizationName?: string;
  roleIds?: number[];
  roleNames?: string[];
  status: number;
  createdAt: string;
}

const MemberManagement: React.FC = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [organizations, setOrganizations] = useState<any[]>([]);
  const [roles, setRoles] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [roleModalVisible, setRoleModalVisible] = useState(false);
  const [editingMember, setEditingMember] = useState<Member | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<number[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    loadMembers();
    loadOrganizations();
    loadRoles();
  }, []);

  const loadMembers = async () => {
    setLoading(true);
    try {
      const { code, data } = await getUserList();
      if (code === 200 && data) {
        setMembers(data);
      }
    } catch (error) {
      message.error('加载成员列表失败');
    } finally {
      setLoading(false);
    }
  };

  const loadOrganizations = async () => {
    try {
      const { code, data } = await getOrganizationTree();
      if (code === 200 && data) {
        setOrganizations(convertToTreeSelect(data));
      }
    } catch (error) {
      console.error('加载组织架构失败', error);
    }
  };

  const loadRoles = async () => {
    try {
      const { code, data } = await getTenantRoles();
      if (code === 200 && data) {
        setRoles(data);
      }
    } catch (error) {
      console.error('加载角色列表失败', error);
    }
  };

  const convertToTreeSelect = (orgs: any[]): any[] => {
    return orgs.map((org) => ({
      value: org.id,
      title: org.name,
      children: org.subOrganizations ? convertToTreeSelect(org.subOrganizations) : [],
    }));
  };

  const handleAssignOrg = (member: Member) => {
    setEditingMember(member);
    form.resetFields();
    // Pre-select current organization if exists
    if (member.organizationId) {
      form.setFieldsValue({ organizationId: member.organizationId });
    }
    setModalVisible(true);
  };

  const handleAssignRole = (member: Member) => {
    setEditingMember(member);
    // Pre-select current roles if exists
    // 只显示在租户角色列表中存在的角色，过滤掉平台级角色
    if (member.roleIds && member.roleIds.length > 0) {
      const tenantRoleIds = roles.map((r) => r.id);
      const filteredRoleIds = member.roleIds.filter((id) => tenantRoleIds.includes(id));
      setSelectedRoles(filteredRoleIds);
    } else {
      setSelectedRoles([]);
    }
    setRoleModalVisible(true);
  };

  const handleSubmitOrg = async () => {
    if (!editingMember) return;
    try {
      const values = await form.validateFields();
      setLoading(true);
      const { code } = await assignUserToOrganization({
        userId: editingMember.id,
        organizationId: values.organizationId,
        isPrimary: true,
      });
      if (code === 200) {
        message.success('分配成功');
        setModalVisible(false);
        loadMembers();
      }
    } catch (error) {
      message.error('分配失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitRole = async () => {
    if (!editingMember || selectedRoles.length === 0) return;
    try {
      setLoading(true);
      const { code } = await assignRoleToUser({
        userId: editingMember.id,
        roleIds: selectedRoles,
      });
      if (code === 200) {
        message.success('角色分配成功');
        setRoleModalVisible(false);
        loadMembers();
      }
    } catch (error) {
      message.error('角色分配失败');
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
      title: '用户名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '显示名',
      dataIndex: 'displayName',
      key: 'displayName',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '所属组织',
      dataIndex: 'organizationName',
      key: 'organizationName',
      render: (text: string) => text || '-',
    },
    {
      title: '角色',
      dataIndex: 'roleNames',
      key: 'roleNames',
      render: (roleNames: string[]) =>
        roleNames?.length > 0 ? roleNames.map((r) => <Tag key={r}>{r}</Tag>) : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === StatusEnum.ENABLED ? 'green' : 'red'}>
          {status === StatusEnum.ENABLED ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: any, record: Member) => (
        <Space>
          <Button
            type="link"
            size="small"
            onClick={() => handleAssignOrg(record)}
          >
            分配组织
          </Button>
          <Button
            type="link"
            size="small"
            onClick={() => handleAssignRole(record)}
          >
            分配角色
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <Card title="成员管理">
        <Table
          dataSource={members}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title={`分配组织 - ${editingMember?.displayName || editingMember?.name}`}
        open={modalVisible}
        onOk={handleSubmitOrg}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" name="memberForm">
          <Form.Item
            name="organizationId"
            label="选择组织"
            rules={[{ required: true, message: '请选择组织' }]}
          >
            <TreeSelect
              treeData={organizations}
              placeholder="请选择组织"
              treeDefaultExpandAll
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`分配角色 - ${editingMember?.displayName || editingMember?.name}`}
        open={roleModalVisible}
        onOk={handleSubmitRole}
        onCancel={() => setRoleModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
      >
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder="请选择角色"
          value={selectedRoles}
          onChange={setSelectedRoles}
        >
          {roles.map((role) => (
            <Select.Option key={role.id} value={role.id}>
              {role.displayName || role.name}
            </Select.Option>
          ))}
        </Select>
      </Modal>
    </div>
  );
};

export default MemberManagement;
