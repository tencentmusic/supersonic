import { useEffect, forwardRef, useImperativeHandle, useState, useRef } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button, Space, Slider, InputNumber } from 'antd';
import { saveLlmConfig } from '../../service';
import { ConfigParametersItem, SystemConfig, dependenciesItem } from '@/pages/System/types';
import { testLLMConn, getLlmConfig } from '@/services/system';
import { formLayout } from '@/components/FormHelper/utils';

import { genneratorFormItemList } from '../../utils';
import styles from '../style.less';
type Props = {
  llmItem?: any;
  onSubmit?: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const LlmCreateForm: ForwardRefRenderFunction<any, Props> = ({ llmItem, onSubmit }, ref) => {
  const [form] = Form.useForm();
  const [config, setConfig] = useState<any>([]);
  const [formData, setFormData] = useState<any>({
    config: {
      timeOut: 60,
      provider: 'OPEN_AI',
      temperature: 0,
    },
  });
  const [testLoading, setTestLoading] = useState<boolean>(false);

  useEffect(() => {
    form.resetFields();
    if (llmItem) {
      form.setFieldsValue({ ...llmItem });
    }
    queryConfig();
  }, [llmItem]);

  const getFormValidateFields = async () => {
    return await form.validateFields();
  };

  useImperativeHandle(ref, () => ({
    getFormValidateFields,
    saveLlmConfig: save,
    testLlmConnection,
  }));

  const save = async () => {
    const values = await form.validateFields();
    const { code, msg } = await saveLlmConfig({
      ...(llmItem || {}),
      ...values,
      config: values,
    });

    if (code === 200) {
      message.success('保存成功');
      onSubmit?.();
      return;
    }
    message.error(msg);
  };
  const queryConfig = async () => {
    setTestLoading(true);
    const { code, data } = await getLlmConfig();
    setTestLoading(false);
    if (code === 200 && data) {
      let parameters = data;
      if (llmItem?.config) {
        parameters = data.map((item) => {
          const target = llmItem.config[item.name];
          if (target) {
            return {
              ...item,
              value: target,
            };
          }
          return item;
        });
      }
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
      initDepConfig(parameters);
      setConfig(parameters);
      return;
    }
    message.error('获取大模型配置信息失败');
  };

  const testLlmConnection = async () => {
    const values = await form.validateFields();
    setTestLoading(true);
    const { code, data } = await testLLMConn({
      ...values,
    });
    setTestLoading(false);
    if (code === 200 && data) {
      message.success('连接测试通过');
      return;
    }
    message.error('连接测试失败');
  };
  const configIocDepMap = useRef<Record<string, any>>();
  const configMap = useRef<Record<string, ConfigParametersItem>>();

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
  const setInitData = (systemConfigParameters: ConfigParametersItem[]) => {
    const fieldsValue = systemConfigParameters.reduce((fields, item) => {
      const { name, value } = item;
      return {
        ...fields,
        [name]: value,
      };
    }, {});
    form.setFieldsValue(fieldsValue);
    return fieldsValue;
  };

  const initDepConfig = (parameters: ConfigParametersItem[]) => {
    const iocMap = getDepIoc(parameters);
    configIocDepMap.current = iocMap;
    const initFormValues = setInitData(parameters);
    Object.keys(initFormValues).forEach((itemName) => {
      const targetDep = iocMap[itemName] || {};
      const excuteStack = Object.values(targetDep);
      if (Array.isArray(excuteStack)) {
        excuteDepConfig(itemName, initFormValues, true);
      }
    });
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
    setConfig(Object.values(tempConfigMap));
  };

  return (
    <>
      <Form
        {...formLayout}
        form={form}
        layout="vertical"
        className={styles.form}
        initialValues={formData}
        onValuesChange={(value, values) => {
          const valueKey = Object.keys(value)[0];
          excuteDepConfig(valueKey, values);
        }}
      >
        <FormItem
          name="name"
          label="连接名称"
          rules={[{ required: true, message: '请输入连接名称' }]}
        >
          <Input placeholder="请输入连接名称" />
        </FormItem>
        {genneratorFormItemList(config)}
        {/* <FormItem name={['config', 'provider']} label="接口协议">
          <Select placeholder="">
            {['OPEN_AI', 'OLLAMA'].map((item) => (
              <Select.Option key={item} value={item}>
                {item}
              </Select.Option>
            ))}
          </Select>
        </FormItem>
        <FormItem name={['config', 'modelName']} label="Model Name">
          <Input placeholder="请输入语言模型名称" />
        </FormItem>
        <FormItem name={['config', 'baseUrl']} label="Base URL">
          <Input placeholder="请输入Base URL" />
        </FormItem>
        <FormItem
          name={['config', 'apiKey']}
          label="API Key"
          hidden={formData?.config?.provider === 'OLLAMA'}
          getValueFromEvent={(event) => {
            const value = event.target.value;
            return encryptPassword(value);
          }}
          getValueProps={(value) => {
            return {
              value: value ? decryptPassword(value) : '',
            };
          }}
        >
          <Input.Password placeholder="请输入API Key" visibilityToggle />
        </FormItem>

        <FormItem name={['config', 'temperature']} label="Temperature">
          <Slider
            min={0}
            max={1}
            step={0.1}
            marks={{
              0: '精准',
              1: '随机',
            }}
          />
        </FormItem>
        <FormItem name={['config', 'timeOut']} label="超时时间(秒)">
          <InputNumber />
        </FormItem> */}
        <FormItem name="description" label="描述">
          <TextArea placeholder="请输入大模型连接描述" style={{ height: 100 }} />
        </FormItem>
      </Form>
    </>
  );
};

export default forwardRef(LlmCreateForm);
