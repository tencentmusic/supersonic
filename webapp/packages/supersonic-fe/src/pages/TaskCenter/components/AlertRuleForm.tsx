import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, InputNumber, Switch, Divider, Tag, Space } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import CronInput from '../../ReportSchedule/components/CronInput';
import type { AlertRule } from '@/services/alertRule';
import { getValidDataSetList, type ValidDataSetItem } from '@/services/reportSchedule';
import {
  getConfigList,
  DeliveryConfig,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';

interface AlertRuleFormProps {
  visible: boolean;
  record?: AlertRule;
  onCancel: () => void;
  onSubmit: (values: Partial<AlertRule>) => void;
}

const AlertRuleForm: React.FC<AlertRuleFormProps> = ({ visible, record, onCancel, onSubmit }) => {
  const [form] = Form.useForm();
  const isEdit = !!record?.id;
  const [deliveryConfigs, setDeliveryConfigs] = useState<DeliveryConfig[]>([]);
  const [loadingConfigs, setLoadingConfigs] = useState(false);
  const [dataSets, setDataSets] = useState<ValidDataSetItem[]>([]);
  const [loadingDataSets, setLoadingDataSets] = useState(false);

  useEffect(() => {
    if (visible) {
      fetchDeliveryConfigs();
      fetchValidDataSets();
    }
  }, [visible]);

  const fetchValidDataSets = async () => {
    setLoadingDataSets(true);
    try {
      const list = await getValidDataSetList();
      setDataSets(Array.isArray(list) ? list : []);
    } catch (error) {
      console.error('Failed to load datasets', error);
      setDataSets([]);
    } finally {
      setLoadingDataSets(false);
    }
  };

  const fetchDeliveryConfigs = async () => {
    setLoadingConfigs(true);
    try {
      const res = await getConfigList({ pageNum: 1, pageSize: 100 });
      const enabledConfigs = (res.records || []).filter((c: DeliveryConfig) => c.enabled);
      setDeliveryConfigs(enabledConfigs);
    } catch (error) {
      console.error('Failed to load delivery configs', error);
    } finally {
      setLoadingConfigs(false);
    }
  };

  useEffect(() => {
    if (visible) {
      if (record) {
        const configIds = record.deliveryConfigIds
          ? record.deliveryConfigIds
              .split(',')
              .map((id) => parseInt(id.trim(), 10))
              .filter((id) => !isNaN(id))
          : [];
        form.setFieldsValue({
          ...record,
          enabled: record.enabled === 1,
          deliveryConfigIds: configIds,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          retryCount: 3,
          retryInterval: 30,
          silenceMinutes: 60,
          enabled: true,
          deliveryConfigIds: [],
        });
      }
    }
  }, [visible, record, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    const configIds = values.deliveryConfigIds;
    const submitValues: Partial<AlertRule> = {
      ...values,
      enabled: values.enabled ? 1 : 0,
      deliveryConfigIds:
        Array.isArray(configIds) && configIds.length > 0 ? configIds.join(',') : undefined,
    };
    onSubmit(submitValues);
  };

  const renderConfigOption = (config: DeliveryConfig) => {
    const typeInfo = DELIVERY_TYPE_MAP[config.deliveryType];
    return (
      <Space>
        <Tag color={typeInfo?.color}>{typeInfo?.text || config.deliveryType}</Tag>
        <span>{config.name}</span>
      </Space>
    );
  };

  return (
    <Modal
      title={isEdit ? '编辑告警规则' : '创建告警规则'}
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      width={640}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="规则名称"
          rules={[{ required: true, message: '请输入规则名称' }]}
        >
          <Input placeholder="如: 订单量异常告警" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <Input.TextArea rows={2} placeholder="规则描述 (可选)" />
        </Form.Item>
        <Form.Item
          name="datasetId"
          label="关联数据集"
          rules={[{ required: true, message: '请选择数据集' }]}
        >
          <Select
            placeholder="请选择已配置的数据集"
            allowClear
            showSearch
            optionFilterProp="label"
            loading={loadingDataSets}
            options={dataSets.map((d) => ({ label: `${d.name} (ID: ${d.id})`, value: d.id }))}
          />
        </Form.Item>
        <Form.Item name="queryConfig" label="查询参数 (JSON)">
          <Input.TextArea rows={3} placeholder='{"metrics": [...], "dimensions": [...]}' />
        </Form.Item>
        <Form.Item name="cronExpression" label="检查频率">
          <CronInput />
        </Form.Item>
        <Form.Item name="silenceMinutes" label="静默时长(分钟)">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item name="retryCount" label="重试次数">
          <InputNumber min={0} max={5} />
        </Form.Item>
        <Form.Item name="retryInterval" label="重试间隔(秒)">
          <InputNumber min={1} />
        </Form.Item>
        <Form.Item name="enabled" label="启用" valuePropName="checked">
          <Switch />
        </Form.Item>

        <Divider>
          <Space>
            <BellOutlined />
            <span>告警推送</span>
          </Space>
        </Divider>

        <Form.Item
          name="deliveryConfigIds"
          label="推送渠道"
          extra="选择告警触发后自动推送的渠道，可多选"
        >
          <Select
            mode="multiple"
            placeholder="选择推送渠道 (可选)"
            loading={loadingConfigs}
            allowClear
            optionFilterProp="label"
            options={deliveryConfigs.map((config) => ({
              label: renderConfigOption(config),
              value: config.id,
              title: `${config.name} (${config.deliveryType})`,
            }))}
            tagRender={(props) => {
              const config = deliveryConfigs.find((c) => c.id === props.value);
              if (!config) return <Tag>{props.value}</Tag>;
              const typeInfo = DELIVERY_TYPE_MAP[config.deliveryType];
              return (
                <Tag
                  color={typeInfo?.color}
                  closable={props.closable}
                  onClose={props.onClose}
                  style={{ marginRight: 3 }}
                >
                  {config.name}
                </Tag>
              );
            }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AlertRuleForm;
