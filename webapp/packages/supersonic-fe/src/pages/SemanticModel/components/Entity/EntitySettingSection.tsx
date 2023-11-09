import { message, Space } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getModelDetail } from '../../service';
import ProCard from '@ant-design/pro-card';
import EntityCreateForm from './EntityCreateForm';
import type { ISemantic } from '../../data';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const EntitySettingSection: React.FC<Props> = ({ domainManger }) => {
  const { dimensionList, selectModelId: modelId } = domainManger;

  const [modelData, setModelData] = useState<ISemantic.IModelItem>();

  const entityCreateRef = useRef<any>({});

  const queryDomainData: any = async () => {
    const { code, data } = await getModelDetail({
      modelId,
    });

    if (code === 200) {
      setModelData(data);

      return;
    }

    message.error('获取问答设置信息失败');
  };

  const initPage = async () => {
    queryDomainData();
  };

  useEffect(() => {
    initPage();
  }, [modelId]);

  return (
    <div style={{ width: 800, margin: '20px auto' }}>
      <Space direction="vertical" style={{ width: '100%' }} size={20}>
        {
          <ProCard title="实体" bordered>
            <EntityCreateForm
              ref={entityCreateRef}
              modelId={Number(modelId)}
              modelData={modelData}
              dimensionList={dimensionList}
              onSubmit={() => {
                queryDomainData();
              }}
            />
          </ProCard>
        }
      </Space>
    </div>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(EntitySettingSection);
