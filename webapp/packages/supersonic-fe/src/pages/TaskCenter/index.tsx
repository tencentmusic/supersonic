import React, { useState } from 'react';
import { Card, Tabs } from 'antd';
import { CalendarOutlined, BellOutlined, DownloadOutlined } from '@ant-design/icons';
import ScheduleTab from './ScheduleTab';
import AlertRuleTab from './AlertRuleTab';
import ExportTaskTab from './ExportTaskTab';
import styles from './style.less';

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
      children: <AlertRuleTab />,
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
    <div className={styles.taskCenterPage}>
      <Card className={styles.contentCard}>
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={tabItems} />
      </Card>
    </div>
  );
};

export default TaskCenterPage;
