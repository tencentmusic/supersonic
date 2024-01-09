import { message, Tag, Space, Tooltip } from 'antd';
import React, { useState, useEffect, ReactNode } from 'react';
import { getMetricData } from '../service';
import { connect, useParams } from 'umi';
import type { StateType } from '../model';
import dayjs from 'dayjs';
import {
  ExportOutlined,
  SolutionOutlined,
  ContainerOutlined,
  PartitionOutlined,
  FallOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import styles from './style.less';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_COLOR } from '../constant';
import { ISemantic } from '../data';
import MetricStar from './components/MetricStar';

type Props = {
  metircData: any;
  domainManger: StateType;
  onNodeChange: (params?: { eventName?: string }) => void;
  onEditBtnClick?: (metircData: any) => void;
  onDimensionRelationBtnClick?: () => void;
  [key: string]: any;
};

const MetricInfoSider: React.FC<Props> = ({ onDimensionRelationBtnClick }) => {
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

  return (
    <div className={styles.metricInfoSider}>
      <div className={styles.title}>
        <div className={styles.name}>
          <Space>
            <MetricStar metricId={metricId} initState={metircData?.isCollect} />
            {metircData?.name}
            {metircData?.alias && `[${metircData.alias}]`}
            {metircData?.hasAdminRes && (
              <span
                className={styles.gotoMetricListIcon}
                onClick={() => {
                  window.open(`/webapp/model/${metircData.domainId}/${metircData.modelId}/`);
                }}
              >
                <Tooltip title="前往所属模型指标列表">
                  <ExportOutlined />
                </Tooltip>
              </span>
            )}

            {metircData?.sensitiveLevel !== undefined && (
              <span style={{ marginLeft: 25 }}>
                <Tag color={SENSITIVE_LEVEL_COLOR[metircData.sensitiveLevel]}>
                  {SENSITIVE_LEVEL_ENUM[metircData.sensitiveLevel]}
                </Tag>
              </span>
            )}
          </Space>
        </div>
        {metircData?.bizName && <div className={styles.bizName}>{metircData.bizName}</div>}
      </div>

      <div className={styles.sectionContainer}>
        <div className={styles.section}>
          <div className={styles.sectionTitleBox}>
            <span className={styles.sectionTitle}>
              <Space>
                <ContainerOutlined />
                基本信息
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
                      window.open(`/webapp/model/${metircData.domainId}/0/overview`);
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
            <span className={styles.itemLable}>描述: </span>
            <span className={styles.itemValue}>{metircData?.description}</span>
          </div>
        </div>

        <div className={styles.section}>
          <div className={styles.sectionTitleBox}>
            <span className={styles.sectionTitle}>
              <Space>
                <SolutionOutlined />
                创建信息
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
        <hr className={styles.hr} />
        <div className={styles.ctrlBox}>
          <ul className={styles.ctrlList}>
            <li
              onClick={() => {
                onDimensionRelationBtnClick?.();
              }}
            >
              <Tooltip title="配置下钻维度后，将可以在指标卡中进行下钻">
                <Space style={{ width: '100%' }}>
                  <span className={styles.ctrlItemIcon}>
                    <FallOutlined />
                  </span>
                  <span className={styles.ctrlItemLable}>下钻维度配置</span>
                </Space>
              </Tooltip>
            </li>
            {/* <li
              onClick={() => {
                onDimensionRelationBtnClick?.();
              }}
            >
              <Space style={{ width: '100%' }}>
                <span className={styles.ctrlItemIcon}>
                  <DeleteOutlined />
                </span>
                <span className={styles.ctrlItemLable}>删除</span>
              </Space>
            </li> */}
          </ul>
        </div>
      </div>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(MetricInfoSider);
