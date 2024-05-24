import React from 'react';
import { LogoutOutlined } from '@ant-design/icons';
import { useModel } from 'umi';
import HeaderDropdown from '../HeaderDropdown';
import styles from './index.less';
import TMEAvatar from '../TMEAvatar';
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
  const { currentUser = {} } = initialState as any;
  const items = [
    {
      label: (
        <>
          <LogoutOutlined />
          退出登录
        </>
      ),
      onClick: (event: any) => {
        const { key } = event;
        if (key === 'logout' && initialState) {
          loginOut().then(() => {
            setInitialState({ ...initialState, currentUser: undefined });
          });
          return;
        }
      },
      key: 'logout',
    },
  ];
  return (
    <HeaderDropdown menu={{ items }} disabled={APP_TARGET === 'inner'}>
      <span className={`${styles.action} ${styles.account}`}>
        <TMEAvatar className={styles.avatar} size="small" staffName={currentUser.staffName} />
        <span className={styles.userName}>{currentUser.staffName}</span>
      </span>
    </HeaderDropdown>
  );
};

export default AvatarDropdown;
