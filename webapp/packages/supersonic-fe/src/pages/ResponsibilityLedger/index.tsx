import React from 'react';
import { Card, Typography, Tabs, Button, Space } from 'antd';
import { history } from '@umijs/max';
import AlertEventsTab from '@/pages/TaskCenter/AlertEventsTab';
import taskStyles from '@/pages/TaskCenter/style.less';

const { Title, Paragraph } = Typography;

/**
 * 运行总览（只读聚合）：Tab 分链「报表执行 / 投递 / 异常处置」，异常 Tab 内嵌任务中心同款列表 API（方案 1）。
 * 满足产品说明书 §5.6 与实现方案阶段 D。
 */
const ResponsibilityLedgerPage: React.FC = () => {
  return (
    <div className={taskStyles.taskCenterPage}>
      <Title level={4} style={{ marginBottom: 8 }}>
        报表运行总览
      </Title>
      <Paragraph type="secondary" style={{ marginBottom: 16 }}>
        汇总查看结果生成、投递状态与异常处置进展；报表与调度明细仍从各入口下钻。
      </Paragraph>

      <Card bordered={false}>
        <Tabs
          items={[
            {
              key: 'execution',
              label: '报表执行',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Paragraph style={{ marginBottom: 0 }}>
                    定时报表执行记录、快照与下载：在任务中心各调度任务的「执行记录」中查看。
                  </Paragraph>
                  <Button type="primary" onClick={() => history.push('/task-center?tab=schedule')}>
                    打开任务中心 · 定时报表
                  </Button>
                  <Paragraph type="secondary">
                    固定报表最新结果与历史：{' '}
                    <Button type="link" style={{ padding: 0 }} onClick={() => history.push('/reports')}>
                      固定报表
                    </Button>
                  </Paragraph>
                </Space>
              ),
            },
            {
              key: 'delivery',
              label: '投递结果',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Paragraph style={{ marginBottom: 0 }}>
                    推送渠道配置、健康状态与单次执行的多渠道投递结果（执行详情内按渠道查看）。
                  </Paragraph>
                  <Button type="primary" onClick={() => history.push('/delivery-config')}>
                    打开推送配置
                  </Button>
                </Space>
              ),
            },
            {
              key: 'alert',
              label: '异常处置',
              children: (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                  <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                    下列列表与「任务中心 · 异常事件」共用接口，只读浏览与筛选；处置操作请在同一列表完成或进入任务中心。
                  </Paragraph>
                  <AlertEventsTab initialResolutionStatus="" />
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
};

export default ResponsibilityLedgerPage;
