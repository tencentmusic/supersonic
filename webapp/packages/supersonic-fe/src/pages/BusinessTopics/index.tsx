import React, { useEffect, useState } from 'react';
import { Table, Tag, Space, Button, Popconfirm, message, Empty } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import TopicFormModal from './components/TopicFormModal';
import TopicDetailDrawer from './components/TopicDetailDrawer';
import {
  getTopicList,
  createTopic,
  updateTopic,
  deleteTopic,
} from '@/services/businessTopic';
import type { BusinessTopic } from '@/services/businessTopic';
import taskStyles from '../TaskCenter/style.less';
import styles from './style.less';

const BusinessTopicsPage: React.FC = () => {
  const [data, setData] = useState<BusinessTopic[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  // Form modal
  const [formVisible, setFormVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<BusinessTopic | undefined>();

  // Detail drawer
  const [detailDrawer, setDetailDrawer] = useState<{ visible: boolean; topicId?: number }>({
    visible: false,
  });

  const fetchData = async (current = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res: any = await getTopicList({ current, pageSize });
      const pageData = res?.data ?? res;
      setData(pageData?.records || []);
      setPagination({ current, pageSize, total: pageData?.total || 0 });
    } catch {
      message.error('加载经营主题失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreate = () => {
    setEditRecord(undefined);
    setFormVisible(true);
  };

  const handleEdit = (record: BusinessTopic) => {
    setEditRecord(record);
    setFormVisible(true);
  };

  const handleFormSubmit = async (values: Partial<BusinessTopic>) => {
    try {
      if (editRecord?.id) {
        await updateTopic(editRecord.id, values);
        message.success('更新成功');
      } else {
        await createTopic(values);
        message.success('创建成功');
      }
      setFormVisible(false);
      fetchData(pagination.current, pagination.pageSize);
    } catch {
      message.error(editRecord ? '更新失败' : '创建失败');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteTopic(id);
      message.success('已删除');
      fetchData(pagination.current, pagination.pageSize);
    } catch {
      message.error('删除失败');
    }
  };

  const columns = [
    {
      title: '主题名称',
      dataIndex: 'name',
      width: 200,
      ellipsis: true,
      render: (val: string, record: BusinessTopic) => (
        <a onClick={() => setDetailDrawer({ visible: true, topicId: record.id })}>
          {val}
        </a>
      ),
    },
    {
      title: '说明',
      dataIndex: 'description',
      width: 200,
      ellipsis: true,
      render: (val?: string) => val || '—',
    },
    {
      title: '优先级',
      dataIndex: 'priority',
      width: 80,
      align: 'center' as const,
    },
    {
      title: '固定报表',
      dataIndex: 'fixedReportCount',
      width: 90,
      align: 'center' as const,
      render: (val: number) => val || 0,
    },
    {
      title: '告警规则',
      dataIndex: 'alertRuleCount',
      width: 90,
      align: 'center' as const,
      render: (val: number) => val || 0,
    },
    {
      title: '定时任务',
      dataIndex: 'scheduleCount',
      width: 90,
      align: 'center' as const,
      render: (val: number) => val || 0,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      render: (enabled: number) => (
        <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '停用'}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (val?: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '—'),
    },
    {
      title: '操作',
      width: 140,
      fixed: 'right' as const,
      render: (_: any, record: BusinessTopic) => (
        <Space size={4} wrap>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确认删除该主题？关联对象将被解绑。"
            onConfirm={() => handleDelete(record.id!)}
            okText="确认"
            cancelText="取消"
          >
            <Button type="link" size="small" danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className={styles.topicsPage}>
      <div className={styles.pageHeader}>
        <h3 className={styles.pageTitle}>经营主题</h3>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建主题
        </Button>
      </div>
      <div className={taskStyles.tableShell}>
        <Table
          rowKey="id"
          size="middle"
          bordered={false}
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="暂无经营主题" /> }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => fetchData(page, size),
          }}
        />
      </div>

      <TopicFormModal
        visible={formVisible}
        record={editRecord}
        onCancel={() => setFormVisible(false)}
        onSubmit={handleFormSubmit}
      />

      <TopicDetailDrawer
        visible={detailDrawer.visible}
        topicId={detailDrawer.topicId}
        onClose={() => setDetailDrawer({ visible: false })}
        onItemRemoved={() => fetchData(pagination.current, pagination.pageSize)}
      />
    </div>
  );
};

export default BusinessTopicsPage;
