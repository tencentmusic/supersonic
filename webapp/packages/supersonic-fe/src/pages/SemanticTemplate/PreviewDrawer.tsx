import React from 'react';
import { Drawer, Tabs, Descriptions, Table, Empty, Tag } from 'antd';
import {
  FolderOutlined,
  TableOutlined,
  BarChartOutlined,
  AppstoreOutlined,
  RobotOutlined,
  TagsOutlined,
} from '@ant-design/icons';
import { SemanticPreviewResult } from '@/services/semanticTemplate';

interface PreviewDrawerProps {
  visible: boolean;
  data: SemanticPreviewResult | null;
  onClose: () => void;
}

const PreviewDrawer: React.FC<PreviewDrawerProps> = ({ visible, data, onClose }) => {
  if (!data) {
    return null;
  }

  const modelColumns = [
    { title: '模型名称', dataIndex: 'name', key: 'name' },
    { title: '代码', dataIndex: 'bizName', key: 'bizName' },
    { title: '描述', dataIndex: 'description', key: 'description' },
  ];

  const tabItems = [
    {
      key: 'domain',
      label: (
        <span>
          <FolderOutlined /> 主题域
        </span>
      ),
      children: data.domain ? (
        <Descriptions bordered column={1} size="small">
          <Descriptions.Item label="名称">{data.domain.name}</Descriptions.Item>
          <Descriptions.Item label="代码">{data.domain.bizName}</Descriptions.Item>
          <Descriptions.Item label="描述">
            {data.domain.description || '-'}
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <Empty description="无主题域配置" />
      ),
    },
    {
      key: 'models',
      label: (
        <span>
          <TableOutlined /> 模型 ({data.models?.length || 0})
        </span>
      ),
      children:
        data.models && data.models.length > 0 ? (
          <Table
            dataSource={data.models}
            columns={modelColumns}
            rowKey="bizName"
            size="small"
            pagination={false}
          />
        ) : (
          <Empty description="暂无模型" />
        ),
    },
    {
      key: 'metrics',
      label: (
        <span>
          <BarChartOutlined /> 指标 ({data.metrics?.length || 0})
        </span>
      ),
      children:
        data.metrics && data.metrics.length > 0 ? (
          <Table
            dataSource={data.metrics}
            columns={[
              { title: '指标名称', dataIndex: 'name', key: 'name' },
              { title: '代码', dataIndex: 'bizName', key: 'bizName' },
            ]}
            rowKey="bizName"
            size="small"
            pagination={false}
          />
        ) : (
          <Empty description="指标将根据模型度量自动生成" />
        ),
    },
    {
      key: 'dimensions',
      label: (
        <span>
          <TableOutlined /> 维度 ({data.dimensions?.length || 0})
        </span>
      ),
      children:
        data.dimensions && data.dimensions.length > 0 ? (
          <Table
            dataSource={data.dimensions}
            columns={[
              { title: '维度名称', dataIndex: 'name', key: 'name' },
              { title: '代码', dataIndex: 'bizName', key: 'bizName' },
              { title: '类型', dataIndex: 'type', key: 'type' },
            ]}
            rowKey="bizName"
            size="small"
            pagination={false}
          />
        ) : (
          <Empty description="暂无维度" />
        ),
    },
    {
      key: 'dataset',
      label: (
        <span>
          <AppstoreOutlined /> 数据集
        </span>
      ),
      children: data.dataSet ? (
        <Descriptions bordered column={1} size="small">
          <Descriptions.Item label="名称">{data.dataSet.name}</Descriptions.Item>
          <Descriptions.Item label="代码">{data.dataSet.bizName}</Descriptions.Item>
          <Descriptions.Item label="描述">
            {data.dataSet.description || '-'}
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <Empty description="数据集将自动生成" />
      ),
    },
    {
      key: 'agent',
      label: (
        <span>
          <RobotOutlined /> Agent (配置)
        </span>
      ),
      children: data.agent ? (
        <div>
          <Tag color="blue" style={{ marginBottom: 12 }}>
            Agent不会自动创建，配置信息供手动创建使用
          </Tag>
          <Descriptions bordered column={1} size="small">
            <Descriptions.Item label="名称">{data.agent.name}</Descriptions.Item>
            <Descriptions.Item label="描述">{data.agent.description || '-'}</Descriptions.Item>
            <Descriptions.Item label="示例问题">
              {data.agent.examples && data.agent.examples.length > 0 ? (
                <ul style={{ margin: 0, paddingLeft: 20 }}>
                  {data.agent.examples.map((e, i) => (
                    <li key={i}>{e}</li>
                  ))}
                </ul>
              ) : (
                '-'
              )}
            </Descriptions.Item>
          </Descriptions>
        </div>
      ) : (
        <Empty description="无Agent配置" />
      ),
    },
    {
      key: 'terms',
      label: (
        <span>
          <TagsOutlined /> 术语 ({data.terms?.length || 0})
        </span>
      ),
      children:
        data.terms && data.terms.length > 0 ? (
          <Table
            dataSource={data.terms}
            columns={[
              { title: '术语', dataIndex: 'name', key: 'name' },
              { title: '描述', dataIndex: 'description', key: 'description' },
              {
                title: '别名',
                dataIndex: 'alias',
                key: 'alias',
                render: (aliases: string[]) =>
                  aliases?.map((a) => <Tag key={a}>{a}</Tag>) || '-',
              },
            ]}
            rowKey="name"
            size="small"
            pagination={false}
          />
        ) : (
          <Empty description="暂无术语定义" />
        ),
    },
  ];

  return (
    <Drawer
      title="部署预览"
      width={800}
      open={visible}
      onClose={onClose}
      styles={{ body: { paddingTop: 0 } }}
    >
      <Tabs items={tabItems} defaultActiveKey="domain" />
    </Drawer>
  );
};

export default PreviewDrawer;
