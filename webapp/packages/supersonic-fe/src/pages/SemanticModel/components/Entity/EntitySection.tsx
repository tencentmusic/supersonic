import { message, Space } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getDomainExtendConfig, getDomainExtendDetailConfig } from '../../service';
import ProCard from '@ant-design/pro-card';
import EntityCreateForm from './EntityCreateForm';
import MetricSettingForm from './MetricSettingForm';
import type { IChatConfig } from '../../data';
import DimensionMetricVisibleForm from './DimensionMetricVisibleForm';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const EntitySection: React.FC<Props> = ({ domainManger, dispatch }) => {
  const { selectDomainId, dimensionList, metricList } = domainManger;

  const [entityData, setEntityData] = useState<IChatConfig.IEntity>({} as IChatConfig.IEntity);

  const [themeData, setThemeData] = useState<any>({});

  const entityCreateRef = useRef<any>({});

  const queryThemeListData: any = async () => {
    const { code, data } = await getDomainExtendDetailConfig({
      domainId: selectDomainId,
    });
    // getDomainExtendConfig({
    //   domainId: selectDomainId,
    // });
    if (code === 200) {
      const target = data;
      if (target) {
        setThemeData(target);
        setEntityData({
          id: target.id,
          ...target.entity,
        });
      }
      return;
    }
    message.error('获取主题域解析词失败');
  };

  const initPage = async () => {
    queryThemeListData();
  };

  useEffect(() => {
    initPage();
  }, [selectDomainId]);

  return (
    <div style={{ width: 800, margin: '0 auto' }}>
      <Space direction="vertical" style={{ width: '100%' }} size={20}>
        <ProCard bordered title="问答可见">
          <DimensionMetricVisibleForm
            themeData={themeData}
            domainId={Number(selectDomainId)}
            metricList={metricList}
            dimensionList={dimensionList}
            onSubmit={(params: any = {}) => {
              if (params.from === 'dimensionSearchVisible') {
                dispatch({
                  type: 'domainManger/queryDimensionList',
                  payload: {
                    domainId: selectDomainId,
                  },
                });
              }
              queryThemeListData();
            }}
          />
        </ProCard>
        <ProCard bordered title="默认指标">
          <MetricSettingForm
            domainId={Number(selectDomainId)}
            themeData={themeData}
            // metricList={metricList}
            metricList={metricList.filter((item) => {
              const blackMetricIdList = themeData.visibility?.blackMetricIdList;
              if (Array.isArray(blackMetricIdList)) {
                return !blackMetricIdList.includes(item.id);
              }
              return false;
            })}
            onSubmit={() => {
              queryThemeListData();
            }}
          />
        </ProCard>
        <ProCard title="实体" bordered>
          <EntityCreateForm
            ref={entityCreateRef}
            domainId={Number(selectDomainId)}
            entityData={entityData}
            // metricList={metricList}
            metricList={metricList.filter((item) => {
              const blackMetricIdList = themeData.visibility?.blackMetricIdList;
              if (Array.isArray(blackMetricIdList)) {
                return !blackMetricIdList.includes(item.id);
              }
              return false;
            })}
            // dimensionList={dimensionList}
            dimensionList={dimensionList.filter((item) => {
              const blackDimensionList = themeData.visibility?.blackDimIdList;
              if (Array.isArray(blackDimensionList)) {
                return !blackDimensionList.includes(item.id);
              }
              return false;
            })}
            onSubmit={() => {
              queryThemeListData();
            }}
          />
        </ProCard>
      </Space>
    </div>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(EntitySection);
