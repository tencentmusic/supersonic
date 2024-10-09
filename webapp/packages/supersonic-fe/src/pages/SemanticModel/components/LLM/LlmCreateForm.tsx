import { useEffect, forwardRef, useImperativeHandle, useState } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { message, Form, Input, Select, Button, Space, Slider, InputNumber } from 'antd';
import { saveLlmConfig } from '../../service';
import { testLLMConn } from '@/services/system';
import { formLayout } from '@/components/FormHelper/utils';
import { encryptPassword, decryptPassword } from '@/utils/utils';
import styles from '../style.less';
type Props = {
  llmItem?: any;
  onSubmit?: (params?: any) => void;
};

const FormItem = Form.Item;
const TextArea = Input.TextArea;

const DatabaseCreateForm: ForwardRefRenderFunction<any, Props> = ({ llmItem, onSubmit }, ref) => {
  const [form] = Form.useForm();

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
  }, [llmItem]);

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
    const { code, msg } = await saveLlmConfig({
      ...(llmItem || {}),
      ...values,
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
    setTestLoading(true);
    const { code, data } = await testLLMConn({
      ...values.config,
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
        initialValues={formData}
      >
        <FormItem
          name="name"
          label="连接名称"
          rules={[{ required: true, message: '请输入连接名称' }]}
        >
          <Input placeholder="请输入连接名称" />
        </FormItem>
        <FormItem name={['config', 'provider']} label="接口协议">
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
        </FormItem>
        <FormItem name="description" label="描述">
          <TextArea placeholder="请输入大模型连接描述" style={{ height: 100 }} />
        </FormItem>

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
      </Form>
    </>
  );
};

export default forwardRef(DatabaseCreateForm);
