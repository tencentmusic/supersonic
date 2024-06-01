import styles from './style.less';
import {
  Button,
  Form,
  Input,
  InputNumber,
  message,
  Space,
  Switch,
  Select,
  Divider,
  Anchor,
  Row,
  Col,
} from 'antd';
import React, { useState, useEffect } from 'react';
import { getSystemConfig, saveSystemConfig } from '@/services/user';
import { ProCard } from '@ant-design/pro-components';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { ConfigParametersItem, SystemConfig } from './types';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { groupBy } from 'lodash';
import { genneratorFormItemList } from '../SemanticModel/utils';

const FormItem = Form.Item;
const { TextArea } = Input;
const System: React.FC = () => {
  const [systemConfig, setSystemConfig] = useState<Record<string, ConfigParametersItem[]>>({});
  const [anchorItems, setAnchorItems] = useState<{ key: string; href: string; title: string }[]>(
    [],
  );
  const [configSource, setConfigSource] = useState<SystemConfig>();

  useEffect(() => {
    querySystemConfig();
  }, []);
  const [form] = Form.useForm();
  const querySystemConfig = async () => {
    const { code, data, msg } = await getSystemConfig();
    if (code === 200 && data) {
      const { parameters = [], admins = [] } = data;
      const groupByConfig = groupBy(parameters, 'module');
      const anchor = Object.keys(groupByConfig).map((key: string) => {
        return {
          key,
          href: `#${key}`,
          title: key,
        };
      });
      setAnchorItems(anchor);
      setSystemConfig(groupByConfig);
      setInitData(admins, parameters);
      setConfigSource(data);
    } else {
      message.error(msg);
    }
  };

  const setInitData = (admins: string[], systemConfigParameters: ConfigParametersItem[]) => {
    const fieldsValue = systemConfigParameters.reduce(
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
      parameters: configSource!.parameters.map((item) => {
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
      <div style={{ margin: '40px auto', width: 1200 }}>
        <Row>
          <Col span={18}>
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
                <Space direction="vertical" style={{ width: '100%' }} size={35}>
                  {Object.keys(systemConfig).map((key: string) => {
                    const itemList = systemConfig[key];
                    return (
                      <ProCard
                        title={<span style={{ color: '#296df3' }}>{key}</span>}
                        key={key}
                        bordered
                        id={key}
                      >
                        {genneratorFormItemList(itemList)}
                      </ProCard>
                    );
                  })}
                </Space>
              </Form>
            </ProCard>
          </Col>
          <Col span={6} style={{ background: '#fff' }}>
            <div style={{ marginTop: 20 }}>
              <Anchor items={anchorItems} />
            </div>
          </Col>
        </Row>
      </div>
    </>
  );
};

export default System;
