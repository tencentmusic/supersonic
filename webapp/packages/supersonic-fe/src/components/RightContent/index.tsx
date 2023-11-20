import { Space } from 'antd';
import React, { useEffect, useState } from 'react';
import { useModel, history } from 'umi';
import Avatar from './AvatarDropdown';
import { SettingOutlined } from '@ant-design/icons';
import { getSystemConfig } from '@/services/user';
import styles from './index.less';

export type SiderTheme = 'light' | 'dark';

const GlobalHeaderRight: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const [hasSettingPermisson, setHasSettingPermisson] = useState<boolean>(false);
  useEffect(() => {
    querySystemConfig();
  }, []);

  if (!initialState || !initialState.settings) {
    return null;
  }
  const { currentUser = {} } = initialState as any;

  const querySystemConfig = async () => {
    const { code, data } = await getSystemConfig();
    if (code === 200) {
      const { admins } = data;
      if (Array.isArray(admins) && admins.includes(currentUser?.staffName)) {
        setHasSettingPermisson(true);
      }
    }
  };

  function handleLogin() {}

  return (
    <Space className={styles.right}>
      <Avatar onClickLogin={handleLogin} />
      {hasSettingPermisson && (
        <span
          className={styles.action}
          style={{ padding: 20 }}
          onClick={() => {
            history.push(`/system`);
          }}
        >
          <SettingOutlined />
        </span>
      )}
    </Space>
  );
};
export default GlobalHeaderRight;
