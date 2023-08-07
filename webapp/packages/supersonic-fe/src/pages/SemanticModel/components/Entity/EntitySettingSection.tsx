import { message, Space } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { getDomainDetail } from '../../service';
import ProCard from '@ant-design/pro-card';
import EntityCreateForm from './EntityCreateForm';
import type { ISemantic } from '../../data';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const EntitySettingSection: React.FC<Props> = ({ domainManger }) => {
  const { selectDomainId, dimensionList } = domainManger;

  const [domainData, setDomainData] = useState<ISemantic.IDomainItem>();

  const entityCreateRef = useRef<any>({});

  const queryDomainData: any = async () => {
    const { code, data } = await getDomainDetail({
      domainId: selectDomainId,
    });

    if (code === 200) {
      setDomainData(data);

      return;
    }

    message.error('获取问答设置信息失败');
  };

  const initPage = async () => {
    queryDomainData();
  };

  useEffect(() => {
    initPage();
  }, [selectDomainId]);

  return (
    <div style={{ width: 800, margin: '0 auto' }}>
      <Space direction="vertical" style={{ width: '100%' }} size={20}>
        {
          <ProCard title="实体" bordered>
            <EntityCreateForm
              ref={entityCreateRef}
              domainId={Number(selectDomainId)}
              domainData={domainData}
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
