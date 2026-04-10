import React, { useEffect, useState } from 'react';
import { Drawer, Descriptions, Table, Tag, Button, Popconfirm, message, Empty } from 'antd';
import dayjs from 'dayjs';
import type { BusinessTopic, TopicItem } from '@/services/businessTopic';
import { getTopicDetail, removeTopicItem } from '@/services/businessTopic';
import { MSG } from '@/common/messages';

const ITEM_TYPE_MAP: Record<string, { color: string; text: string }> = {
  FIXED_REPORT: { color: 'blue', text: '固定报表' },
  ALERT_RULE: { color: 'red', text: '告警规则' },
  SCHEDULE: { color: 'green', text: '定时任务' },
};

interface TopicDetailDrawerProps {
  visible: boolean;
  topicId?: number;
  onClose: () => void;
  onItemRemoved?: () => void;
}

const TopicDetailDrawer: React.FC<TopicDetailDrawerProps> = ({
  visible,
  topicId,
  onClose,
  onItemRemoved,
}) => {
  const [topic, setTopic] = useState<BusinessTopic | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchDetail = async () => {
    if (!topicId) return;
    setLoading(true);
    try {
      const res: any = await getTopicDetail(topicId);
      const data = res?.data ?? res;
      setTopic(data);
    } catch {
      message.error(MSG.OPERATION_FAILED);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && topicId) {
      fetchDetail();
    }
  }, [visible, topicId]);

  const handleRemoveItem = async (itemType: string, itemId: number) => {
    if (!topicId) return;
    try {
      await removeTopicItem(topicId, itemType, itemId);
      message.success(MSG.DELETE_SUCCESS);
      fetchDetail();
      onItemRemoved?.();
    } catch {
      message.error(MSG.DELETE_FAILED);
    }
  };

  const itemColumns = [
    {
      title: '类型',
      dataIndex: 'itemType',
      width: 120,
      render: (type: string) => {
        const info = ITEM_TYPE_MAP[type] || { color: 'default', text: type };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '名称',
      dataIndex: 'itemName',
      ellipsis: true,
    },
    {
      title: '状态摘要',
      width: 140,
      render: (_: unknown, record: TopicItem) => {
        if (record.itemType === 'FIXED_REPORT' && record.consumptionStatus) {
          return <Tag>{record.consumptionStatus}</Tag>;
        }
        if (record.itemType === 'SCHEDULE' && record.scheduleEnabled !== undefined) {
          return record.scheduleEnabled ? <Tag color="green">启用</Tag> : <Tag>暂停</Tag>;
        }
        return '—';
      },
    },
    {
      title: '操作',
      width: 80,
      render: (_: any, record: TopicItem) => (
        <Popconfirm title="确认移除?" onConfirm={() => handleRemoveItem(record.itemType, record.itemId)} okText="确认" cancelText="取消">
          <Button type="link" size="small" danger>
            移除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <Drawer
      title={topic?.name || '主题详情'}
      open={visible}
      onClose={onClose}
      width={640}
      loading={loading}
    >
      {topic && (
        <>
          <Descriptions column={2} size="small" style={{ marginBottom: 24 }}>
            <Descriptions.Item label="说明" span={2}>
              {topic.description || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="优先级">{topic.priority}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={topic.enabled ? 'green' : 'default'}>
                {topic.enabled ? '启用' : '停用'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="固定报表">{topic.fixedReportCount} 个</Descriptions.Item>
            <Descriptions.Item label="告警规则">{topic.alertRuleCount} 个</Descriptions.Item>
            <Descriptions.Item label="定时任务">{topic.scheduleCount} 个</Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {topic.createdAt ? dayjs(topic.createdAt).format('YYYY-MM-DD HH:mm:ss') : '—'}
            </Descriptions.Item>
          </Descriptions>

          <h4 style={{ marginBottom: 12 }}>关联对象</h4>
          <Table
            rowKey={(r) => `${r.itemType}-${r.itemId}`}
            columns={itemColumns}
            dataSource={topic.items || []}
            size="small"
            bordered={false}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无关联对象" /> }}
          />
        </>
      )}
    </Drawer>
  );
};

export default TopicDetailDrawer;
