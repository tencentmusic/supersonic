import { Tag, Space, Tooltip } from 'antd';
import React, { useState } from 'react';
import dayjs from 'dayjs';
import { MetricSettingKey, MetricSettingWording } from './constants';
import { basePath } from '../../../../config/defaultSettings';
import {
  ExportOutlined,
  SolutionOutlined,
  PartitionOutlined,
  ProjectOutlined,
  ConsoleSqlOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import styles from './style.less';
import { ISemantic } from '../data';
import IndicatorStar from '../components/IndicatorStar';

type Props = {
  metircData: ISemantic.IMetricItem;
  onSettingKeyChange?: (key: MetricSettingKey) => void;
};

const MetricInfoEditSider: React.FC<Props> = ({ metircData, onSettingKeyChange }) => {
  const [settingKey, setSettingKey] = useState<MetricSettingKey>(MetricSettingKey.BASIC);

  const settingList = [
    {
      icon: <ProjectOutlined />,
      key: MetricSettingKey.BASIC,
      text: MetricSettingWording[MetricSettingKey.BASIC],
    },
    {
      icon: <ConsoleSqlOutlined />,
      key: MetricSettingKey.SQL_CONFIG,
      text: MetricSettingWording[MetricSettingKey.SQL_CONFIG],
    },
    // {
    //   icon: <DashboardOutlined />,
    //   key: MetricSettingKey.DIMENSION_CONFIG,
    //   text: MetricSettingWording[MetricSettingKey.DIMENSION_CONFIG],
    // },
  ];

  return (
    <div className={styles.metricInfoSider}>
      <div className={styles.sectionContainer}>
        {metircData?.id ? (
          <div className={styles.title}>
            <div className={styles.name}>
              <Space>
                <IndicatorStar indicatorId={metircData?.id} initState={metircData?.isCollect} />
                {metircData?.name}
                {metircData?.hasAdminRes && (
                  <span
                    className={styles.gotoMetricListIcon}
                    onClick={() => {
                      window.open(`${basePath}model/${metircData.domainId}/${metircData.modelId}/`);
                    }}
                  >
                    <Tooltip title="前往所属模型指标列表">
                      <ExportOutlined />
                    </Tooltip>
                  </span>
                )}
              </Space>
            </div>
            {metircData?.bizName && <div className={styles.bizName}>{metircData.bizName}</div>}
          </div>
        ) : (
          <div className={styles.createTitle}>
            <Space>
              <SettingOutlined />
              新建指标
            </Space>
          </div>
        )}

        <hr className={styles.hr} />
        <div className={styles.section} style={{ padding: '16px 0' }}>
          <ul className={styles.settingList}>
            {settingList.map((item) => {
              return (
                <li
                  className={item.key === settingKey ? styles.active : ''}
                  key={item.key}
                  onClick={() => {
                    onSettingKeyChange?.(item.key);
                    setSettingKey(item.key);
                  }}
                >
                  <div className={styles.icon}>{item.icon}</div>
                  <div className={styles.content}>
                    <span className={styles.text}> {item.text}</span>
                  </div>
                </li>
              );
            })}
          </ul>
        </div>
        {/* <hr className={styles.hr} /> */}
        {metircData?.id && (
          <div className={styles.section} style={{ marginTop: 'auto' }}>
            <div className={styles.sectionTitleBox}>
              <span className={styles.sectionTitle}>
                <Space>
                  <SolutionOutlined />
                  创建信息
                </Space>
              </span>
            </div>
            <div className={styles.item}>
              <span className={styles.itemLable}>所属模型: </span>
              <span className={styles.itemValue}>
                <Space>
                  <Tag icon={<PartitionOutlined />} color="#3b5999">
                    {metircData?.modelName || '模型名为空'}
                  </Tag>
                  {metircData?.hasAdminRes && (
                    <span
                      className={styles.gotoMetricListIcon}
                      onClick={() => {
                        window.open(`${basePath}model/${metircData.domainId}/0/overview`);
                      }}
                    >
                      <Tooltip title="前往模型设置页">
                        <ExportOutlined />
                      </Tooltip>
                    </span>
                  )}
                </Space>
              </span>
            </div>
            <div className={styles.item}>
              <span className={styles.itemLable}>创建人: </span>
              <span className={styles.itemValue}>{metircData?.createdBy}</span>
            </div>
            <div className={styles.item}>
              <span className={styles.itemLable}>创建时间: </span>
              <span className={styles.itemValue}>
                {metircData?.createdAt
                  ? dayjs(metircData?.createdAt).format('YYYY-MM-DD HH:mm:ss')
                  : ''}
              </span>
            </div>
            <div className={styles.item}>
              <span className={styles.itemLable}>更新时间: </span>
              <span className={styles.itemValue}>
                {metircData?.createdAt
                  ? dayjs(metircData?.updatedAt).format('YYYY-MM-DD HH:mm:ss')
                  : ''}
              </span>
            </div>
          </div>
        )}

        {/* <hr className={styles.hr} /> */}
      </div>
    </div>
  );
};

export default MetricInfoEditSider;
