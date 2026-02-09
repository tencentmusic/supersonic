import React, { useState } from 'react';
import { Table, Checkbox, Select, Input, Typography, Space, Tag, Collapse } from 'antd';
import type {
  DiscoveredSchema,
  DiscoveredTable,
  DiscoveredColumn,
  TableSyncConfig,
  SyncMode,
} from '@/services/dataSync';

const { Text } = Typography;

interface StreamConfigStepProps {
  schema: DiscoveredSchema | null;
  tableConfigs: TableSyncConfig[];
  onChange: (configs: TableSyncConfig[]) => void;
}

const SYNC_MODE_OPTIONS: { value: SyncMode; label: string; description: string }[] = [
  { value: 'FULL', label: '全量覆盖', description: '每次全量同步，目标表 TRUNCATE 后重写' },
  {
    value: 'INCREMENTAL',
    label: '增量追加',
    description: '仅同步游标字段 > 上次水位线的记录',
  },
  {
    value: 'INCREMENTAL_DEDUP',
    label: '增量去重',
    description: '增量同步 + 按主键去重',
  },
  {
    value: 'PARTITION_OVERWRITE',
    label: '分区覆盖',
    description: '按分区字段删除后重写',
  },
];

const StreamConfigStep: React.FC<StreamConfigStepProps> = ({ schema, tableConfigs, onChange }) => {
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);
  const [searchText, setSearchText] = useState('');

  const isTableSelected = (tableName: string) => {
    const config = tableConfigs.find((c) => c.sourceTable === tableName);
    return !!config?.syncMode; // has syncMode means selected
  };

  const getTableConfig = (tableName: string): TableSyncConfig => {
    return (
      tableConfigs.find((c) => c.sourceTable === tableName) || {
        sourceTable: tableName,
        targetTable: tableName,
        syncMode: 'FULL',
      }
    );
  };

  const updateTableConfig = (tableName: string, updates: Partial<TableSyncConfig>) => {
    const existing = tableConfigs.find((c) => c.sourceTable === tableName);
    if (existing) {
      onChange(
        tableConfigs.map((c) => (c.sourceTable === tableName ? { ...c, ...updates } : c)),
      );
    } else {
      onChange([
        ...tableConfigs,
        { sourceTable: tableName, targetTable: tableName, syncMode: 'FULL', ...updates },
      ]);
    }
  };

  const toggleTableSelection = (tableName: string, selected: boolean) => {
    if (selected) {
      const existing = tableConfigs.find((c) => c.sourceTable === tableName);
      if (!existing) {
        onChange([
          ...tableConfigs,
          { sourceTable: tableName, targetTable: tableName, syncMode: 'FULL' },
        ]);
      }
    } else {
      onChange(tableConfigs.filter((c) => c.sourceTable !== tableName));
    }
  };

  const handleSelectAll = (selected: boolean) => {
    if (selected) {
      const allConfigs =
        schema?.tables.map((t) => ({
          sourceTable: t.tableName,
          targetTable: t.tableName,
          syncMode: 'FULL' as SyncMode,
        })) || [];
      onChange(allConfigs);
    } else {
      onChange([]);
    }
  };

  const filteredTables =
    schema?.tables.filter((t) =>
      t.tableName.toLowerCase().includes(searchText.toLowerCase()),
    ) || [];

  const selectedCount = tableConfigs.filter((c) => c.syncMode).length;
  const totalCount = schema?.tables?.length || 0;

  const needsCursorField = (mode: SyncMode) =>
    mode === 'INCREMENTAL' || mode === 'INCREMENTAL_DEDUP' || mode === 'PARTITION_OVERWRITE';

  const renderExpandedRow = (record: DiscoveredTable) => {
    const config = getTableConfig(record.tableName);
    const columns = record.columns || [];

    return (
      <div style={{ padding: '8px 0' }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <div>
            <Text strong>字段列表 ({columns.length})</Text>
          </div>
          <Table
            size="small"
            dataSource={columns}
            rowKey="columnName"
            pagination={false}
            columns={[
              {
                title: '字段名',
                dataIndex: 'columnName',
                width: 200,
              },
              {
                title: '类型',
                dataIndex: 'columnType',
                width: 120,
              },
              {
                title: '说明',
                dataIndex: 'columnComment',
                ellipsis: true,
                render: (val: string) => val || '-',
              },
            ]}
          />
          {needsCursorField(config.syncMode) && (
            <div style={{ marginTop: 8 }}>
              <Space>
                <Text>游标字段：</Text>
                <Select
                  style={{ width: 200 }}
                  placeholder="选择游标字段"
                  value={config.cursorField}
                  onChange={(val) => updateTableConfig(record.tableName, { cursorField: val })}
                  options={columns.map((c: DiscoveredColumn) => ({
                    value: c.columnName,
                    label: `${c.columnName} (${c.columnType})`,
                  }))}
                  showSearch
                  optionFilterProp="label"
                />
              </Space>
              {config.syncMode === 'INCREMENTAL_DEDUP' && (
                <Space style={{ marginLeft: 16 }}>
                  <Text>主键字段：</Text>
                  <Select
                    style={{ width: 200 }}
                    placeholder="选择主键字段"
                    value={config.primaryKey}
                    onChange={(val) => updateTableConfig(record.tableName, { primaryKey: val })}
                    options={columns.map((c: DiscoveredColumn) => ({
                      value: c.columnName,
                      label: `${c.columnName} (${c.columnType})`,
                    }))}
                    showSearch
                    optionFilterProp="label"
                  />
                </Space>
              )}
            </div>
          )}
        </Space>
      </div>
    );
  };

  const columns = [
    {
      title: (
        <Checkbox
          checked={selectedCount === totalCount && totalCount > 0}
          indeterminate={selectedCount > 0 && selectedCount < totalCount}
          onChange={(e) => handleSelectAll(e.target.checked)}
        />
      ),
      width: 50,
      render: (_: any, record: DiscoveredTable) => (
        <Checkbox
          checked={isTableSelected(record.tableName)}
          onChange={(e) => toggleTableSelection(record.tableName, e.target.checked)}
        />
      ),
    },
    {
      title: '源表',
      dataIndex: 'tableName',
      width: 200,
      render: (name: string) => <Text strong>{name}</Text>,
    },
    {
      title: '同步模式',
      width: 160,
      render: (_: any, record: DiscoveredTable) => {
        const config = getTableConfig(record.tableName);
        const isSelected = isTableSelected(record.tableName);
        return (
          <Select
            style={{ width: 140 }}
            value={config.syncMode}
            disabled={!isSelected}
            onChange={(val) => updateTableConfig(record.tableName, { syncMode: val })}
            options={SYNC_MODE_OPTIONS.map((opt) => ({
              value: opt.value,
              label: opt.label,
            }))}
          />
        );
      },
    },
    {
      title: '游标字段',
      width: 140,
      render: (_: any, record: DiscoveredTable) => {
        const config = getTableConfig(record.tableName);
        const isSelected = isTableSelected(record.tableName);
        if (!isSelected) return '-';
        if (!needsCursorField(config.syncMode)) return '-';
        return config.cursorField ? (
          <Tag color="blue">{config.cursorField}</Tag>
        ) : (
          <Text type="warning">需配置</Text>
        );
      },
    },
    {
      title: '目标表',
      width: 200,
      render: (_: any, record: DiscoveredTable) => {
        const config = getTableConfig(record.tableName);
        const isSelected = isTableSelected(record.tableName);
        return (
          <Input
            size="small"
            value={config.targetTable}
            disabled={!isSelected}
            placeholder={record.tableName}
            onChange={(e) => updateTableConfig(record.tableName, { targetTable: e.target.value })}
          />
        );
      },
    },
  ];

  return (
    <div style={{ marginTop: 16 }}>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="搜索表名"
          style={{ width: 240 }}
          allowClear
          onSearch={setSearchText}
          onChange={(e) => !e.target.value && setSearchText('')}
        />
        <Text type="secondary">
          已选择 {selectedCount} / {totalCount} 张表
        </Text>
      </Space>

      <Collapse
        ghost
        defaultActiveKey={['modeHelp']}
        items={[
          {
            key: 'modeHelp',
            label: '同步模式说明',
            children: (
              <div style={{ marginBottom: 16 }}>
                {SYNC_MODE_OPTIONS.map((opt) => (
                  <div key={opt.value} style={{ marginBottom: 4 }}>
                    <Tag>{opt.label}</Tag>
                    <Text type="secondary">{opt.description}</Text>
                  </div>
                ))}
              </div>
            ),
          },
        ]}
      />

      <Table
        size="small"
        rowKey="tableName"
        columns={columns}
        dataSource={filteredTables}
        pagination={{ pageSize: 10, showSizeChanger: true }}
        expandable={{
          expandedRowRender: renderExpandedRow,
          expandedRowKeys,
          onExpandedRowsChange: (keys) => setExpandedRowKeys(keys as React.Key[]),
          rowExpandable: (record) => isTableSelected(record.tableName),
        }}
      />
    </div>
  );
};

export default StreamConfigStep;
