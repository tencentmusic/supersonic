import React from 'react';
import { Input, Select, Space } from 'antd';
import { SearchOutlined } from '@ant-design/icons';

const STATUS_OPTIONS = [
  { label: '全部状态', value: '' },
  { label: '可查看', value: 'AVAILABLE' },
  { label: '暂无结果', value: 'NO_RESULT' },
  { label: '结果过期', value: 'EXPIRED' },
  { label: '最近失败', value: 'RECENTLY_FAILED' },
  { label: '未配置投递', value: 'NO_DELIVERY' },
];

const VIEW_OPTIONS = [
  { label: '全部', value: '' },
  { label: '我订阅的', value: 'subscribed' },
];

interface FilterBarProps {
  keyword: string;
  domainName: string;
  statusFilter: string;
  viewFilter: string;
  domainOptions: string[];
  onKeywordChange: (val: string) => void;
  onDomainChange: (val: string) => void;
  onStatusChange: (val: string) => void;
  onViewChange: (val: string) => void;
}

const FilterBar: React.FC<FilterBarProps> = ({
  keyword,
  domainName,
  statusFilter,
  viewFilter,
  domainOptions,
  onKeywordChange,
  onDomainChange,
  onStatusChange,
  onViewChange,
}) => {
  return (
    <Space wrap size={12}>
      <Input
        placeholder="搜索报表名称"
        prefix={<SearchOutlined />}
        value={keyword}
        onChange={(e) => onKeywordChange(e.target.value)}
        allowClear
        style={{ width: 220 }}
      />
      <Select
        value={domainName}
        onChange={onDomainChange}
        style={{ width: 160 }}
        options={[
          { label: '全部业务域', value: '' },
          ...domainOptions.map((d) => ({ label: d, value: d })),
        ]}
      />
      <Select
        value={statusFilter}
        onChange={onStatusChange}
        style={{ width: 140 }}
        options={STATUS_OPTIONS}
      />
      <Select
        value={viewFilter}
        onChange={onViewChange}
        style={{ width: 140 }}
        options={VIEW_OPTIONS}
      />
    </Space>
  );
};

export default FilterBar;
