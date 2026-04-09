// import type { FC } from 'react';
import styles from './style.less';
import { Button, Form, Input, message, Space, Divider, Spin } from 'antd';
import { LockOutlined, UserOutlined, GoogleOutlined, WindowsOutlined, KeyOutlined } from '@ant-design/icons';
import RegisterForm from './components/RegisterForm';
// import ForgetPwdForm from './components/ForgetPwdForm';
import { ROUTE_AUTH_CODES } from '../../../config/routes';
import S2Icon, { ICON } from '@/components/S2Icon';
import React, { useState, useEffect } from 'react';
import { useForm } from 'antd/lib/form/Form';
import type { RegisterFormDetail } from './components/types';
import { postUserLogin, userRegister, getOAuthProviders, getOAuthAuthorizeUrl } from './services';
import { AUTH_TOKEN_KEY, TENANT_ID_KEY } from '@/common/constants';
import { queryCurrentUser } from '@/services/user';
import { history, useModel } from '@umijs/max';
import CryptoJS from 'crypto-js';
import { encryptPassword } from '@/utils/utils';

const { Item } = Form;

// OAuth provider icon mapping
const OAUTH_ICONS: Record<string, React.ReactNode> = {
  google: <GoogleOutlined />,
  azure: <WindowsOutlined />,
  keycloak: <KeyOutlined />,
};

// OAuth provider display names
const OAUTH_NAMES: Record<string, string> = {
  google: 'Google',
  azure: 'Microsoft',
  keycloak: 'Keycloak',
};

