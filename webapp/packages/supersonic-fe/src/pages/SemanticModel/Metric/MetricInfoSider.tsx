import { Tag, Space, Tooltip } from 'antd';
import React from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
import { isArrayOfValues } from '@/utils/utils';
import dayjs from 'dayjs';
import {
  ExportOutlined,
  SolutionOutlined,
  ContainerOutlined,
  PartitionOutlined,
  PlusOutlined,
  AreaChartOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import styles from './style.less';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_COLOR } from '../constant';
import { ISemantic } from '../data';
import MetricStar from './components/MetricStar';

type Props = {
  metircData: ISemantic.IMetricItem;
  domainManger: StateType;
  relationDimensionOptions: { value: string; label: string; modelId: number }[];
  onNodeChange: (params?: { eventName?: string }) => void;
  onEditBtnClick?: (metircData: any) => void;
  onDimensionRelationBtnClick?: () => void;
  [key: string]: any;
};

const MetricInfoSider: React.FC<Props> = ({
  metircData,
  relationDimensionOptions,
  onDimensionRelationBtnClick,
}) => {
  return (
    <div className={styles.metricInfoSider}>
      <div className={styles.title}>
        <div className={styles.name}>
          <Space>
            <MetricStar metricId={metircData?.id} initState={metircData?.isCollect} />
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
          </Space>
        </div>
        {metircData?.bizName && <div className={styles.bizName}>{metircData.bizName}</div>}
      </div>

      <div className={styles.sectionContainer}>
        <hr className={styles.hr} />
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
            <span className={styles.itemLable}>敏感度: </span>
            <span className={styles.itemValue}>
              {metircData?.sensitiveLevel !== undefined && (
                <span>
                  <Tag color={SENSITIVE_LEVEL_COLOR[metircData.sensitiveLevel]}>
                    {SENSITIVE_LEVEL_ENUM[metircData.sensitiveLevel]}
                  </Tag>
                </span>
              )}
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
        <hr className={styles.hr} />
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

        <div className={styles.section}>
          <div className={styles.sectionTitleBox}>
            <span className={styles.sectionTitle}>
              <Space>
                <AreaChartOutlined />
                应用信息
              </Space>
            </span>
          </div>

          {isArrayOfValues(metircData?.tags) && (
            <div className={styles.item}>
              <span className={styles.itemLable}>标签: </span>
              <span className={styles.itemValue}>
                <Space size={2} wrap>
                  {metircData?.tags.map((tag) => (
                    <Tag color="blue" key={tag}>
                      {tag}
                    </Tag>
                  ))}
                </Space>
              </span>
            </div>
          )}
        </div>

        <div className={styles.ctrlBox}>
          <ul className={styles.ctrlList}>
            <Tooltip title="配置下钻维度后，将可以在指标卡中进行下钻">
              <li
                style={{ display: 'block' }}
                onClick={() => {
                  onDimensionRelationBtnClick?.();
                }}
              >
                <Space style={{ width: '100%' }}>
                  <div className={styles.subTitle}>下钻维度</div>
                  <span className={styles.ctrlItemIcon}>
                    <PlusOutlined />
                  </span>
                </Space>
                {isArrayOfValues(relationDimensionOptions) && (
                  <div style={{ marginLeft: 0, marginTop: 20 }}>
                    <Space size={5} wrap>
                      {relationDimensionOptions.map((item) => (
                        <Tag color="blue" key={item.value} style={{ marginRight: 0 }}>
                          {item.label}
                        </Tag>
                      ))}
                    </Space>
                  </div>
                )}
              </li>
            </Tooltip>
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
