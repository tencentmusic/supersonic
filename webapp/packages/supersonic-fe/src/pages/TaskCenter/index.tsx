import React, { useEffect, useMemo, useState } from 'react';
import { useLocation } from '@umijs/max';
import { Card, Tabs } from 'antd';
import queryString from 'query-string';
import ScheduleTab from './ScheduleTab';
import AlertRuleTab from './AlertRuleTab';
import AlertEventsTab from './AlertEventsTab';
import ExportTaskTab from './ExportTaskTab';
import styles from './style.less';

const VALID_TABS = ['schedule', 'alertEvents', 'alert', 'export'] as const;

const TaskCenterPage: React.FC = () => {
  const location = useLocation();
  const q = queryString.parse(location.search);
  const tabFromUrl = typeof q.tab === 'string' ? q.tab : null;
  const resolutionFromUrl = typeof q.resolution === 'string' ? q.resolution : undefined;

  const initialTab = useMemo(() => {
    if (tabFromUrl && VALID_TABS.includes(tabFromUrl as (typeof VALID_TABS)[number])) {
      return tabFromUrl;
    }
    return 'schedule';
  }, [tabFromUrl]);

  const [activeTab, setActiveTab] = useState(initialTab);

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab]);

  const alertEventsChild = useMemo(
    () => (
      <AlertEventsTab
        key={`${resolutionFromUrl ?? 'OPEN'}`}
        initialResolutionStatus={resolutionFromUrl ?? 'OPEN'}
      />
    ),
    [resolutionFromUrl],
  );

  const tabItems = [
    {
      key: 'schedule',
      label: '定时报表',
      children: <ScheduleTab />,
    },
    {
      key: 'alertEvents',
      label: '异常事件',
      children: alertEventsChild,
    },
    {
      key: 'alert',
      label: '告警规则',
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
