import { Space } from 'antd';
import React from 'react';
import type { Dispatch } from 'umi';
import { connect } from 'umi';
import type { StateType } from '../../model';
import { ProCard } from '@ant-design/pro-card';
import PermissionTable from './PermissionTable';
import PermissionAdminForm from './PermissionAdminForm';

type Props = {
  dispatch: Dispatch;
  domainManger: StateType;
};

const PermissionSection: React.FC<Props> = () => {
  return (
    <>
      <div>
        <Space direction="vertical" style={{ width: '100%' }} size={20}>
          <ProCard title="邀请成员" bordered>
            <PermissionAdminForm />
          </ProCard>

          <PermissionTable />
        </Space>
      </div>
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(PermissionSection);
