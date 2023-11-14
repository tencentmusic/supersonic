import styles from './style.less';
import { Button, Form, Input, InputNumber, message, Space, Switch, Divider } from 'antd';
import React, { useState, useEffect } from 'react';
import { getSystemConfig, saveSystemConfig } from '@/services/user';
import ProCard from '@ant-design/pro-card';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { SystemConfigParametersItem, SystemConfig } from './types';

const FormItem = Form.Item;
const { TextArea } = Input;
const System: React.FC = () => {
  const [systemConfig, setSystemConfig] = useState<SystemConfigParametersItem[]>([]);

  const [configSource, setConfigSource] = useState<SystemConfig>();

  useEffect(() => {
    querySystemConfig();
  }, []);
  const [form] = Form.useForm();
  const querySystemConfig = async () => {
    const { code, data, msg } = await getSystemConfig();
    if (code === 200) {
      const { parameters, admins } = data;
      setSystemConfig(parameters);
      setInitData(admins, parameters);
      setConfigSource(data);
    } else {
      message.error(msg);
    }
  };

  const setInitData = (admins: string[], systemConfig: SystemConfigParametersItem[]) => {
    const fieldsValue = systemConfig.reduce(
      (fields, item) => {
        const { name, value } = item;
        return {
          ...fields,
          [name]: value,
        };
      },
      { admins },
    );
    form.setFieldsValue(fieldsValue);
  };

  const querySaveSystemConfig = async () => {
    const submitData = await form.validateFields();
    const { code, msg } = await saveSystemConfig({
      ...configSource,
      admins: submitData.admins,
      parameters: systemConfig.map((item) => {
        const { name } = item;
        if (submitData[name] !== undefined) {
          return {
            ...item,
            value: submitData[name],
          };
        }
        return item;
      }),
    });
    if (code === 200) {
      message.success('保存成功');
    } else {
      message.error(msg);
    }
  };

  return (
    <>
      <div style={{ margin: '40px auto', width: 800 }}>
        <ProCard
          title="系统设置"
          extra={
            <Space>
              <Button
                type="primary"
                onClick={() => {
                  querySaveSystemConfig();
                }}
              >
                保 存
              </Button>
            </Space>
          }
        >
          <Form form={form} layout="vertical" className={styles.form}>
            <FormItem name="admins" label="管理员">
              <SelectTMEPerson placeholder="请邀请团队成员" />
            </FormItem>
            <Divider />
            {systemConfig.map((item: SystemConfigParametersItem) => {
              const { dataType, name, comment } = item;
              let defaultItem = <Input />;
              switch (dataType) {
                case 'string':
                  defaultItem = <TextArea placeholder="" style={{ height: 100 }} />;
                  break;
                case 'number':
                  defaultItem = <InputNumber style={{ width: '100%' }} />;
                  break;
                case 'bool':
                  return (
                    <FormItem
                      name={name}
                      label={comment}
                      key={name}
                      valuePropName="checked"
                      getValueFromEvent={(value) => {
                        return value === true ? 'true' : 'false';
                      }}
                      getValueProps={(value) => {
                        return {
                          checked: value === 'true',
                        };
                      }}
                    >
                      <Switch />
                    </FormItem>
                  );
                default:
                  defaultItem = <Input />;
                  break;
              }
              return (
                <FormItem name={name} label={comment} key={name}>
                  {defaultItem}
                </FormItem>
              );
            })}
          </Form>
        </ProCard>
      </div>
    </>
  );
};

export default System;
