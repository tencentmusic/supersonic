import { Tag, Space, Tooltip } from 'antd';
import React, { ReactNode } from 'react';
import dayjs from 'dayjs';
import { basePath } from '../../../../../config/defaultSettings';
import {
  ExportOutlined,
  SolutionOutlined,
  ContainerOutlined,
  PartitionOutlined,
  AreaChartOutlined,
} from '@ant-design/icons';
import styles from '../style.less';
import {
  SENSITIVE_LEVEL_ENUM,
  SENSITIVE_LEVEL_COLOR,
  TAG_DEFINE_TYPE,
  TagDefineTypeMap,
} from '../../constant';

import { ISemantic } from '../../data';
import IndicatorStar from '../../components/IndicatorStar';

type Props = {
  tagData: ISemantic.ITagItem;
  onNodeChange: (params?: { eventName?: string }) => void;
  onEditBtnClick?: (tagData: any) => void;
  onDimensionRelationBtnClick?: () => void;
  dimensionMap: Record<string, ISemantic.IDimensionItem>;
  metricMap: Record<string, ISemantic.IMetricItem>;
  [key: string]: any;
};

const TagInfoSider: React.FC<Props> = ({ tagData, dimensionMap, metricMap }) => {
  const tagDefineDependenciesRender = () => {
    if (!tagData) {
      return <></>;
    }
    const { tagDefineType, tagDefineParams = {} } = tagData;
    const { dependencies } = tagDefineParams as any;
    if (!Array.isArray(dependencies)) {
      return <></>;
    }

    if (tagDefineType === TAG_DEFINE_TYPE.DIMENSION) {
      return dependencies.reduce((nodes: ReactNode[], id) => {
        const target = dimensionMap[id];
        if (target) {
          nodes.push(
            <Tag color="blue" key={id}>
              {target.name}
            </Tag>,
          );
        }
        return nodes;
      }, []);
    }

    if (tagDefineType === TAG_DEFINE_TYPE.METRIC) {
      return dependencies.reduce((nodes: ReactNode[], id) => {
        const target = metricMap[id];
        if (target) {
          nodes.push(
            <Tag color="blue" key={id}>
              {target.name}
            </Tag>,
          );
        }
        return nodes;
      }, []);
    }

    if (tagDefineType === TAG_DEFINE_TYPE.FIELD) {
      return dependencies.map((fieldName) => (
        <Tag color="blue" key={fieldName}>
          {fieldName}
        </Tag>
      ));
    }
    return <></>;
  };

  return (
    <div className={styles.metricInfoSider}>
      <div className={styles.title}>
        <div className={styles.name}>
          <Space>
            <IndicatorStar indicatorId={tagData?.id} initState={tagData?.isCollect} />
            {tagData?.name}
            {tagData?.hasAdminRes && (
              <span
                className={styles.gotoMetricListIcon}
                onClick={() => {
                  window.open(`${basePath}model/${tagData.domainId}/${tagData.modelId}/`);
                }}
              >
                <Tooltip title="前往所属模3型指标列表">
                  <ExportOutlined />
                </Tooltip>
              </span>
            )}
          </Space>
        </div>
        {tagData?.bizName && <div className={styles.bizName}>{tagData.bizName}</div>}
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
              {tagData?.sensitiveLevel !== undefined && (
                <span>
                  <Tag color={SENSITIVE_LEVEL_COLOR[tagData.sensitiveLevel]}>
                    {SENSITIVE_LEVEL_ENUM[tagData.sensitiveLevel]}
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
                  {tagData?.modelName || '模型名为空'}
                </Tag>
                {tagData?.hasAdminRes && (
                  <span
                    className={styles.gotoMetricListIcon}
                    onClick={() => {
                      window.open(`${basePath}model/${tagData.domainId}/0/overview`);
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
            <span className={styles.itemValue}>{tagData?.description}</span>
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
            <span className={styles.itemValue}>{tagData?.createdBy}</span>
          </div>
          <div className={styles.item}>
            <span className={styles.itemLable}>创建时间: </span>
            <span className={styles.itemValue}>
              {tagData?.createdAt ? dayjs(tagData?.createdAt).format('YYYY-MM-DD HH:mm:ss') : ''}
            </span>
          </div>
          <div className={styles.item}>
            <span className={styles.itemLable}>更新时间: </span>
            <span className={styles.itemValue}>
              {tagData?.createdAt ? dayjs(tagData?.updatedAt).format('YYYY-MM-DD HH:mm:ss') : ''}
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

          <div className={styles.item}>
            <span className={styles.itemLable}>创建来源:</span>
            <span className={styles.itemValue}>
              <span style={{ color: '#3182ce' }}>
                {TagDefineTypeMap[TAG_DEFINE_TYPE[tagData?.tagDefineType]]}
              </span>
            </span>
          </div>

          <div className={styles.item}>
            <span className={styles.itemValue}>
              <Space size={2} wrap>
                {tagDefineDependenciesRender()}
              </Space>
            </span>
          </div>
        </div>

        {/* <div className={styles.ctrlBox}>
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
          </ul>
        </div> */}
      </div>
    </div>
  );
};

export default TagInfoSider;