const LoginPage: React.FC = () => {
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [oauthEnabled, setOauthEnabled] = useState<boolean>(false);
  const [oauthProviders, setOauthProviders] = useState<string[]>([]);
  const [oauthLoading, setOauthLoading] = useState<boolean>(true);
  const encryptKey = CryptoJS.enc.Utf8.parse('supersonic@2024');
  const [form] = useForm();
  const { initialState = {}, setInitialState } = useModel('@@initialState');

  // Load OAuth providers on mount
  useEffect(() => {
    loadOAuthProviders();
    // Check for error in URL
    const urlParams = new URLSearchParams(window.location.search);
    const error = urlParams.get('error');
    if (error) {
      message.error(`Login failed: ${error}`);
    }
  }, []);

  const loadOAuthProviders = async () => {
    try {
      const result = await getOAuthProviders();
      setOauthEnabled(result.enabled);
      setOauthProviders(result.providers || []);
    } catch (err) {
      console.log('OAuth not available');
      setOauthEnabled(false);
    } finally {
      setOauthLoading(false);
    }
  };

  // Handle OAuth login
  const handleOAuthLogin = (provider: string) => {
    window.location.href = getOAuthAuthorizeUrl(provider);
  };
  // 获取用户权限码（与 app.tsx 中的 getAuthCodes 逻辑保持一致）
  const getAuthCodes = (currentUser: any) => {
    // 如果用户是超级管理员，返回所有权限码
    if (currentUser?.superAdmin) {
      return Object.values(ROUTE_AUTH_CODES);
    }
    // 否则返回后端返回的权限列表
    const permissions = currentUser?.permissions || [];
    // 同时兼容旧的 SYSTEM_ADMIN 逻辑
    if (currentUser?.isAdmin === 1) {
      if (!permissions.includes(ROUTE_AUTH_CODES.SYSTEM_ADMIN)) {
        permissions.push(ROUTE_AUTH_CODES.SYSTEM_ADMIN);
      }
    }
    return permissions;
  };

  // 通过用户信息进行登录
  const loginDone = async (values: RegisterFormDetail) => {
    const { code, data, msg } = await postUserLogin(values);
    if (code === 200) {
      localStorage.setItem(AUTH_TOKEN_KEY, data);
      const { code: queryUserCode, data: queryUserData } = await queryCurrentUser();
      if (queryUserCode === 200) {
        const currentUser = {
          ...queryUserData,
          staffName: queryUserData.staffName || queryUserData.name,
        };
        // 使用与 app.tsx 一致的权限码获取逻辑
        const authCodes = getAuthCodes(currentUser);
        // Store tenant ID for multi-tenancy support
        if (queryUserData.tenantId) {
          localStorage.setItem(TENANT_ID_KEY, String(queryUserData.tenantId));
        }
        setInitialState({ ...initialState, currentUser, authCodes });
      }
      history.push('/');
      return;
    } else {
      message.error(msg);
    }
  };

  // 处理登录按钮响应
  const handleLogin = async () => {
    const { validateFields } = form;
    const content = await validateFields();
    await loginDone({ ...content, password: encryptPassword(content.password, encryptKey as any) });
  };

  // 处理注册弹窗确定按钮
  const handleRegister = async (values: RegisterFormDetail) => {
    const enCodeValues = { ...values, password: encryptPassword(values.password, encryptKey as any) };
    const { code, msg } = await userRegister(enCodeValues);
    if (code === 200) {
      message.success('注册成功');
      setCreateModalVisible(false);
      // 注册完自动帮用户登录
      await loginDone(enCodeValues);
    } else {
      message.error(msg);
    }
  };

  // 相应注册按钮
  const handleRegisterBtn = () => {
    setCreateModalVisible(true);
  };

  // // 忘记密码弹窗确定响应
  // const handleForgetPwd = async (values: RegisterFormDetail) => {
  //   await getUserForgetPwd({ ...values });
  //   message.success('发送邮件成功，请在收到邮件后进入邮件链接进行密码重置');
  //   setForgetModalVisible(false);
  // };

  // // 响应忘记密码按钮
  // const handleForgetPwdBtn = () => {
  //   setForgetModalVisible(true);
  // };

  return (
    <div className={styles.loginWarp}>
      <div className={styles.content}>
        <div className={styles.formContent}>
          <div className={styles.formBox}>
            <Form form={form} labelCol={{ span: 6 }} colon={false} name="loginForm">
              <div className={styles.loginMain}>
                <h3 className={styles.title}>
                  <Space>
                    <S2Icon
                      icon={ICON.iconlogobiaoshi}
                      size={30}
                      color="#296DF3"
                      style={{ display: 'inline-block', marginTop: 8 }}
                    />
                    <div>SuperSonic</div>
                  </Space>
                </h3>
                <Item name="name" rules={[{ required: true }]} label="">
                  <Input size="large" placeholder="用户名: admin" prefix={<UserOutlined />} />
                </Item>
                <Item name="password" rules={[{ required: true }]} label="">
                  <Input
                    size="large"
                    type="password"
                    placeholder="密码: 123456"
                    onPressEnter={handleLogin}
                    prefix={<LockOutlined />}
                  />
                </Item>

                <Button className={styles.signInBtn} type="primary" onClick={handleLogin}>
                  登录
                </Button>

                <div className={styles.tool}>
                  <Button className={styles.button} onClick={handleRegisterBtn}>
                    注册
                  </Button>
                  {/* <Button className={styles.button} type="link" onClick={handleForgetPwdBtn}>
              忘记密码
            </Button> */}
                </div>

                {/* OAuth Login Section */}
                {!oauthLoading && oauthEnabled && oauthProviders.length > 0 && (
                  <>
                    <Divider plain>或使用以下方式登录</Divider>
                    <div className={styles.oauthButtons}>
                      {oauthProviders.map((provider) => (
                        <Button
                          key={provider}
                          className={styles.oauthButton}
                          icon={OAUTH_ICONS[provider] || <KeyOutlined />}
                          onClick={() => handleOAuthLogin(provider)}
                        >
                          {OAUTH_NAMES[provider] || provider}
                        </Button>
                      ))}
                    </div>
                  </>
                )}
                {oauthLoading && (
                  <div className={styles.oauthLoading}>
                    <Spin size="small" />
                  </div>
                )}
              </div>
            </Form>
          </div>
        </div>
      </div>
      <RegisterForm
        onCancel={() => {
          setCreateModalVisible(false);
        }}
        onSubmit={handleRegister}
        createModalVisible={createModalVisible}
      />
    </div>
  );
};

export default LoginPage;
