import React, { useState } from 'react';
import { Card, Tabs } from 'antd';
import ScheduleTab from './ScheduleTab';
import AlertRuleTab from './AlertRuleTab';
import ExportTaskTab from './ExportTaskTab';
import styles from './style.less';

const TaskCenterPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('schedule');

  const tabItems = [
    {
      key: 'schedule',
      label: '定时报表',
      children: <ScheduleTab />,
    },
    {
      key: 'alert',
      label: '异常提醒',
      children: <AlertRuleTab />,
    },
    {
      key: 'export',
      label: '导出任务',
      children: <ExportTaskTab />,
    },
  ];

  return (
    <div className={styles.taskCenterPage}>
      <h1 className={styles.pageTitle}>任务中心</h1>
      <Card className={styles.contentCard}>
        <Tabs
          className={styles.taskCenterTabs}
          activeKey={activeTab}
          onChange={setActiveTab}
          items={tabItems}
        />
      </Card>
    </div>
  );
};

export default TaskCenterPage;
