import { Tag, Space, Tooltip } from 'antd';
import React, { useState, useEffect } from 'react';
import dayjs from 'dayjs';
import {
  ExportOutlined,
  SolutionOutlined,
  PartitionOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import styles from './style.less';
import IndicatorStar from '../IndicatorStar';
import { toDomainList, toModelList } from '@/pages/SemanticModel/utils';
import { MenuItem } from './type';

type Props = {
  detailData: any;
  menuKey: string;
  menuList: MenuItem[];
  onMenuKeyChange?: (key: string, item: MenuItem) => void;
};

const DetailSider: React.FC<Props> = ({ detailData, menuList, menuKey, onMenuKeyChange }) => {
  const [settingKey, setSettingKey] = useState<string>(menuKey);

  useEffect(() => {
    if (menuKey) {
      setSettingKey(menuKey);
    }
  }, [menuKey]);

  return (
    <div className={styles.DetailInfoSider}>
      <div className={styles.sectionContainer}>
        {detailData?.id ? (
          <div className={styles.title}>
            <div className={styles.name}>
              <Space>
                {detailData?.isCollect !== undefined ? (
                  <IndicatorStar indicatorId={detailData?.id} initState={detailData?.isCollect} />
                ) : (
                  <div style={{ width: 15 }}></div>
                )}

                {detailData?.name}
                {detailData?.hasAdminRes && (
                  <span
                    className={styles.gotoMetricListIcon}
                    onClick={() => {
                      toModelList(detailData.domainId, detailData.modelId);
                    }}
                  >
                    <Tooltip title="前往所属模型指标列表">
                      <ExportOutlined />
                    </Tooltip>
                  </span>
                )}
              </Space>
            </div>
            {detailData?.bizName && <div className={styles.bizName}>{detailData.bizName}</div>}
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
            {menuList.map((item) => {
              return (
                <li
                  className={item.key === settingKey ? styles.active : ''}
                  key={item.key}
                  onClick={() => {
                    onMenuKeyChange?.(item.key, item);
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
        {detailData?.id && (
          <div className={styles.section} style={{ marginTop: 'auto' }}>
            <div className={styles.sectionTitleBox}>
              <span className={styles.sectionTitle}>
                <Space>
                  <SolutionOutlined />
                  创建信息
                </Space>
              </span>
            </div>
            {detailData?.modelName && (
              <div className={styles.item}>
                <span className={styles.itemLable}>所属模型: </span>
                <span className={styles.itemValue}>
                  <Space>
                    <Tag icon={<PartitionOutlined />} color="#3b5999">
                      {detailData?.modelName || '模型名为空'}
                    </Tag>
                    {detailData?.hasAdminRes && (
                      <span
                        className={styles.gotoMetricListIcon}
                        onClick={() => {
                          toDomainList(detailData.domainId, 'overview');
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
            )}

            <div className={styles.item}>
              <span className={styles.itemLable}>创建人: </span>
              <span className={styles.itemValue}>{detailData?.createdBy}</span>
            </div>
            <div className={styles.item}>
              <span className={styles.itemLable}>创建时间: </span>
              <span className={styles.itemValue}>
                {detailData?.createdAt
                  ? dayjs(detailData?.createdAt).format('YYYY-MM-DD HH:mm:ss')
                  : ''}
              </span>
            </div>
            <div className={styles.item}>
              <span className={styles.itemLable}>更新时间: </span>
              <span className={styles.itemValue}>
                {detailData?.createdAt
                  ? dayjs(detailData?.updatedAt).format('YYYY-MM-DD HH:mm:ss')
                  : ''}
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DetailSider;
