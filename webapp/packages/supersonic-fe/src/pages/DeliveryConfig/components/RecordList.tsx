import React, { useState, useEffect } from 'react';
import { Drawer, Table, Tag, Button, Space, Tooltip, Popconfirm, message } from 'antd';
import { ReloadOutlined, RedoOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  getRecordList,
  retryRecord,
  DeliveryRecord,
  DELIVERY_STATUS_MAP,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';

interface RecordListProps {
  visible: boolean;
  configId?: number;
  configName?: string;
  scheduleId?: number;
  scheduleName?: string;
  onClose: () => void;
}

const RecordList: React.FC<RecordListProps> = ({
  visible,
  configId,
  configName,
  scheduleId,
  scheduleName,
  onClose,
}) => {
  const [data, setData] = useState<DeliveryRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  const fetchData = async (page = pagination.current, size = pagination.pageSize) => {
    if (!visible || (!configId && !scheduleId)) return;

    setLoading(true);
    try {
      const res = await getRecordList({
        pageNum: page,
        pageSize: size,
        configId,
        scheduleId,
      });
      // Handle response format: could be direct data or wrapped in { code, data }
      const responseData = res?.data ?? res;
      const records = Array.isArray(responseData) ? responseData : (responseData?.records || []);
      const total = responseData?.total || records.length;
      setData(records);
      setPagination((prev) => ({ ...prev, current: page, pageSize: size, total }));
    } catch (error) {
      message.error('Failed to load delivery records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (visible && (configId || scheduleId)) {
      fetchData(1, pagination.pageSize);
    }
  }, [visible, configId, scheduleId]);

  const handleRetry = async (id: number) => {
    try {
      await retryRecord(id);
      message.success('Retry initiated');
      fetchData();
    } catch (error) {
      message.error('Retry failed');
    }
  };

  const columns: ColumnsType<DeliveryRecord> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 60,
    },
    {
      title: '渠道类型',
      dataIndex: 'deliveryType',
      width: 90,
      render: (type: string) => {
        const info = DELIVERY_TYPE_MAP[type as keyof typeof DELIVERY_TYPE_MAP];
        return info ? <Tag color={info.color}>{info.text}</Tag> : type;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (status: string) => {
        const info = DELIVERY_STATUS_MAP[status as keyof typeof DELIVERY_STATUS_MAP];
        return info ? <Tag color={info.color}>{info.text}</Tag> : status;
      },
    },
    {
      title: '调度ID',
      dataIndex: 'scheduleId',
      width: 80,
    },
    {
      title: '执行ID',
      dataIndex: 'executionId',
      width: 80,
    },
    {
      title: '重试次数',
      dataIndex: 'retryCount',
      width: 80,
    },
    {
      title: '开始时间',
      dataIndex: 'startedAt',
      width: 160,
    },
    {
      title: '完成时间',
      dataIndex: 'completedAt',
      width: 160,
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      width: 200,
      ellipsis: true,
      render: (msg: string) =>
        msg ? (
          <Tooltip title={msg}>
            <span style={{ color: 'red' }}>{msg}</span>
          </Tooltip>
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      width: 80,
      render: (_: any, record: DeliveryRecord) => (
        <Space>
          {record.status === 'FAILED' && (
            <Popconfirm
              title="确认重试此推送?"
              onConfirm={() => handleRetry(record.id)}
              okText="确认"
              cancelText="取消"
            >
              <Tooltip title="重试">
                <Button type="link" size="small" icon={<RedoOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const getTitle = () => {
    if (configName) {
      return `推送记录 - ${configName}`;
    }
    if (scheduleName) {
      return `推送记录 - ${scheduleName}`;
    }
    return '推送记录';
  };

  return (
    <Drawer
      title={getTitle()}
      open={visible}
      onClose={onClose}
      width={1000}
      extra={
        <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
          刷新
        </Button>
      }
    >
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => fetchData(page, size),
        }}
      />
    </Drawer>
  );
};

export default RecordList;
