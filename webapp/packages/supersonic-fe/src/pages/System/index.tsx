import styles from './style.less';
import { Button, Form, message, Space, Divider, Anchor, Row, Col } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import { getSystemConfig, saveSystemConfig } from '@/services/user';
import { ProCard } from '@ant-design/pro-components';
import SelectTMEPerson from '@/components/SelectTMEPerson';
import { ConfigParametersItem, SystemConfig, dependenciesItem } from './types';
// import { testLLMConn } from '../../services/system';
import { groupBy } from 'lodash';
import { genneratorFormItemList } from '../SemanticModel/utils';

const FormItem = Form.Item;

type Admin = string[];

const System: React.FC = () => {
  const [systemConfig, setSystemConfig] = useState<Record<string, ConfigParametersItem[]>>({});
  const [anchorItems, setAnchorItems] = useState<{ key: string; href: string; title: string }[]>(
    [],
  );
  const [configSource, setConfigSource] = useState<SystemConfig>();

  const configMap = useRef<Record<string, ConfigParametersItem>>();

  const configIocDepMap = useRef<Record<string, any>>();
  // const [llmTestLoading, setLlmTestLoading] = useState<boolean>(false);

  useEffect(() => {
    querySystemConfig();
  }, []);
  const [form] = Form.useForm();
  const querySystemConfig = async () => {
    const { code, data, msg } = await getSystemConfig();

    if (code === 200 && data) {
      const { parameters = [], admins = [] } = data;

      const parametersMap = parameters.reduce(
        (configReduceMap: Record<string, ConfigParametersItem>, item: ConfigParametersItem) => {
          return {
            ...configReduceMap,
            [item.name]: item,
          };
        },
        {},
      );

      configMap.current = parametersMap;

      groupConfigAndSet(parameters);
      initDepConfig(parameters, admins);

      setConfigSource(data);
    } else {
      message.error(msg);
    }
  };

  const initDepConfig = (parameters: ConfigParametersItem[], admins: Admin) => {
    const iocMap = getDepIoc(parameters);
    configIocDepMap.current = iocMap;
    const initFormValues = setInitData(admins, parameters);
    Object.keys(initFormValues).forEach((itemName) => {
      const targetDep = iocMap[itemName] || {};
      const excuteStack = Object.values(targetDep);
      if (Array.isArray(excuteStack)) {
        excuteDepConfig(itemName, initFormValues, true);
      }
    });
  };

  const groupConfigAndSet = (parameters: ConfigParametersItem[]) => {
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
  };

  const getDepIoc = (parameters: ConfigParametersItem[]) => {
    const iocMap: Record<string, Record<string, ConfigParametersItem>> = {};
    parameters.forEach((item) => {
      const { name: itemName, dependencies } = item;
      if (Array.isArray(dependencies)) {
        dependencies.forEach((depItem) => {
          const { name } = depItem;

          if (iocMap[name]) {
            iocMap[name] = {
              ...iocMap[name],
              [itemName]: item,
            };
          } else {
            iocMap[name] = {
              [itemName]: item,
            };
          }
        });
      }
    });
    return iocMap;
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
    return fieldsValue;
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

  const excuteDepConfig = (
    itemName: string,
    formValues: Record<string, any>,
    isInit: boolean = false,
  ) => {
    const targetDep = configIocDepMap?.current?.[itemName];
    if (!targetDep) {
      return;
    }
    const excuteStack = Object.values(targetDep);
    if (!Array.isArray(excuteStack)) {
      return;
    }
    const tempConfigMap: any = { ...configMap.current };
    const currentFormValues = formValues;

    excuteStack.forEach((configItem: any) => {
      const showStateList: boolean[] = [];
      const hasValueFieldsSetDefaultValueList: any[] = [];
      const { dependencies, name: configItemName } = configItem;
      dependencies.forEach((item: dependenciesItem) => {
        const { name, setDefaultValue } = item;
        const currentDepValue = currentFormValues[name];
        const showIncludesValue = item.show?.includesValue;
        if (Array.isArray(showIncludesValue)) {
          showStateList.push(showIncludesValue.includes(currentDepValue));
        }
        if (setDefaultValue && currentDepValue) {
          hasValueFieldsSetDefaultValueList.push({
            excuteItem: configItemName,
            ...item,
          });
        }
      });

      const visible = showStateList.every((item) => item);
      tempConfigMap[configItemName].visible = visible;
      const lastSetDefaultValueItem =
        hasValueFieldsSetDefaultValueList[hasValueFieldsSetDefaultValueList.length - 1];
      const lastSetDefaultValue = lastSetDefaultValueItem?.setDefaultValue;

      if (lastSetDefaultValue) {
        const targetValue = lastSetDefaultValue[currentFormValues[lastSetDefaultValueItem.name]];
        if (targetValue && !isInit) {
          form.setFieldValue(lastSetDefaultValueItem.excuteItem, targetValue);
        }
      }
    });

    groupConfigAndSet(Object.values(tempConfigMap));
  };

  // const testLLMConnect = async (params: any) => {
  //   setLlmTestLoading(true);
  //   const { code, data } = await testLLMConn(params);
  //   setLlmTestLoading(false);
  //   if (code === 200 && data) {
  //     message.success('连接成功');
  //   } else {
  //     message.error('模型连接失败');
  //   }
  // };

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
              <Form
                form={form}
                layout="vertical"
                className={styles.form}
                onValuesChange={(value, values) => {
                  const valueKey = Object.keys(value)[0];
                  excuteDepConfig(valueKey, values);
                }}
              >
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
