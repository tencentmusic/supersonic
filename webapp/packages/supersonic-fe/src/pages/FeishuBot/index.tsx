import React from 'react';
import { Tabs } from 'antd';
import UserMappingTab from './UserMappingTab';
import QueryLogTab from './QueryLogTab';
import BotConfigTab from './BotConfigTab';

const FeishuBot: React.FC = () => {
  return (
    <div style={{ padding: 24 }}>
      <Tabs
        defaultActiveKey="mapping"
        items={[
          {
            key: 'mapping',
            label: '用户映射',
            children: <UserMappingTab />,
          },
          {
            key: 'queryLog',
            label: '查询日志',
            children: <QueryLogTab />,
          },
          {
            key: 'config',
            label: '机器人配置',
            children: <BotConfigTab />,
          },
        ]}
      />
    </div>
  );
};

export default FeishuBot;
