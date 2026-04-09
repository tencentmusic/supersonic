import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Divider,
  Tag,
  Space,
  message,
} from 'antd';
import { BellOutlined } from '@ant-design/icons';
import CronInput from '../../ReportSchedule/components/CronInput';
import type { AlertRule } from '@/services/alertRule';
import {
  getValidDataSetList,
  getDataSetSchema,
  type ValidDataSetItem,
  type DataSetSchemaField,
} from '@/services/reportSchedule';
import {
  getConfigList,
  DeliveryConfig,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';
import QueryConfigFormSection, {
  type QueryType,
  type QueryAggregatorFormItem,
  type QueryDimensionFilterFormItem,
  type QueryOrderFormItem,
  type QueryMetricFilterFormItem,
} from '@/components/QueryConfigFormSection';

const DEFAULT_ALERT_LIMIT = 1000;

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
  const [queryType, setQueryType] = useState<QueryType>('AGGREGATE');
  const [currentDimensions, setCurrentDimensions] = useState<DataSetSchemaField[]>([]);
  const [currentMetrics, setCurrentMetrics] = useState<DataSetSchemaField[]>([]);

  useEffect(() => {
    if (visible) {
      fetchDeliveryConfigs();
      fetchValidDataSets();
    }
  }, [visible]);

  const fetchDatasetSchema = async (datasetId: number) => {
    try {
      const res = await getDataSetSchema(datasetId);
      const payload: any = res?.data ?? res;
      const rawDims = Array.isArray(payload?.dimensions) ? payload.dimensions : [];
      const rawMetrics = Array.isArray(payload?.metrics) ? payload.metrics : [];
      setCurrentDimensions(rawDims.filter((field: any) => field.bizName));
      setCurrentMetrics(rawMetrics.filter((field: any) => field.bizName));
    } catch (error) {
      console.error('Failed to load dataset schema', error);
      setCurrentDimensions([]);
      setCurrentMetrics([]);
    }
  };

  const fetchValidDataSets = async () => {
    setLoadingDataSets(true);
    try {
      const res = await getValidDataSetList();
      const list = (res as any)?.data ?? res;
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
      const pageData = (res as any)?.data ?? res;
      const enabledConfigs = (pageData?.records || []).filter((c: DeliveryConfig) => c.enabled);
      setDeliveryConfigs(enabledConfigs);
    } catch (error) {
      console.error('Failed to load delivery configs', error);
    } finally {
      setLoadingConfigs(false);
    }
  };

  // ── queryConfig parsing helpers ──

  const parseQueryConfig = (qc: string | undefined): any => {
    if (!qc) return {};
    try { return JSON.parse(qc); } catch { return {}; }
  };

  const parseAggregators = (qc: string | undefined): QueryAggregatorFormItem[] => {
    const parsed = parseQueryConfig(qc);
    if (!Array.isArray(parsed?.aggregators)) return [];
    return parsed.aggregators
      .map((item: any) => ({ column: item?.column, func: item?.func || 'SUM' }))
      .filter((item: QueryAggregatorFormItem) => item.column);
  };

  const parseDimensionFilters = (qc: string | undefined): QueryDimensionFilterFormItem[] => {
    const parsed = parseQueryConfig(qc);
    if (!Array.isArray(parsed?.dimensionFilters)) return [];
    return parsed.dimensionFilters
      .map((item: any) => ({
        bizName: item?.bizName || item?.name,
        operator: item?.operator || 'EQUALS',
        value: Array.isArray(item?.value) ? item.value.join(', ') : item?.value,
      }))
      .filter((item: QueryDimensionFilterFormItem) => item.bizName || item.value);
  };

  const parseOrders = (qc: string | undefined): QueryOrderFormItem[] => {
    const parsed = parseQueryConfig(qc);
    if (!Array.isArray(parsed?.orders)) return [];
    return parsed.orders
      .map((item: any) => ({ column: item?.column, direction: item?.direction || 'ASC' }))
      .filter((item: QueryOrderFormItem) => item.column);
  };

  const parseMetricFilters = (qc: string | undefined): QueryMetricFilterFormItem[] => {
    const parsed = parseQueryConfig(qc);
    if (!Array.isArray(parsed?.metricFilters)) return [];
    return parsed.metricFilters
      .map((item: any) => ({
        bizName: item?.bizName || item?.name,
        operator: item?.operator || 'GREATER_THAN',
        value: Array.isArray(item?.value) ? item.value.join(', ') : item?.value,
      }))
      .filter((item: QueryMetricFilterFormItem) => item.bizName || item.value);
  };

  // ── Form initialization ──

  useEffect(() => {
    if (visible) {
      if (record) {
        const parsedQC = parseQueryConfig(record.queryConfig);
        const configIds = record.deliveryConfigIds
          ? record.deliveryConfigIds
              .split(',')
              .map((id) => parseInt(id.trim(), 10))
              .filter((id) => !isNaN(id))
          : [];
        const currentQueryType: QueryType =
          parsedQC.queryType === 'DETAIL' ? 'DETAIL' : 'AGGREGATE';
        setQueryType(currentQueryType);
        form.setFieldsValue({
          ...record,
          enabled: record.enabled === 1,
          deliveryConfigIds: configIds,
          queryType: currentQueryType,
          queryGroups: Array.isArray(parsedQC.groups) ? parsedQC.groups : [],
          queryLimit:
            typeof parsedQC.limit === 'number' && parsedQC.limit > 0
              ? parsedQC.limit
              : DEFAULT_ALERT_LIMIT,
          queryAggregators: parseAggregators(record.queryConfig),
          queryDimensionFilters: parseDimensionFilters(record.queryConfig),
          queryOrders: parseOrders(record.queryConfig),
          queryMetricFilters: parseMetricFilters(record.queryConfig),
        });
        if (record.datasetId) {
          fetchDatasetSchema(record.datasetId);
        }
      } else {
        setQueryType('AGGREGATE');
        setCurrentDimensions([]);
        setCurrentMetrics([]);
        form.resetFields();
        form.setFieldsValue({
          retryCount: 3,
          retryInterval: 30,
          silenceMinutes: 60,
          enabled: true,
          deliveryConfigIds: [],
          queryType: 'AGGREGATE',
          queryLimit: DEFAULT_ALERT_LIMIT,
          queryAggregators: [],
          queryDimensionFilters: [],
          queryGroups: [],
          queryOrders: [],
          queryMetricFilters: [],
        });
      }
    }
  }, [visible, record, form]);

  // ── Submit ──

  const handleOk = async () => {
    const values = await form.validateFields();
    const configIds = values.deliveryConfigIds;
    const groups =
      Array.isArray(values.queryGroups) && values.queryGroups.length > 0
        ? values.queryGroups
        : values.queryType === 'AGGREGATE'
          ? []
          : currentDimensions.map((item) => item.bizName);
    const aggregators = Array.isArray(values.queryAggregators)
      ? values.queryAggregators
          .filter((item: QueryAggregatorFormItem) => item?.column)
          .map((item: QueryAggregatorFormItem) => ({
            column: item.column,
            func: item.func || 'SUM',
          }))
      : [];
    if (values.queryType === 'AGGREGATE' && aggregators.length === 0) {
      message.error('聚合告警至少需要配置一个聚合指标');
      return;
    }
    if (
      values.queryType === 'AGGREGATE' &&
      new Set(aggregators.map((item: QueryAggregatorFormItem) => `${item.column}-${item.func}`)).size !==
        aggregators.length
    ) {
      message.error('聚合指标存在重复配置，请调整后再保存');
      return;
    }
    const dimensions = currentDimensions.map((field) => ({
      id: field.id,
      name: field.name,
      bizName: field.bizName,
    }));

    const orders = Array.isArray(values.queryOrders)
      ? values.queryOrders
          .filter((item: QueryOrderFormItem) => item?.column)
          .map((item: QueryOrderFormItem) => ({
            column: item.column,
            direction: item.direction || 'ASC',
          }))
      : [];

    const metricFilters = Array.isArray(values.queryMetricFilters)
      ? values.queryMetricFilters
          .filter((item: QueryMetricFilterFormItem) => {
            if (!item?.bizName) return false;
            if (['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'GREATER_THAN')) return true;
            return !!item?.value?.trim();
          })
          .map((item: QueryMetricFilterFormItem) => {
            const isMultiValue = ['IN', 'NOT_IN'].includes(item.operator || 'GREATER_THAN');
            const isNoValueOperator = ['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'GREATER_THAN');
            const rawValue = item.value?.trim() || '';
            return {
              bizName: item.bizName,
              name: item.bizName,
              operator: item.operator || 'GREATER_THAN',
              value: isNoValueOperator
                ? null
                : isMultiValue
                  ? rawValue.split(',').map((part: string) => part.trim()).filter(Boolean)
                  : rawValue,
            };
          })
      : [];

    const finalQueryConfig: Record<string, any> = {
      queryType: values.queryType || 'AGGREGATE',
      dimensions,
      groups,
      aggregators: values.queryType === 'AGGREGATE' ? aggregators : [],
      limit:
        typeof values.queryLimit === 'number' && values.queryLimit > 0
          ? values.queryLimit
          : DEFAULT_ALERT_LIMIT,
      dimensionFilters: Array.isArray(values.queryDimensionFilters)
        ? values.queryDimensionFilters
            .filter((item: QueryDimensionFilterFormItem) => {
              if (!item?.bizName) return false;
              if (['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'EQUALS')) return true;
              return !!item?.value?.trim();
            })
            .map((item: QueryDimensionFilterFormItem) => {
              const matchedDimension = dimensions.find(
                (dimension: any) => dimension?.bizName === item.bizName,
              );
              const isMultiValue = ['IN', 'NOT_IN'].includes(item.operator || 'EQUALS');
              const isNoValueOperator = ['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'EQUALS');
              const rawValue = item.value?.trim() || '';
              return {
                bizName: item.bizName,
                name: matchedDimension?.name || item.bizName,
                operator: item.operator || 'EQUALS',
                value: isNoValueOperator
                  ? null
                  : isMultiValue
                    ? rawValue.split(',').map((part: string) => part.trim()).filter(Boolean)
                    : rawValue,
              };
            })
        : [],
    };

    if (orders.length > 0) {
      finalQueryConfig.orders = orders;
    }
    if (metricFilters.length > 0) {
      finalQueryConfig.metricFilters = metricFilters;
    }

    const submitValues: Partial<AlertRule> = {
      name: values.name,
      description: values.description,
      datasetId: values.datasetId,
      enabled: values.enabled ? 1 : 0,
      cronExpression: values.cronExpression,
      silenceMinutes: values.silenceMinutes,
      retryCount: values.retryCount,
      retryInterval: values.retryInterval,
      queryConfig: JSON.stringify(finalQueryConfig),
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
            onChange={(value) => {
              form.setFieldsValue({
                queryGroups: [],
                queryAggregators: [],
                queryDimensionFilters: [],
                queryOrders: [],
                queryMetricFilters: [],
              });
              fetchDatasetSchema(value);
            }}
            options={dataSets.map((d) => ({ label: `${d.name} (ID: ${d.id})`, value: d.id }))}
          />
        </Form.Item>

        <Divider>
          <Space>
            <span>查询配置</span>
          </Space>
        </Divider>
        <QueryConfigFormSection
          form={form}
          queryType={queryType}
          setQueryType={setQueryType}
          currentDimensions={currentDimensions}
          currentMetrics={currentMetrics}
          queryTypeOptions={[
            { label: '聚合告警', value: 'AGGREGATE' },
            { label: '明细告警', value: 'DETAIL' },
          ]}
          groupsPlaceholder="选择分组字段，明细告警默认使用全部维度"
          metricsPlaceholder="选择指标"
          limitMax={1000}
          limitTooltip="告警查询会在后端限制最大 1000 行，建议在这里明确控制查询规模。"
        />
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
