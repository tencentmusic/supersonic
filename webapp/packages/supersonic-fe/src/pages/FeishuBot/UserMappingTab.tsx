import React, { useState, useEffect } from 'react';
import { Table, Button, Space, Tag, Switch, message, Popconfirm } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { getFeishuMappings, deleteFeishuMapping, enableFeishuMapping, disableFeishuMapping } from '@/services/feishu';
import { getUserList } from '@/services/user';
import { getAgentList } from '@/pages/Agent/service';
import MappingFormModal from './components/MappingFormModal';

const MATCH_TYPE_LABELS: Record<string, string> = {
  PENDING: '待审核',
  MANUAL: '手动',
  AUTO_EMPLOYEE_ID: '工号匹配',
  AUTO_EMAIL: '邮箱匹配',
  AUTO_MOBILE: '手机匹配',
  OAUTH_BIND: 'OAuth绑定',
  SYNC: '同步',
};

const MATCH_TYPE_COLORS: Record<string, string> = {
  PENDING: 'warning',
  MANUAL: 'blue',
  AUTO_EMPLOYEE_ID: 'green',
  AUTO_EMAIL: 'cyan',
  AUTO_MOBILE: 'orange',
  OAUTH_BIND: 'purple',
  SYNC: 'geekblue',
};

const UserMappingTab: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [modalVisible, setModalVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<any>(null);
  const [userMap, setUserMap] = useState<Record<number, string>>({});
  const [agentMap, setAgentMap] = useState<Record<number, string>>({});

  const fetchLookups = async () => {
    try {
      const [userRes, agentRes] = await Promise.all([getUserList(), getAgentList()]);
      const userList = userRes?.code === 200 ? userRes.data || [] : userRes || [];
      const uMap: Record<number, string> = {};
      (Array.isArray(userList) ? userList : []).forEach((u: any) => {
        uMap[u.id] = u.displayName || u.name;
      });
      setUserMap(uMap);

      const aMap: Record<number, string> = {};
      (agentRes?.data || []).forEach((a: any) => {
        aMap[a.id] = a.name;
      });
      setAgentMap(aMap);
    } catch {
      // ignore
    }
  };

  const fetchData = async (page = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res = await getFeishuMappings({ current: page, pageSize });
      // ResponseAdvice wraps response as {code, data: {records, total, ...}}
      const body = res?.data ?? res;
      if (Array.isArray(body)) {
        setData(body);
        setPagination({ current: page, pageSize, total: body.length });
      } else if (body?.records) {
        setData(body.records);
        setPagination({ current: page, pageSize, total: body.total || 0 });
      }
    } catch (e) {
      message.error('加载映射列表失败');
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchLookups();
    fetchData();
  }, []);

  const handleDelete = async (id: number) => {
    await deleteFeishuMapping(id);
    message.success('删除成功');
    fetchData(pagination.current, pagination.pageSize);
  };

  const handleToggleStatus = async (id: number, checked: boolean) => {
    if (checked) {
      await enableFeishuMapping(id);
    } else {
      await disableFeishuMapping(id);
    }
    message.success('状态更新成功');
    fetchData(pagination.current, pagination.pageSize);
  };

  const columns = [
    { title: '飞书用户名', dataIndex: 'feishuUserName', key: 'feishuUserName' },
    { title: '飞书OpenID', dataIndex: 'feishuOpenId', key: 'feishuOpenId', ellipsis: true },
    {
      title: '匹配类型',
      dataIndex: 'matchType',
      key: 'matchType',
      render: (type: string) => (
        <Tag color={MATCH_TYPE_COLORS[type] || 'default'}>
          {MATCH_TYPE_LABELS[type] || type}
        </Tag>
      ),
    },
    {
      title: '平台用户',
      dataIndex: 's2UserId',
      key: 's2UserId',
      render: (id: number) => userMap[id] ? `${userMap[id]} (${id})` : id,
    },
    {
      title: '默认助理',
      dataIndex: 'defaultAgentId',
      key: 'defaultAgentId',
      render: (id: number) => id ? (agentMap[id] ? agentMap[id] : id) : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number, record: any) => (
        <Switch checked={status === 1} onChange={(checked) => handleToggleStatus(record.id, checked)} />
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <Space>
          <a onClick={() => { setEditRecord(record); setModalVisible(true); }}>编辑</a>
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id)}>
            <a style={{ color: '#ff4d4f' }}>删除</a>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditRecord(null); setModalVisible(true); }}>
          新增映射
        </Button>
      </div>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          ...pagination,
          onChange: (page, pageSize) => fetchData(page, pageSize),
        }}
      />
      <MappingFormModal
        visible={modalVisible}
        record={editRecord}
        onCancel={() => { setModalVisible(false); setEditRecord(null); }}
        onSuccess={() => { setModalVisible(false); setEditRecord(null); fetchData(pagination.current, pagination.pageSize); }}
      />
    </>
  );
};

export default UserMappingTab;
