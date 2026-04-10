import React from 'react';
import { Tabs } from 'antd';
import { useModel } from '@umijs/max';
import UserMappingTab from './UserMappingTab';
import QueryLogTab from './QueryLogTab';
import BotConfigTab from './BotConfigTab';

const FeishuBot: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser as API.CurrentUser | undefined;
  const isAdmin = Boolean(currentUser?.superAdmin || currentUser?.isAdmin === 1);
  const items = [
    ...(isAdmin
      ? [
          {
            key: 'mapping',
            label: '用户映射',
            children: <UserMappingTab />,
          },
        ]
      : []),
    {
      key: 'queryLog',
      label: '查询日志',
      children: <QueryLogTab />,
    },
    ...(isAdmin
      ? [
          {
            key: 'config',
            label: '机器人配置',
            children: <BotConfigTab />,
          },
        ]
      : []),
  ];

  return (
    <div style={{ padding: 24 }}>
      <Tabs defaultActiveKey={isAdmin ? 'mapping' : 'queryLog'} items={items} />
    </div>
  );
};

export default FeishuBot;
