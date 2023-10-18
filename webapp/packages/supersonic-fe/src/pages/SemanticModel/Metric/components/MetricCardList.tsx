import { CheckCard } from '@ant-design/pro-components';
import React, { useState } from 'react';
import { Dropdown, Popconfirm, Typography } from 'antd';
import { EllipsisOutlined } from '@ant-design/icons';
import { ISemantic } from '../../data';
import { connect } from 'umi';
import icon from '../../../../assets/icon/sourceState.svg';
import type { Dispatch } from 'umi';
import type { StateType } from '../../model';
import { SemanticNodeType } from '../../enum';
import styles from '../style.less';

const { Paragraph } = Typography;
type Props = {
  disabledEdit?: boolean;
  metricList: ISemantic.IMetricItem[];
  onMetricChange?: (metricItem: ISemantic.IMetricItem) => void;
  onEditBtnClick?: (metricItem: ISemantic.IMetricItem) => void;
  onDeleteBtnClick?: (metricItem: ISemantic.IMetricItem) => void;
  domainManger: StateType;
  dispatch: Dispatch;
};

const MetricCardList: React.FC<Props> = ({
  metricList,
  disabledEdit = false,
  onMetricChange,
  onEditBtnClick,
  onDeleteBtnClick,
  domainManger,
}) => {
  const [currentNodeData, setCurrentNodeData] = useState<any>({});

  const descNode = (metricItem: ISemantic.IMetricItem) => {
    const { modelName, createdBy } = metricItem;
    return (
      <>
        <div className={styles.overviewExtraContainer}>
          <div className={styles.extraWrapper}>
            <div className={styles.extraStatistic}>
              <div className={styles.extraTitle}>所属模型:</div>
              <div className={styles.extraValue}>
                <Paragraph style={{ maxWidth: 70, margin: 0 }} ellipsis={{ tooltip: modelName }}>
                  <span>{modelName}</span>
                </Paragraph>
              </div>
            </div>
          </div>
          <div className={styles.extraWrapper}>
            <div className={styles.extraStatistic}>
              <div className={styles.extraTitle}>创建人:</div>
              <div className={styles.extraValue}>
                <Paragraph style={{ maxWidth: 70, margin: 0 }} ellipsis={{ tooltip: createdBy }}>
                  <span>{createdBy}</span>
                </Paragraph>
              </div>
            </div>
          </div>
        </div>
      </>
    );
  };

  const extraNode = (metricItem: ISemantic.IMetricItem) => {
    return (
      <Dropdown
        placement="top"
        menu={{
          onClick: ({ key, domEvent }) => {
            domEvent.stopPropagation();
            if (key === 'edit') {
              onEditBtnClick?.(metricItem);
            }
          },
          items: [
            {
              label: '编辑',
              key: 'edit',
            },
            {
              label: (
                <Popconfirm
                  title="确认删除？"
                  okText="是"
                  cancelText="否"
                  onConfirm={() => {
                    onDeleteBtnClick?.(metricItem);
                  }}
                >
                  <a key="modelDeleteBtn">删除</a>
                </Popconfirm>
              ),
              key: 'delete',
            },
          ],
        }}
      >
        <EllipsisOutlined
          style={{ fontSize: 22, color: 'rgba(0,0,0,0.5)' }}
          onClick={(e) => e.stopPropagation()}
        />
      </Dropdown>
    );
  };

  return (
    <div style={{ padding: '0px 20px 20px' }}>
      <CheckCard.Group value={currentNodeData.id} defaultValue={undefined}>
        {metricList &&
          metricList.map((metricItem: ISemantic.IMetricItem) => {
            return (
              <CheckCard
                style={{ width: 350 }}
                avatar={icon}
                title={`${metricItem.name}`}
                key={metricItem.id}
                value={metricItem.id}
                description={descNode(metricItem)}
                extra={!disabledEdit && extraNode(metricItem)}
                onClick={() => {
                  setCurrentNodeData({ ...metricItem, nodeType: SemanticNodeType.METRIC });
                  onMetricChange?.(metricItem);
                }}
              />
            );
          })}
      </CheckCard.Group>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(MetricCardList);
