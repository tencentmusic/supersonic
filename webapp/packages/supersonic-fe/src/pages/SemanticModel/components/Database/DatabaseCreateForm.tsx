import { useEffect, forwardRef, useImperativeHandle, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button, Space } from 'antd';
import { saveDatabase, getDatabaseByDomainId, testDatabaseConnect } from '../../service';
import { formLayout } from '@/components/FormHelper/utils';

import styles from '../style.less';
type Props = {
  domainId: number;
  onSubmit: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const DatabaseCreateForm: ForwardRefRenderFunction<any, Props> = ({ domainId }, ref) => {
  const [form] = Form.useForm();
  const [selectedDbType, setSelectedDbType] = useState<string>('h2');
  const queryDatabaseConfig = async () => {
    const { code, data } = await getDatabaseByDomainId(domainId);
    if (code === 200) {
      form.setFieldsValue({ ...data });
      setSelectedDbType(data?.type);
      return;
    }
    message.error('数据库配置获取错误');
  };

  useEffect(() => {
    form.resetFields();
    queryDatabaseConfig();
  }, [domainId]);

  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
  }));

  const saveDatabaseConfig = async () => {
    const values = await form.validateFields();
    const { code, msg } = await saveDatabase({
      ...values,
      domainId,
    });

    if (code === 200) {
      message.success('保存成功');
      return;
    }
    message.error(msg);
  };
  const testDatabaseConnection = async () => {
    const values = await form.validateFields();
    const { code, data } = await testDatabaseConnect({
      ...values,
      domainId,
    });
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
            options={[
              { value: 'h2', label: 'h2' },
              { value: 'mysql', label: 'mysql' },
              { value: 'clickhouse', label: 'clickhouse' },
            ]}
          />
        </FormItem>
        {selectedDbType === 'h2' ? (
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
        <FormItem
          name="username"
          label="用户名"
          rules={[{ required: true, message: '请输入用户名' }]}
        >
          <Input placeholder="请输入用户名" />
        </FormItem>
        <FormItem name="password" label="密码">
          <Input.Password placeholder="请输入密码" />
        </FormItem>
        <FormItem name="database" label="数据库名称">
          <Input placeholder="请输入数据库名称" />
        </FormItem>

        <FormItem name="description" label="描述">
          <TextArea placeholder="请输入数据库描述" style={{ height: 100 }} />
        </FormItem>
        <FormItem>
          <Space>
            <Button
              type="primary"
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
      </Form>
    </>
  );
};

export default forwardRef(DatabaseCreateForm);
