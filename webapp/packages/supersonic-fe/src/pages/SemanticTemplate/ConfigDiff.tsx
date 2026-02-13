import React, { useMemo } from 'react';
import { Collapse, Tag, Empty, Space, Typography } from 'antd';
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer-continued';
import type { SemanticTemplateConfig } from '@/services/semanticTemplate';
import {
  diffTemplateConfig,
  groupChanges,
  CATEGORY_LABELS,
  type ChangeCategory,
  type ConfigChange,
} from './diffUtils';

const { Text } = Typography;

interface ConfigDiffProps {
  oldConfig?: SemanticTemplateConfig;
  newConfig?: SemanticTemplateConfig;
  oldVersion?: number;
  newVersion?: number;
  oldTime?: string;
  newTime?: string;
}

const TYPE_TAG: Record<string, { color: string; label: string }> = {
  added: { color: 'success', label: '新增' },
  removed: { color: 'error', label: '删除' },
  modified: { color: 'warning', label: '变更' },
};

const renderChange = (change: ConfigChange, idx: number) => {
  const tagCfg = TYPE_TAG[change.type];
  return (
    <div key={idx} style={{ padding: '4px 0', lineHeight: '22px' }}>
      <Tag color={tagCfg.color}>{tagCfg.label}</Tag>
      <Text strong>{change.name}</Text>
      {change.parentName && (
        <Text type="secondary"> — 模型 {change.parentName}</Text>
      )}
      {change.detail && (
        <div style={{ marginLeft: 56, color: '#666', fontSize: 12 }}>{change.detail}</div>
      )}
    </div>
  );
};

const ConfigDiff: React.FC<ConfigDiffProps> = ({
  oldConfig,
  newConfig,
  oldVersion,
  newVersion,
  oldTime,
  newTime,
}) => {
  const changes = useMemo(
    () => diffTemplateConfig(oldConfig, newConfig),
    [oldConfig, newConfig],
  );

  const grouped = useMemo(() => groupChanges(changes), [changes]);

  if (changes.length === 0) {
    return <Empty description="两个版本的配置完全一致，无变更" />;
  }

  const structuredItems = (Object.keys(grouped) as ChangeCategory[])
    .filter((cat) => grouped[cat].length > 0)
    .map((cat) => ({
      key: cat,
      label: (
        <Space>
          {CATEGORY_LABELS[cat]}
          <Tag>{grouped[cat].length} 项变更</Tag>
        </Space>
      ),
      children: grouped[cat].map(renderChange),
    }));

  const oldJson = JSON.stringify(oldConfig || {}, null, 2);
  const newJson = JSON.stringify(newConfig || {}, null, 2);
  const leftTitle = `V${oldVersion || '?'}${oldTime ? ` (${oldTime})` : ''}`;
  const rightTitle = `V${newVersion || '?'}${newTime ? ` (${newTime})` : ''}`;

  return (
    <>
      <Collapse
        defaultActiveKey={structuredItems.map((i) => i.key)}
        items={structuredItems}
        style={{ marginBottom: 16 }}
      />
      <Collapse
        items={[
          {
            key: 'raw-diff',
            label: 'JSON 原始对比',
            children: (
              <div style={{ maxHeight: 500, overflow: 'auto' }}>
                <ReactDiffViewer
                  oldValue={oldJson}
                  newValue={newJson}
                  splitView={true}
                  compareMethod={DiffMethod.WORDS}
                  leftTitle={leftTitle}
                  rightTitle={rightTitle}
                />
              </div>
            ),
          },
        ]}
      />
    </>
  );
};

export default ConfigDiff;
