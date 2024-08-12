import { useEffect, forwardRef, useImperativeHandle, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button, Space } from 'antd';
import {
  saveDatabase,
  testDatabaseConnect,
  getDatabaseParameters,
  getDatabaseDetail,
} from '../../service';
import { formLayout } from '@/components/FormHelper/utils';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { ConfigParametersItem } from '../../../System/types';
import { genneratorFormItemList } from '../../utils';
import { ISemantic } from '../../data';
import { encryptPassword, decryptPassword } from '@/utils/utils';
import CryptoJS from 'crypto-js';
import styles from '../style.less';
type Props = {
  domainId?: number;
  databaseId?: number;
  hideSubmitBtn?: boolean;
  onSubmit?: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const DatabaseCreateForm: ForwardRefRenderFunction<any, Props> = (
  { domainId, databaseId, onSubmit, hideSubmitBtn = false },
  ref,
) => {
  const encryptKey = CryptoJS.enc.Utf8.parse('supersonic@2024');
  const [form] = Form.useForm();
  const [selectedDbType, setSelectedDbType] = useState<string>('');
  const [databaseOptions, setDatabaseOptions] = useState<{ value: string; label: string }[]>([]);
  const [databaseConfig, setDatabaseConfig] = useState<Record<string, ConfigParametersItem[]>>({});
  const [testLoading, setTestLoading] = useState<boolean>(false);

  const [dataBaseDetail, setDataBaseDetail] = useState<ISemantic.IDatabaseItem>();

  useEffect(() => {
    form.resetFields();
    if (dataBaseDetail) {
      form.setFieldsValue({ ...dataBaseDetail });
      setSelectedDbType(dataBaseDetail?.type);
    }
  }, [dataBaseDetail]);

  useEffect(() => {
    if (databaseId) {
      queryDatabaseDetail(databaseId);
    }
  }, [databaseId]);

  useEffect(() => {
    queryDatabaseConfig();
  }, []);

  const queryDatabaseDetail = async (id: number) => {
    const { code, msg, data } = await getDatabaseDetail(id);
    if (code === 200) {
      setDataBaseDetail({ ...data, password: data.password ? decryptPassword(data.password) : '' });
      return;
    }
    message.error(msg);
  };

  const queryDatabaseConfig = async () => {
    const { code, msg, data } = await getDatabaseParameters();
    if (code === 200) {
      const options = Object.keys(data).map((sqlName: string) => {
        return {
          value: sqlName,
          label: sqlName,
        };
      });
      setDatabaseConfig(data);
      setDatabaseOptions(options);
      return;
    }
    message.error(msg);
  };

  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
    saveDatabaseConfig,
    testDatabaseConnection,
  }));

  const saveDatabaseConfig = async () => {
    const values = await form.validateFields();
    const { password } = values;
    const { code, msg } = await saveDatabase({
      ...(dataBaseDetail || {}),
      ...values,
      domainId,
      password: encryptPassword(password),
    });

    if (code === 200) {
      message.success('保存成功');
      onSubmit?.();
      return;
    }
    message.error(msg);
  };
  const testDatabaseConnection = async () => {
    const values = await form.validateFields();
    const { password } = values;
    setTestLoading(true);
    const { code, data } = await testDatabaseConnect({
      ...values,
      domainId,
      password: encryptPassword(password),
    });
    setTestLoading(false);
    if (code === 200 && data) {
      message.success('连接测试通过');
      return;
    }
    message.error('连接测试失败');
  };
  return (
    <>
      <Form
        {...formLayout}
        form={form}
        layout="vertical"
        className={styles.form}
        onValuesChange={(value) => {
          const { type } = value;
          if (type) {
            setSelectedDbType(type);
          }
        }}
      >
        <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input placeholder="请输入数据库名称" />
        </FormItem>
        <FormItem
          name="type"
          label="数据库类型"
          rules={[{ required: true, message: '请选择数据库类型' }]}
        >
          <Select
            style={{ width: '100%' }}
            placeholder="请选择数据库类型"
            options={databaseOptions}
          />
        </FormItem>

        {databaseConfig[selectedDbType] && genneratorFormItemList(databaseConfig[selectedDbType])}
        {/* {selectedDbType === 'h2' ? (
          <FormItem name="url" label="链接" rules={[{ required: true, message: '请输入链接' }]}>
            <Input placeholder="请输入链接" />
          </FormItem>
        ) : (
          <>
            <FormItem name="host" label="host" rules={[{ required: true, message: '请输入IP' }]}>
              <Input placeholder="请输入IP" />
            </FormItem>
            <FormItem
              name="port"
              label="port"
              rules={[{ required: true, message: '请输入端口号' }]}
            >
              <Input placeholder="请输入端口号" />
            </FormItem>
          </>
        )}

        {selectedDbType === 'mysql' && (
          <FormItem
            name="version"
            label="数据库版本"
            rules={[{ required: true, message: '请选择数据库版本' }]}
          >
            <Select
              style={{ width: '100%' }}
              placeholder="请选择数据库版本"
              options={[
                { value: '5.7', label: '5.7' },
                { value: '8.0', label: '8.0' },
              ]}
            />
          </FormItem>
        )} */}
        {/* <FormItem
          name="username"
          label="用户名"
        >
          <Input placeholder="请输入用户名" />
        </FormItem>
        <FormItem name="password" label="密码">
          <Input.Password placeholder="请输入密码" />
        </FormItem>
        <FormItem name="database" label="数据库名称">
          <Input placeholder="请输入数据库名称" />
        </FormItem> */}
        <FormItem
          name="admins"
          label="管理员"
          // rules={[{ required: true, message: '请设定数据库连接管理者' }]}
        >
          <SelectTMEPerson placeholder="请邀请团队成员" />
        </FormItem>
        <FormItem name="viewers" label="使用者">
          <SelectTMEPerson placeholder="请邀请团队成员" />
        </FormItem>

        <FormItem name="description" label="描述">
          <TextArea placeholder="请输入数据库描述" style={{ height: 100 }} />
        </FormItem>
        {!hideSubmitBtn && (
          <FormItem>
            <Space>
              <Button
                type="primary"
                loading={testLoading}
                onClick={() => {
                  testDatabaseConnection();
                }}
              >
                连接测试
              </Button>

              <Button
                type="primary"
                onClick={() => {
                  saveDatabaseConfig();
                }}
              >
                保 存
              </Button>
            </Space>
          </FormItem>
        )}
      </Form>
    </>
  );
};

export default forwardRef(DatabaseCreateForm);
