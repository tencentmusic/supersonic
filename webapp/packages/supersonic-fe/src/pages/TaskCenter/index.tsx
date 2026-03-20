import React, { useState } from 'react';
import { Tabs } from 'antd';
import { CalendarOutlined, BellOutlined, DownloadOutlined } from '@ant-design/icons';
import ScheduleTab from './ScheduleTab';
import ExportTaskTab from './ExportTaskTab';

const TaskCenterPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('schedule');

  const tabItems = [
    {
      key: 'schedule',
      label: (
        <span>
          <CalendarOutlined /> 定时报表
        </span>
      ),
      children: <ScheduleTab />,
    },
    {
      key: 'alert',
      label: (
        <span>
          <BellOutlined /> 异常提醒
        </span>
      ),
      children: (
        <div style={{ padding: 24, color: '#999' }}>异常提醒功能即将推出</div>
      ),
    },
    {
      key: 'export',
      label: (
        <span>
          <DownloadOutlined /> 导出任务
        </span>
      ),
      children: <ExportTaskTab />,
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <h2>任务中心</h2>
      <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
    </div>
  );
};

export default TaskCenterPage;
