import React, { useState, useEffect } from 'react';
import {
  Tree,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  message,
  Space,
  Popconfirm,
  Card,
  Row,
  Col,
  Table,
  Tag,
  Select,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
} from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import {
  getOrganizationTree,
  createOrganization,
  updateOrganization,
  deleteOrganization,
  getUsersByOrganization,
  batchAssignUsersToOrganization,
  batchRemoveUsersFromOrganization,
  Organization,
  OrganizationReq,
} from '@/services/organization';
import { getUserList } from '@/services/user';
import { StatusEnum } from '@/common/constants';
import { MSG } from '@/common/messages';
import styles from './style.less';

interface UserInfo {
  id: number;
  name: string;
  displayName: string;
  email?: string;
}

const OrganizationManagement: React.FC = () => {
  const [treeData, setTreeData] = useState<DataNode[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [orgUsers, setOrgUsers] = useState<UserInfo[]>([]);
  const [allUsers, setAllUsers] = useState<UserInfo[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [userModalVisible, setUserModalVisible] = useState(false);
  const [editingOrg, setEditingOrg] = useState<Organization | null>(null);
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    loadOrganizations();
    loadAllUsers();
  }, []);

  const loadOrganizations = async () => {
    try {
      const { code, data } = await getOrganizationTree();
      if (code === 200 && data) {
        setOrganizations(data);
        setTreeData(convertToTreeData(data));
      }
    } catch (error) {
      message.error('加载组织架构失败');
    }
  };

  const loadAllUsers = async () => {
    try {
      const { code, data } = await getUserList();
      if (code === 200 && data) {
        setAllUsers(data);
      }
    } catch (error) {
      console.error('加载用户列表失败', error);
    }
  };

  const loadOrgUsers = async (orgId: string) => {
    try {
      const { code, data } = await getUsersByOrganization(Number(orgId));
      if (code === 200 && data) {
        // data is array of user ids, need to map to user info
        const userInfoList = data
          .map((userId: number) => allUsers.find((u) => u.id === userId))
          .filter(Boolean);
        setOrgUsers(userInfoList);
      }
    } catch (error) {
      message.error('加载组织成员失败');
    }
  };

  const convertToTreeData = (orgs: Organization[]): DataNode[] => {
    return orgs.map((org) => ({
      key: org.id,
      title: org.name,
      children: org.subOrganizations ? convertToTreeData(org.subOrganizations) : [],
    }));
  };

  const findOrgById = (orgs: Organization[], id: string): Organization | null => {
    for (const org of orgs) {
      if (org.id === id) return org;
      if (org.subOrganizations) {
        const found = findOrgById(org.subOrganizations, id);
        if (found) return found;
      }
    }
    return null;
  };

  const handleSelect = (selectedKeys: React.Key[]) => {
    if (selectedKeys.length > 0) {
      const orgId = selectedKeys[0] as string;
      const org = findOrgById(organizations, orgId);
      setSelectedOrg(org);
      if (org) {
        loadOrgUsers(orgId);
      }
    } else {
      setSelectedOrg(null);
      setOrgUsers([]);
    }
  };

  const handleAddOrg = (parentId?: string) => {
    setEditingOrg(null);
    form.resetFields();
    form.setFieldsValue({ parentId: parentId ? Number(parentId) : 0 });
    setModalVisible(true);
  };

  const handleEditOrg = (org: Organization) => {
    setEditingOrg(org);
    form.setFieldsValue({
      parentId: Number(org.parentId),
      name: org.name,
      sortOrder: 0,
      status: StatusEnum.ENABLED,
    });
    setModalVisible(true);
  };

  const handleDeleteOrg = async (orgId: string) => {
    try {
      const { code, msg } = await deleteOrganization(Number(orgId));
      if (code === 200) {
        message.success(MSG.DELETE_SUCCESS);
        loadOrganizations();
        if (selectedOrg?.id === orgId) {
          setSelectedOrg(null);
          setOrgUsers([]);
        }
      } else {
        message.error(msg || MSG.DELETE_FAILED);
      }
    } catch (error: any) {
      message.error(error?.message || MSG.DELETE_FAILED);
    }
  };

  const handleSubmitOrg = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const reqData: OrganizationReq = {
        parentId: values.parentId || 0,
        name: values.name,
        sortOrder: values.sortOrder || 0,
        status: values.status ?? StatusEnum.ENABLED,
      };

      let result;
      if (editingOrg) {
        result = await updateOrganization(Number(editingOrg.id), reqData);
      } else {
        result = await createOrganization(reqData);
      }

      if (result.code === 200) {
        message.success(editingOrg ? MSG.UPDATE_SUCCESS : MSG.CREATE_SUCCESS);
        setModalVisible(false);
        loadOrganizations();
      } else {
        message.error(result.msg || MSG.OPERATION_FAILED);
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddUsers = () => {
    setSelectedUserIds([]);
    setUserModalVisible(true);
  };

  const handleRemoveUser = async (userId: number) => {
    if (!selectedOrg) return;
    try {
      const { code } = await batchRemoveUsersFromOrganization({
        userIds: [userId],
        organizationId: Number(selectedOrg.id),
      });
      if (code === 200) {
        message.success('移除成功');
        loadOrgUsers(selectedOrg.id);
      }
    } catch (error) {
      message.error('移除失败');
    }
  };

  const handleSubmitUsers = async () => {
    if (!selectedOrg || selectedUserIds.length === 0) return;
    try {
      setLoading(true);
      const { code } = await batchAssignUsersToOrganization({
        userIds: selectedUserIds,
        organizationId: Number(selectedOrg.id),
      });
      if (code === 200) {
        message.success('添加成功');
        setUserModalVisible(false);
        loadOrgUsers(selectedOrg.id);
      }
    } catch (error) {
      message.error('添加失败');
    } finally {
      setLoading(false);
    }
  };

  const userColumns = [
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
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: UserInfo) => (
        <Popconfirm
          title="确认移除该用户?"
          onConfirm={() => handleRemoveUser(record.id)}
        >
          <Button type="link" danger icon={<UserDeleteOutlined />}>
            移除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  // Filter out users already in the organization
  const availableUsers = allUsers.filter(
    (user) => !orgUsers.find((ou) => ou.id === user.id),
  );

  return (
    <div className={styles.container}>
      <Row gutter={16}>
        <Col span={8}>
          <Card
            title="组织架构"
            extra={
              <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAddOrg()}>
                添加根组织
              </Button>
            }
          >
            <Tree
              treeData={treeData}
              onSelect={handleSelect}
              selectedKeys={selectedOrg ? [selectedOrg.id] : []}
              defaultExpandAll
              titleRender={(node) => (
                <div className={styles.treeNode}>
                  <span>{node.title as string}</span>
                  <Space className={styles.treeNodeActions} size={0}>
                    <Button
                      type="link"
                      size="small"
                      icon={<PlusOutlined />}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleAddOrg(node.key as string);
                      }}
                      title="添加子组织"
                    />
                    <Button
                      type="link"
                      size="small"
                      icon={<EditOutlined />}
                      onClick={(e) => {
                        e.stopPropagation();
                        const org = findOrgById(organizations, node.key as string);
                        if (org) handleEditOrg(org);
                      }}
                      title="编辑"
                    />
                    <Popconfirm
                      title="确认删除该组织?"
                      onConfirm={(e) => {
                        e?.stopPropagation();
                        handleDeleteOrg(node.key as string);
                      }}
                      onCancel={(e) => e?.stopPropagation()}
                    >
                      <Button
                        type="link"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={(e) => e.stopPropagation()}
                        title="删除"
                      />
                    </Popconfirm>
                  </Space>
                </div>
              )}
            />
          </Card>
        </Col>
        <Col span={16}>
          <Card
            title={
              selectedOrg ? (
                <span>
                  组织成员 <Tag color="blue">{selectedOrg.fullName || selectedOrg.name}</Tag>
                </span>
              ) : (
                '请选择组织'
              )
            }
            extra={
              selectedOrg && (
                <Button type="primary" icon={<UserAddOutlined />} onClick={handleAddUsers}>
                  添加成员
                </Button>
              )
            }
          >
            {selectedOrg ? (
              <Table
                dataSource={orgUsers}
                columns={userColumns}
                rowKey="id"
                pagination={{ pageSize: 10 }}
                locale={{ emptyText: '暂无成员' }}
              />
            ) : (
              <div className={styles.placeholder}>
                请从左侧组织架构中选择一个组织查看其成员
              </div>
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title={editingOrg ? '编辑组织' : '添加组织'}
        open={modalVisible}
        onOk={handleSubmitOrg}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" name="organizationForm">
          <Form.Item name="parentId" label="父组织ID" hidden>
            <InputNumber />
          </Form.Item>
          <Form.Item
            name="name"
            label="组织名称"
            rules={[{ required: true, message: '请输入组织名称' }]}
          >
            <Input placeholder="请输入组织名称" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} placeholder="请输入排序序号" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={StatusEnum.ENABLED}>
            <Select>
              <Select.Option value={StatusEnum.ENABLED}>启用</Select.Option>
              <Select.Option value={StatusEnum.DISABLED}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="添加成员"
        open={userModalVisible}
        onOk={handleSubmitUsers}
        onCancel={() => setUserModalVisible(false)}
        confirmLoading={loading}
        width={600}
        okText="确定"
        cancelText="取消"
      >
        <Select
          mode="multiple"
          style={{ width: '100%' }}
          placeholder="请选择要添加的用户"
          value={selectedUserIds}
          onChange={setSelectedUserIds}
          optionFilterProp="children"
          showSearch
        >
          {availableUsers.map((user) => (
            <Select.Option key={user.id} value={user.id}>
              {user.displayName || user.name} ({user.name})
            </Select.Option>
          ))}
        </Select>
      </Modal>
    </div>
  );
};

export default OrganizationManagement;
