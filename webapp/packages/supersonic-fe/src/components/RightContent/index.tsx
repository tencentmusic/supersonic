import { Space } from 'antd';
import React from 'react';
import { useModel } from 'umi';
import Avatar from './AvatarDropdown';

import styles from './index.less';
import cx from 'classnames';

export type SiderTheme = 'light' | 'dark';

const GlobalHeaderRight: React.FC = () => {
  const { initialState } = useModel('@@initialState');

  if (!initialState || !initialState.settings) {
    return null;
  }

  const { navTheme, layout } = initialState.settings;
  let className = styles.right;

  if (layout === 'top' || layout === 'mix') {
    className = cx(styles.right, styles.dark);
  }

  function handleLogin() {}

  return (
    <Space className={className}>
      <Avatar onClickLogin={handleLogin} />
    </Space>
  );
};
export default GlobalHeaderRight;
