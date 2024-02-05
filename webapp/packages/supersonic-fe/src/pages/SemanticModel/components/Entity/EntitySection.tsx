import { message, Space } from 'antd';
import { useEffect, useState, forwardRef, useImperativeHandle, Ref } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getDomainExtendDetailConfig } from '../../service';
import ProCard from '@ant-design/pro-card';

import DefaultSettingForm from './DefaultSettingForm';
import type { IChatConfig } from '../../data';
import { ChatConfigType } from '../../enum';

type Props = {
  chatConfigType: ChatConfigType.TAG | ChatConfigType.METRIC;
  onConfigSave?: () => void;
  dispatch: Dispatch;
  domainManger: StateType;
};

const EntitySection: React.FC<Props> = forwardRef(
  ({ domainManger, chatConfigType = ChatConfigType.TAG, onConfigSave }, ref: Ref<any>) => {
    const { selectDomainId, selectModelId: modelId, dimensionList, metricList } = domainManger;

    const [entityData, setEntityData] = useState<IChatConfig.IChatRichConfig>();

    useImperativeHandle(ref, () => ({
      refreshConfigData: queryThemeListData,
    }));

    const queryThemeListData: any = async () => {
      const { code, data } = await getDomainExtendDetailConfig({
        modelId,
      });

      if (code === 200) {
        const { chatAggRichConfig, chatDetailRichConfig, id, domainId, modelId } = data;
        if (chatConfigType === ChatConfigType.TAG) {
          setEntityData({ ...chatDetailRichConfig, id, domainId, modelId });
        }
        if (chatConfigType === ChatConfigType.METRIC) {
          setEntityData({ ...chatAggRichConfig, id, domainId, modelId });
        }
        return;
      }

      message.error('获取问答设置信息失败');
    };

    const initPage = async () => {
      queryThemeListData();
    };

    useEffect(() => {
      if (!modelId) {
        return;
      }
      initPage();
    }, [modelId]);

    return (
      <div style={{ width: 800, margin: '0 auto' }}>
        <Space direction="vertical" style={{ width: '100%' }} size={20}>
          <ProCard bordered title="默认设置">
            <DefaultSettingForm
              domainId={Number(selectDomainId)}
              entityData={entityData || {}}
              chatConfigType={chatConfigType}
              chatConfigKey={
                chatConfigType === ChatConfigType.TAG ? 'chatDetailConfig' : 'chatAggConfig'
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
                onConfigSave?.();
              }}
            />
          </ProCard>
        </Space>
      </div>
    );
  },
);
export default connect(
  ({ domainManger }: { domainManger: StateType }) => ({
    domainManger,
  }),
  () => {},
  null,
  { forwardRef: true },
)(EntitySection);
