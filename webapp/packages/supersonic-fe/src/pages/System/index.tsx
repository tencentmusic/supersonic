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
import ProCard from '@ant-design/pro-card';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { SystemConfigParametersItem, SystemConfig } from './types';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { groupBy } from 'lodash';

const FormItem = Form.Item;
const { TextArea } = Input;
const System: React.FC = () => {
  const [systemConfig, setSystemConfig] = useState<Record<string, SystemConfigParametersItem[]>>(
    {},
  );
  const [anchorItems, setAnchorItems] = useState<{ key: string; href: string; title: string }[]>(
    [],
  );
  const [configSource, setConfigSource] = useState<SystemConfig>();
  const [paramDescMap, setParamDescMap] = useState<Record<string, string>>({});

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
      initDescMap(parameters);
      setConfigSource(data);
    } else {
      message.error(msg);
    }
  };

  const initDescMap = (systemConfigParameters: SystemConfigParametersItem[]) => {
    const descData = systemConfigParameters.reduce(
      (descMap: Record<string, string>, item: SystemConfigParametersItem) => {
        descMap[item.name] = item.description;
        return descMap;
      },
      {},
    );
    setParamDescMap(descData);
  };

  const setInitData = (admins: string[], systemConfigParameters: SystemConfigParametersItem[]) => {
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
            description: paramDescMap[name],
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
                        {itemList.map((item) => {
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
                            case 'list': {
                              const { candidateValues = [] } = item;
                              const options = candidateValues.map((value) => {
                                return { label: value, value };
                              });
                              defaultItem = <Select style={{ width: '100%' }} options={options} />;
                              break;
                            }
                            default:
                              defaultItem = <Input />;
                              break;
                          }
                          return (
                            <FormItem
                              name={name}
                              key={name}
                              label={
                                <FormItemTitle
                                  title={comment}
                                  subTitle={paramDescMap[name]}
                                  // subTitleEditable={true}
                                  onSubTitleChange={(title) => {
                                    setParamDescMap({
                                      ...paramDescMap,
                                      [name]: title,
                                    });
                                  }}
                                />
                              }
                            >
                              {defaultItem}
                            </FormItem>
                          );
                        })}
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
