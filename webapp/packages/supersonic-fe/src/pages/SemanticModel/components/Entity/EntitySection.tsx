import { message, Space } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getDomainExtendDetailConfig } from '../../service';
import ProCard from '@ant-design/pro-card';

import DefaultSettingForm from './DefaultSettingForm';
import type { IChatConfig } from '../../data';
import DimensionMetricVisibleForm from './DimensionMetricVisibleForm';
import { ChatConfigType } from '../../enum';

type Props = {
  chatConfigType: ChatConfigType.DETAIL | ChatConfigType.AGG;
  dispatch: Dispatch;
  domainManger: StateType;
};

const EntitySection: React.FC<Props> = ({
  domainManger,
  dispatch,
  chatConfigType = ChatConfigType.DETAIL,
}) => {
  const { selectDomainId, dimensionList, metricList } = domainManger;

  const [entityData, setentityData] = useState<IChatConfig.IChatRichConfig>();

  const queryThemeListData: any = async () => {
    const { code, data } = await getDomainExtendDetailConfig({
      domainId: selectDomainId,
    });

    if (code === 200) {
      const { chatAggRichConfig, chatDetailRichConfig, id, domainId } = data;
      if (chatConfigType === ChatConfigType.DETAIL) {
        setentityData({ ...chatDetailRichConfig, id, domainId });
      }
      if (chatConfigType === ChatConfigType.AGG) {
        setentityData({ ...chatAggRichConfig, id, domainId });
      }
      return;
    }

    message.error('获取问答设置信息失败');
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
            chatConfigKey={
              chatConfigType === ChatConfigType.DETAIL ? 'chatDetailConfig' : 'chatAggConfig'
            }
            entityData={entityData || {}}
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
        <ProCard bordered title="默认设置">
          <DefaultSettingForm
            domainId={Number(selectDomainId)}
            entityData={entityData || {}}
            chatConfigType={chatConfigType}
            chatConfigKey={
              chatConfigType === ChatConfigType.DETAIL ? 'chatDetailConfig' : 'chatAggConfig'
            }
            dimensionList={dimensionList.filter((item) => {
              const blackDimensionList = entityData?.visibility?.blackDimIdList;
              if (Array.isArray(blackDimensionList)) {
                return !blackDimensionList.includes(item.id);
              }
              return false;
            })}
            metricList={metricList.filter((item) => {
              const blackMetricIdList = entityData?.visibility?.blackMetricIdList;
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
      </Space>
    </div>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(EntitySection);
