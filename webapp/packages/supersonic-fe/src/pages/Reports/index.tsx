import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Button, Tag, Space, message, Empty, Spin } from 'antd';
import { BarChartOutlined, CalendarOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import {
  getDeploymentHistory,
  SemanticDeployment,
} from '@/services/semanticTemplate';
import ScheduleForm from '@/pages/ReportSchedule/components/ScheduleForm';
import type { ReportSchedule } from '@/services/reportSchedule';
import { createSchedule } from '@/services/reportSchedule';
import styles from './style.less';

const ReportsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [deployments, setDeployments] = useState<SemanticDeployment[]>([]);
  const [scheduleFormVisible, setScheduleFormVisible] = useState(false);
  const [selectedDatasetId, setSelectedDatasetId] = useState<number | undefined>(undefined);

  const loadDeployments = async () => {
    setLoading(true);
    try {
      const res: any = await getDeploymentHistory();
      if (res?.code === 200 && res?.data) {
        const all = (res.data as SemanticDeployment[]) || [];
        setDeployments(all.filter((d) => d.status === 'SUCCESS'));
      } else {
        setDeployments([]);
      }
    } catch (error) {
      message.error('加载报表列表失败');
      setDeployments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDeployments();
  }, []);

  // Group deployments by domainName
  const grouped: Record<string, SemanticDeployment[]> = {};
  deployments.forEach((d) => {
    const domainName = d.resultDetail?.domainName || '其他';
    if (!grouped[domainName]) {
      grouped[domainName] = [];
    }
    grouped[domainName].push(d);
  });

  const handleViewChat = () => {
    history.push('/chat');
  };

  const handleCreateSchedule = (deployment: SemanticDeployment) => {
    setSelectedDatasetId(deployment.resultDetail?.dataSetId);
    setScheduleFormVisible(true);
  };

  const handleScheduleSubmit = async (values: Partial<ReportSchedule>) => {
    try {
      const res: any = await createSchedule(values);
      if (res?.code === 200) {
        message.success('创建定时任务成功');
        setScheduleFormVisible(false);
        setSelectedDatasetId(undefined);
      } else {
        message.error(res?.msg || '创建定时任务失败');
      }
    } catch (error) {
      message.error('创建定时任务失败');
    }
  };

  const handleScheduleCancel = () => {
    setScheduleFormVisible(false);
    setSelectedDatasetId(undefined);
  };

  const renderDeploymentCard = (deployment: SemanticDeployment) => {
    const templateName =
      deployment.templateName ||
      deployment.templateConfigSnapshot?.domain?.name ||
      `报表 #${deployment.id}`;
    const description =
      deployment.templateConfigSnapshot?.dataSet?.description ||
      deployment.templateConfigSnapshot?.domain?.description ||
      '暂无描述';
    const domainName = deployment.resultDetail?.domainName;
    const dataSetName = deployment.resultDetail?.dataSetName;

    return (
      <Col xs={24} sm={12} lg={8} xl={6} key={deployment.id}>
        <Card
          hoverable
          className={styles.templateCard}
          cover={
            <div className={styles.cardPlaceholder} style={{ height: 100 }}>
              <BarChartOutlined style={{ fontSize: 40, color: '#1890ff' }} />
            </div>
          }
          actions={[
            <Button type="link" key="view" onClick={handleViewChat}>
              <BarChartOutlined /> 查看结果
            </Button>,
            <Button
              type="link"
              key="schedule"
              onClick={() => handleCreateSchedule(deployment)}
            >
              <CalendarOutlined /> 创建定时任务
            </Button>,
          ]}
        >
          <Card.Meta title={templateName} description={description} />
          <div className={styles.cardTags}>
            <Space size={4} wrap>
              {domainName && <Tag color="blue">{domainName}</Tag>}
              {dataSetName && <Tag color="green">{dataSetName}</Tag>}
            </Space>
          </div>
        </Card>
      </Col>
    );
  };

  const domainNames = Object.keys(grouped);

  return (
    <div className={styles.container}>
      <Spin spinning={loading}>
        {deployments.length === 0 && !loading ? (
          <Card>
            <Empty description="暂无已部署的报表" />
          </Card>
        ) : (
          domainNames.map((domainName) => (
            <Card
              key={domainName}
              title={domainName}
              className={styles.sectionCard}
            >
              <Row gutter={[16, 16]}>
                {grouped[domainName].map(renderDeploymentCard)}
              </Row>
            </Card>
          ))
        )}
      </Spin>

      <ScheduleForm
        visible={scheduleFormVisible}
        initialDatasetId={selectedDatasetId}
        onCancel={handleScheduleCancel}
        onSubmit={handleScheduleSubmit}
      />
    </div>
  );
};

export default ReportsPage;
