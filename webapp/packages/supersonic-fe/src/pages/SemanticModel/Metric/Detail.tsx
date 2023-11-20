import { message, Tag, Space, Layout, Tabs, Tooltip } from 'antd';
import React, { useState, useEffect, ReactNode } from 'react';
import { getMetricData } from '../service';
import { connect, useParams, history } from 'umi';
import type { StateType } from '../model';
import moment from 'moment';
import {
  LeftOutlined,
  UserOutlined,
  CalendarOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import styles from './style.less';
import MetricTrendSection from '@/pages/SemanticModel/Metric/components/MetricTrendSection';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_COLOR } from '../constant';
import type { TabsProps } from 'antd';
import { ISemantic } from '../data';
import MetricStar from './components/MetricStar';

const { Content } = Layout;
type Props = {
  metircData: any;
  domainManger: StateType;
  onNodeChange: (params?: { eventName?: string }) => void;
  onEditBtnClick?: (metircData: any) => void;
  [key: string]: any;
};

interface DescriptionItemProps {
  title: string | ReactNode;
  content: React.ReactNode;
  icon: ReactNode;
}

const DescriptionItem = ({ title, content, icon }: DescriptionItemProps) => (
  <Tooltip title={title}>
    <div style={{ width: 'max-content', fontSize: 14, color: '#546174' }}>
      <Space>
        <span style={{ width: 'max-content' }}>{icon}</span>
        {content}
      </Space>
    </div>
  </Tooltip>
);

const MetricDetail: React.FC<Props> = ({ domainManger }) => {
  const params: any = useParams();
  const metricId = params.metricId;

  const [metircData, setMetircData] = useState<ISemantic.IMetricItem>();
  useEffect(() => {
    queryMetricData(metricId);
  }, [metricId]);

  const queryMetricData = async (metricId: string) => {
    const { code, data, msg } = await getMetricData(metricId);
    if (code === 200) {
      setMetircData(data);
      return;
    }
    message.error(msg);
  };

  const tabItems: TabsProps['items'] = [
    {
      key: 'metricTrend',
      label: '图表',
      children: <MetricTrendSection metircData={metircData} />,
    },
  ];

  const contentStyle: React.CSSProperties = {
    minHeight: 120,
    color: '#fff',
    backgroundColor: '#fff',
  };

  return (
    <Layout className={styles.metricDetail}>
      <Layout>
        <Content style={contentStyle}>
          <h2 className={styles.title}>
            <div className={styles.titleLeft}>
              <div
                className={styles.backBtn}
                onClick={() => {
                  history.push(`/metric/market`);
                }}
              >
                <LeftOutlined />
              </div>

              <div className={styles.navContainer}>
                <Space>
                  <MetricStar metricId={metricId} initState={metircData?.isCollect} />
                  <span style={{ color: '#296DF3' }}>
                    {metircData?.name}
                    {metircData?.alias && `[${metircData.alias}]`}
                  </span>
                  {metircData?.name && (
                    <>
                      <span style={{ position: 'relative', top: '-2px', color: '#c3c3c3' }}>|</span>
                      <span style={{ fontSize: 16, color: '#296DF3' }}>{metircData?.bizName}</span>
                    </>
                  )}
                  {metircData?.sensitiveLevel !== undefined && (
                    <span style={{ position: 'relative', top: '-2px' }}>
                      <Tag color={SENSITIVE_LEVEL_COLOR[metircData.sensitiveLevel]}>
                        {SENSITIVE_LEVEL_ENUM[metircData.sensitiveLevel]}
                      </Tag>
                    </span>
                  )}
                </Space>
                {metircData?.description ? (
                  <div className={styles.description}>
                    <Tooltip title="指标描述">
                      <Space>
                        <InfoCircleOutlined />
                        {metircData?.description}
                      </Space>
                    </Tooltip>
                  </div>
                ) : (
                  <></>
                )}
              </div>
            </div>
            <div className={styles.titleRight}>
              {metircData?.createdBy ? (
                <>
                  <div className={styles.info}>
                    <DescriptionItem
                      title="创建人"
                      icon={<UserOutlined />}
                      content={metircData?.createdBy}
                    />

                    <DescriptionItem
                      title="更新时间"
                      icon={<CalendarOutlined />}
                      content={moment(metircData?.updatedAt).format('YYYY-MM-DD HH:mm:ss')}
                    />
                  </div>
                </>
              ) : (
                <></>
              )}
            </div>
          </h2>
          <div className={styles.tabContainer}>
            <Tabs defaultActiveKey="metricTrend" items={tabItems} size="large" />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(MetricDetail);
