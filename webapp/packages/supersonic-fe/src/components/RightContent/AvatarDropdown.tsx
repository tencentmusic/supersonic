import React, { useRef } from 'react';
import { LogoutOutlined, KeyOutlined, UnlockOutlined } from '@ant-design/icons';
import { useModel } from 'umi';
import HeaderDropdown from '../HeaderDropdown';
import styles from './index.less';
import TMEAvatar from '../TMEAvatar';
import { AUTH_TOKEN_KEY } from '@/common/constants';
import ChangePasswordModal, { IRef as IRefChangePasswordModal } from './ChangePasswordModal';
import AccessTokensModal, { IRef as IAccessTokensModalRef } from './AccessTokensModal';
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
  const changePasswordModalRef = useRef<IRefChangePasswordModal>(null);
  const accessTokensModalRef = useRef<IAccessTokensModalRef>(null);

  const handleAccessToken = () => {
    accessTokensModalRef.current?.open();
  };

  const handleChangePassword = () => {
    changePasswordModalRef.current?.open();
  };

  const items = [
    {
      label: (
        <>
          <KeyOutlined />
          访问令牌
        </>
      ),
      key: 'accessToken',
      onClick: handleAccessToken,
    },
    {
      label: (
        <>
          <UnlockOutlined />
          修改密码
        </>
      ),
      key: 'changePassword',
      onClick: handleChangePassword,
    },
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
    <>
      <HeaderDropdown
        menu={{
          items,
        }}
        disabled={APP_TARGET === 'inner'}
      >
        <span className={`${styles.action} ${styles.account}`}>
          <TMEAvatar className={styles.avatar} size="small" staffName={currentUser.staffName} />
          <span className={styles.userName}>{currentUser.staffName}</span>
        </span>
      </HeaderDropdown>
      <ChangePasswordModal ref={changePasswordModalRef} />
      <AccessTokensModal ref={accessTokensModalRef} />
    </>
  );
};

export default AvatarDropdown;
