import React, { useState, useEffect } from 'react';
import { Table, Tag, Space, DatePicker, Select } from 'antd';
import dayjs from 'dayjs';
import { getFeishuSessions } from '@/services/feishu';

const { RangePicker } = DatePicker;

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'processing',
  SUCCESS: 'success',
  ERROR: 'error',
};

const QueryLogTab: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const [filters, setFilters] = useState<{ status?: string; startDate?: string; endDate?: string }>({});

  const fetchData = async (page = 1, pageSize = 20) => {
    setLoading(true);
    try {
      const res = await getFeishuSessions({ current: page, pageSize, ...filters });
      const body = res?.data ?? res;
      if (Array.isArray(body)) {
        setData(body);
        setPagination({ current: page, pageSize, total: body.length });
      } else if (body?.records) {
        setData(body.records);
        setPagination({ current: page, pageSize, total: body.total || 0 });
      }
    } catch {
      // ignore
    }
    setLoading(false);
  };

  useEffect(() => { fetchData(); }, [filters]);

  const columns = [
    { title: '飞书用户', dataIndex: 'feishuOpenId', key: 'feishuOpenId', width: 160, ellipsis: true },
    { title: '查询文本', dataIndex: 'queryText', key: 'queryText', ellipsis: true },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const labels: Record<string, string> = { PENDING: '处理中', SUCCESS: '成功', ERROR: '失败' };
        return <Tag color={STATUS_COLORS[status] || 'default'}>{labels[status] || status}</Tag>;
      },
    },
    {
      title: 'SQL',
      dataIndex: 'sqlText',
      key: 'sqlText',
      ellipsis: true,
      render: (sql: string) => sql ? <span title={sql} style={{ fontFamily: 'monospace', fontSize: 12 }}>{sql}</span> : '-',
    },
    { title: '行数', dataIndex: 'rowCount', key: 'rowCount', width: 80 },
    { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 180,
      render: (value: string) => value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="状态筛选"
          allowClear
          style={{ width: 140 }}
          onChange={(val) => setFilters({ ...filters, status: val })}
          options={[
            { label: '处理中', value: 'PENDING' },
            { label: '成功', value: 'SUCCESS' },
            { label: '失败', value: 'ERROR' },
          ]}
        />
        <RangePicker
          onChange={(dates, dateStrings) => {
            setFilters({ ...filters, startDate: dateStrings[0], endDate: dateStrings[1] });
          }}
        />
      </Space>
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
    </>
  );
};

export default QueryLogTab;
