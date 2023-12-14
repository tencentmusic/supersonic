import React, { useRef } from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
import ProCard from '@ant-design/pro-card';
import EntitySection from '../components/Entity/EntitySection';
import { ChatConfigType } from '../enum';
import type { Dispatch } from 'umi';

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};

const ChatSettingSection: React.FC<Props> = () => {
  const metricRef = useRef<any>();
  const tagRef = useRef<any>();
  return (
    <div style={{ width: 900, margin: '20px auto' }}>
      <ProCard bordered title="指标模式" style={{ marginBottom: 20 }}>
        <EntitySection
          ref={metricRef}
          chatConfigType={ChatConfigType.AGG}
          onConfigSave={() => {
            tagRef.current.refreshConfigData();
          }}
        />
      </ProCard>
      <ProCard bordered title="标签模式" style={{ marginBottom: 20 }}>
        <EntitySection
          ref={tagRef}
          chatConfigType={ChatConfigType.DETAIL}
          onConfigSave={() => {
            metricRef.current.refreshConfigData();
          }}
        />
      </ProCard>
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ChatSettingSection);
