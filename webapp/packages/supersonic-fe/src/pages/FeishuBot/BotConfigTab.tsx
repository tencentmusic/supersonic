import React from 'react';
import { Descriptions, Tag } from 'antd';

const BotConfigTab: React.FC = () => {
  return (
    <div>
      <Descriptions
        title="飞书机器人配置"
        bordered
        column={1}
        style={{ maxWidth: 600 }}
      >
        <Descriptions.Item label="状态">
          <Tag color="orange">配置文件管理</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="说明">
          飞书机器人配置通过 application.yaml 中的 <code>s2.feishu</code> 配置项管理。
          修改配置后需要重启服务生效。
        </Descriptions.Item>
        <Descriptions.Item label="配置项">
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li><code>s2.feishu.enabled</code> — 是否启用飞书机器人</li>
            <li><code>s2.feishu.app-id</code> — 飞书应用 App ID</li>
            <li><code>s2.feishu.app-secret</code> — 飞书应用 App Secret</li>
            <li><code>s2.feishu.verification-token</code> — 事件验证 Token</li>
            <li><code>s2.feishu.default-agent-id</code> — 默认 Agent ID</li>
            <li><code>s2.feishu.max-table-rows</code> — 卡片内表格最大行数</li>
            <li><code>s2.feishu.user-mapping.auto-match-enabled</code> — 自动匹配开关</li>
          </ul>
        </Descriptions.Item>
        <Descriptions.Item label="Webhook URL">
          <code>/api/feishu/webhook</code> — 配置到飞书开放平台的事件订阅地址
        </Descriptions.Item>
      </Descriptions>
    </div>
  );
};

export default BotConfigTab;
