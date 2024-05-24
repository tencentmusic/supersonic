import { Space } from 'antd';
import React from 'react';
import { ProCard } from '@ant-design/pro-components';
import PermissionTable from './PermissionTable';
import PermissionAdminForm from './PermissionAdminForm';

type Props = {
  permissionTarget: 'model' | 'domain';
};

const PermissionSection: React.FC<Props> = ({ permissionTarget }) => {
  return (
    <>
      <div>
        <Space direction="vertical" style={{ width: '100%' }} size={20}>
          <ProCard title="邀请成员" bordered>
            <PermissionAdminForm permissionTarget={permissionTarget} />
          </ProCard>
          {permissionTarget === 'model' && <PermissionTable />}
        </Space>
      </div>
    </>
  );
};
export default PermissionSection;
