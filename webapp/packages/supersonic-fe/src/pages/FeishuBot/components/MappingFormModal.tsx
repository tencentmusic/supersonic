import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';
import { createFeishuMapping, updateFeishuMapping } from '@/services/feishu';
import { getUserList, queryCurrentUser } from '@/services/user';
import { getAgentList } from '@/pages/Agent/service';

interface Props {
  visible: boolean;
  record?: any;
  onCancel: () => void;
  onSuccess: () => void;
}

const MappingFormModal: React.FC<Props> = ({ visible, record, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const isEdit = !!record;
  const [users, setUsers] = useState<any[]>([]);
  const [agents, setAgents] = useState<any[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [loadingAgents, setLoadingAgents] = useState(false);

  const fetchOptions = async () => {
    setLoadingUsers(true);
    setLoadingAgents(true);
    try {
      const [userRes, agentRes, currentUserRes] = await Promise.all([
        getUserList(),
        getAgentList(),
        queryCurrentUser(),
      ]);
      const userList = userRes?.code === 200 ? userRes.data || [] : userRes || [];
      setUsers(Array.isArray(userList) ? userList : []);
      setAgents(agentRes?.data || []);

      // Auto-fill tenantId from current user for new mappings
      if (!record && currentUserRes?.code === 200 && currentUserRes.data?.tenantId) {
        form.setFieldValue('tenantId', currentUserRes.data.tenantId);
      }
    } catch {
      message.error('加载选项数据失败');
    }
    setLoadingUsers(false);
    setLoadingAgents(false);
  };

  useEffect(() => {
    if (visible) {
      fetchOptions();
      if (record) {
        form.setFieldsValue(record);
      } else {
        form.resetFields();
      }
    }
  }, [visible, record]);

  const handleOk = async () => {
    const values = await form.validateFields();
    if (isEdit) {
      await updateFeishuMapping(record.id, values);
    } else {
      await createFeishuMapping({ ...values, matchType: 'MANUAL', status: 1 });
    }
    onSuccess();
  };

  return (
    <Modal
      title={isEdit ? '编辑映射' : '新增映射'}
      open={visible}
      onCancel={onCancel}
      onOk={handleOk}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="feishuOpenId"
          label="飞书 Open ID"
          rules={[{ required: true, message: '请输入飞书 Open ID' }]}
        >
          <Input placeholder="ou_xxxxxxxxxx" disabled={isEdit} />
        </Form.Item>
        <Form.Item name="feishuUserName" label="飞书用户名">
          <Input placeholder="张三" />
        </Form.Item>
        <Form.Item name="feishuEmail" label="飞书邮箱">
          <Input placeholder="user@example.com" />
        </Form.Item>
        <Form.Item name="feishuMobile" label="飞书手机号">
          <Input placeholder="13800138000" />
        </Form.Item>
        <Form.Item name="feishuEmployeeId" label="飞书工号">
          <Input placeholder="EMP001" />
        </Form.Item>
        <Form.Item
          name="s2UserId"
          label="平台用户"
          rules={[{ required: true, message: '请选择平台用户' }]}
        >
          <Select
            showSearch
            placeholder="请选择平台用户"
            loading={loadingUsers}
            optionFilterProp="label"
            options={users.map((u: any) => ({
              label: `${u.displayName || u.name}${u.email ? ` (${u.email})` : ''}`,
              value: u.id,
            }))}
          />
        </Form.Item>
        <Form.Item name="tenantId" label="租户" hidden>
          <Input />
        </Form.Item>
        <Form.Item name="defaultAgentId" label="默认助理">
          <Select
            allowClear
            showSearch
            placeholder="请选择默认助理"
            loading={loadingAgents}
            optionFilterProp="label"
            options={agents.map((a: any) => ({
              label: a.name,
              value: a.id,
            }))}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default MappingFormModal;
