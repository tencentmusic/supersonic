import React from 'react';
import { LogoutOutlined } from '@ant-design/icons';
import { Menu } from 'antd';
import { useModel } from 'umi';
import HeaderDropdown from '../HeaderDropdown';
import styles from './index.less';
import TMEAvatar from '../TMEAvatar';
import cx from 'classnames';
import { AUTH_TOKEN_KEY } from '@/common/constants';
import { history } from 'umi';

export type GlobalHeaderRightProps = {
  menu?: boolean;
  onClickLogin?: () => void;
};

/**
 * 退出登录
 * 并返回到首页
 */
const loginOut = async () => {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  history.push('/login');
  window.location.reload();
};

const { APP_TARGET } = process.env;

const AvatarDropdown: React.FC<GlobalHeaderRightProps> = () => {
  const { initialState = {}, setInitialState } = useModel('@@initialState');

  const onMenuClick = (event: any) => {
    const { key } = event;
    if (key === 'logout' && initialState) {
      loginOut().then(() => {
        setInitialState({ ...initialState, currentUser: undefined });
      });
      return;
    }
  };

  const { currentUser = {} } = initialState as any;
  console.log(currentUser, 'currentUser');
  const menuHeaderDropdown = (
    <Menu className={styles.menu} selectedKeys={[]} onClick={onMenuClick}>
      <Menu.Item key="logout">
        <LogoutOutlined />
        退出登录
      </Menu.Item>
    </Menu>
  );
  return (
    <HeaderDropdown overlay={menuHeaderDropdown} disabled={APP_TARGET === 'inner'}>
      <span className={`${styles.action} ${styles.account}`}>
        <TMEAvatar className={styles.avatar} size="small" staffName={currentUser.staffName} />
        <span className={cx(styles.name, 'anticon')}>{currentUser.staffName}</span>
      </span>
    </HeaderDropdown>
  );
};

export default AvatarDropdown;
