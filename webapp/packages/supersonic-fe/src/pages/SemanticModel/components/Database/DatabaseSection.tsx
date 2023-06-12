import { Space } from 'antd';
import React, { useRef } from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import ProCard from '@ant-design/pro-card';
import DatabaseCreateForm from './DatabaseCreateForm';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const DatabaseSection: React.FC<Props> = ({ domainManger }) => {
  const { selectDomainId } = domainManger;

  const entityCreateRef = useRef<any>({});

  return (
    <div style={{ width: 800, margin: '0 auto' }}>
      <Space direction="vertical" style={{ width: '100%' }} size={20}>
        <ProCard title="数据库设置" bordered>
          <DatabaseCreateForm
            ref={entityCreateRef}
            domainId={Number(selectDomainId)}
            onSubmit={() => {}}
          />
        </ProCard>
      </Space>
    </div>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DatabaseSection);
