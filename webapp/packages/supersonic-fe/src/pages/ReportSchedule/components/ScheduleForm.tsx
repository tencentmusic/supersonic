import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, InputNumber, Switch, Divider, Tag, Space } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import CronInput from './CronInput';
import type { ReportSchedule } from '@/services/reportSchedule';
import { getValidDataSetList, type ValidDataSetItem } from '@/services/reportSchedule';
import {
  getConfigList,
  DeliveryConfig,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';

interface ScheduleFormProps {
  visible: boolean;
  record?: ReportSchedule;
  initialDatasetId?: number;
  onCancel: () => void;
  onSubmit: (values: Partial<ReportSchedule>) => void;
}

const ScheduleForm: React.FC<ScheduleFormProps> = ({ visible, record, initialDatasetId, onCancel, onSubmit }) => {
  const [form] = Form.useForm();
  const isEdit = !!record?.id;
  const [deliveryConfigs, setDeliveryConfigs] = useState<DeliveryConfig[]>([]);
  const [loadingConfigs, setLoadingConfigs] = useState(false);
  const [dataSets, setDataSets] = useState<ValidDataSetItem[]>([]);
  const [loadingDataSets, setLoadingDataSets] = useState(false);

  // Fetch delivery configs and valid datasets when modal opens
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
      // Only show enabled configs
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
        // Parse deliveryConfigIds from comma-separated string to array
        const configIds = record.deliveryConfigIds
          ? record.deliveryConfigIds.split(',').map((id) => parseInt(id.trim(), 10)).filter((id) => !isNaN(id))
          : [];
        form.setFieldsValue({
          ...record,
          deliveryConfigIds: configIds,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          retryCount: 3,
          retryInterval: 30,
          outputFormat: 'EXCEL',
          enabled: true,
          deliveryConfigIds: [],
          ...(initialDatasetId !== undefined ? { datasetId: initialDatasetId } : {}),
        });
      }
    }
  }, [visible, record, initialDatasetId, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    // Convert deliveryConfigIds array back to comma-separated string
    const configIds = values.deliveryConfigIds;
    const submitValues = {
      ...values,
      deliveryConfigIds: Array.isArray(configIds) && configIds.length > 0
        ? configIds.join(',')
        : undefined,
    };
    onSubmit(submitValues);
  };

  // Custom option render for delivery config select
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
      title={isEdit ? '编辑调度任务' : '创建调度任务'}
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      width={640}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="任务名称" rules={[{ required: true, message: '请输入任务名称' }]}>
          <Input placeholder="如: GMV 日报" />
        </Form.Item>
        <Form.Item name="datasetId" label="关联数据集" rules={[{ required: true, message: '请选择数据集' }]}>
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
          <Input.TextArea rows={4} placeholder='{"metrics": [...], "dimensions": [...]}' />
        </Form.Item>
        <Form.Item name="cronExpression" label="调度频率" rules={[{ required: true, message: '请设置 Cron 表达式' }]}>
          <CronInput />
        </Form.Item>
        <Form.Item name="outputFormat" label="输出格式">
          <Select
            options={[
              { label: 'Excel', value: 'EXCEL' },
              { label: 'CSV', value: 'CSV' },
              { label: 'JSON', value: 'JSON' },
            ]}
          />
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
            <SendOutlined />
            <span>推送配置</span>
          </Space>
        </Divider>

        <Form.Item
          name="deliveryConfigIds"
          label="推送渠道"
          extra="选择报表生成后自动推送的渠道，可多选"
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

export default ScheduleForm;
