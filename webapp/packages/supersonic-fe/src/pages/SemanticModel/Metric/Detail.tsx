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

// type InfoListItemChildrenItem = {
//   label: string;
//   value: string;
//   content?: ReactNode;
//   hideItem?: boolean;
// };

// type InfoListItem = {
//   title: string;
//   hideItem?: boolean;
//   render?: () => ReactNode;
//   children?: InfoListItemChildrenItem[];
// };

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
  // const bizName = params.bizName;

  // const [infoList, setInfoList] = useState<InfoListItem[]>([]);
  // const { selectModelName } = domainManger;
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

  // useEffect(() => {
  //   if (!metircData) {
  //     return;
  //   }
  //   const {
  //     alias,
  //     bizName,
  //     createdBy,
  //     createdAt,
  //     updatedAt,
  //     description,
  //     sensitiveLevel,
  //     modelName,
  //   } = metircData;

  //   const list = [
  //     {
  //       title: '基本信息',
  //       children: [
  //         {
  //           label: '字段名称',
  //           value: bizName,
  //         },
  //         {
  //           label: '别名',
  //           hideItem: !alias,
  //           value: alias || '-',
  //         },
  //         {
  //           label: '所属模型',
  //           value: modelName,
  //           content: <Tag>{modelName || selectModelName}</Tag>,
  //         },

  //         {
  //           label: '描述',
  //           value: description,
  //         },
  //       ],
  //     },
  //     {
  //       title: '应用信息',
  //       children: [
  //         {
  //           label: '敏感度',
  //           value: SENSITIVE_LEVEL_ENUM[sensitiveLevel],
  //         },
  //       ],
  //     },
  //     // {
  //     //   title: '指标趋势',
  //     //   render: () => (
  //     //     <Row key={`metricTrendSection`} style={{ marginBottom: 10, display: 'flex' }}>
  //     //       <Col span={24}>
  //     //         <MetricTrendSection nodeData={metircData} />
  //     //       </Col>
  //     //     </Row>
  //     //   ),
  //     // },
  //     {
  //       title: '创建信息',
  //       children: [
  //         {
  //           label: '创建人',
  //           value: createdBy,
  //         },
  //         {
  //           label: '创建时间',
  //           value: createdAt ? moment(createdAt).format('YYYY-MM-DD HH:mm:ss') : '',
  //         },
  //         {
  //           label: '更新时间',
  //           value: updatedAt ? moment(updatedAt).format('YYYY-MM-DD HH:mm:ss') : '',
  //         },
  //       ],
  //     },
  //   ];

  //   setInfoList(list);
  // }, [metircData]);

  const tabItems: TabsProps['items'] = [
    {
      key: 'metricTrend',
      label: '图表',
      children: <MetricTrendSection metircData={metircData} />,
    },
    // {
    //   key: '2',
    //   label: 'Tab 2',
    //   children: 'Content of Tab Pane 2',
    // },
  ];

  const contentStyle: React.CSSProperties = {
    minHeight: 120,
    color: '#fff',
    // marginRight: 15,
    backgroundColor: '#fff',
  };

  // const siderStyle: React.CSSProperties = {
  //   width: '300px',
  //   backgroundColor: '#fff',
  // };

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
                  <span style={{ color: '#296DF3' }}>
                    {metircData?.name}
                    {metircData?.alias && `[${metircData.alias}]`}
                  </span>
                  {metircData?.name && (
                    <>
                      <span style={{ position: 'relative', top: '-2px' }}> | </span>
                      <span style={{ fontSize: 16, color: '#296DF3' }}>{metircData?.bizName}</span>
                    </>
                  )}
                  {metircData?.sensitiveLevel !== undefined && (
                    <span style={{ position: 'relative', top: '-2px' }}>
                      <Tag color={SENSITIVE_LEVEL_COLOR[metircData.sensitiveLevel]}>
                        {SENSITIVE_LEVEL_ENUM[metircData.sensitiveLevel]}敏感度
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
                  {/* <div className={styles.info}>
                    <DescriptionItem
                      title="所属模型"
                      icon={<UserOutlined />}
                      content={metircData?.modelName}
                    />

                    <DescriptionItem
                      title="更新时间"
                      icon={<CalendarOutlined />}
                      content={metircData?.description}
                    />
                  </div> */}
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
      {/* <Sider style={siderStyle} width={400}>
        <>
          <div key={metircData?.id} className={styles.metricInfoContent}>
            {infoList.map((item) => {
              const { children, title, render } = item;
              return (
                <div key={title} style={{ display: item.hideItem ? 'none' : 'block' }}>
                  <p className={styles.title}>{title}</p>
                  {render?.() ||
                    (Array.isArray(children) &&
                      children.map((childrenItem) => {
                        return (
                          <Row
                            key={`${childrenItem.label}-${childrenItem.value}`}
                            style={{
                              marginBottom: 10,
                              display: childrenItem.hideItem ? 'none' : 'flex',
                            }}
                          >
                            <Col span={24}>
                              <DescriptionItem
                                title={childrenItem.label}
                                content={childrenItem.content || childrenItem.value}
                              />
                            </Col>
                          </Row>
                        );
                      }))}
                  <Divider />
                </div>
              );
            })}
          </div>
        </>
      </Sider> */}
    </Layout>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(MetricDetail);
