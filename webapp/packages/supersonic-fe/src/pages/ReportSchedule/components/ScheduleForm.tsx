import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Tag,
  Space,
  Radio,
  DatePicker,
  Alert,
  Button,
  Steps,
  Typography,
  message,
} from 'antd';
import { SendOutlined, CalendarOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import CronInput from './CronInput';
import type { DataSetSchemaField, ReportSchedule } from '@/services/reportSchedule';
import {
  getDataSetSchema,
  getValidDataSetList,
  type ValidDataSetItem,
} from '@/services/reportSchedule';
import { getConfigList, DeliveryConfig, DELIVERY_TYPE_MAP } from '@/services/deliveryConfig';
import QueryConfigFormSection, {
  type QueryType,
  type QueryDimensionFilterFormItem,
  type QueryAggregatorFormItem,
  type QueryOrderFormItem,
  type QueryMetricFilterFormItem,
} from '@/components/QueryConfigFormSection';

const { RangePicker } = DatePicker;
const { Text, Paragraph } = Typography;
const DEFAULT_DETAIL_LIMIT = 500;
const STEP_ITEMS = [
  {
    title: '基本信息',
    description: '先确定任务名称和数据集',
  },
  {
    title: '查询配置',
    description: '设置时间范围、分组和筛选规则',
  },
  {
    title: '执行与推送',
    description: '配置调度频率、输出和投递渠道',
  },
] as const;
const STEP_FIELD_NAMES = [
  ['name', 'datasetId'],
  [
    'dateField',
    'dateMode',
    'dateRange',
    'recentUnit',
    'recentPeriod',
    'queryType',
    'queryGroups',
    'queryAggregators',
    'queryDimensionFilters',
    'queryLimit',
    'queryOrders',
    'queryMetricFilters',
  ],
  ['cronExpression', 'outputFormat', 'retryCount', 'retryInterval', 'enabled', 'deliveryConfigIds'],
] as const;

const sectionStyle: React.CSSProperties = {
  border: '1px solid #e5e7eb',
  borderRadius: 12,
  padding: 20,
  background: '#fff',
  boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04)',
};

const sectionHeaderStyle: React.CSSProperties = {
  marginBottom: 18,
};

const formRowStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
  gap: 16,
};

const modalLayoutStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'minmax(0, 1fr) 260px',
  gap: 16,
  alignItems: 'start',
};

const summaryStyle: React.CSSProperties = {
  ...sectionStyle,
  position: 'sticky',
  top: 0,
};

interface ScheduleFormProps {
  visible: boolean;
  record?: ReportSchedule;
  initialDatasetId?: number;
  onCancel: () => void;
  onSubmit: (values: Partial<ReportSchedule>) => void;
}

type DateMode = 'BETWEEN' | 'RECENT';

const ScheduleForm: React.FC<ScheduleFormProps> = ({
  visible,
  record,
  initialDatasetId,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();
  const isEdit = !!record?.id;
  const [deliveryConfigs, setDeliveryConfigs] = useState<DeliveryConfig[]>([]);
  const [loadingConfigs, setLoadingConfigs] = useState(false);
  const [dataSets, setDataSets] = useState<ValidDataSetItem[]>([]);
  const [loadingDataSets, setLoadingDataSets] = useState(false);
  const [dateMode, setDateMode] = useState<DateMode>('BETWEEN');
  const [queryType, setQueryType] = useState<QueryType>('DETAIL');
  const [noPartitionDim, setNoPartitionDim] = useState(false);
  const [currentDimensions, setCurrentDimensions] = useState<DataSetSchemaField[]>([]);
  const [currentMetrics, setCurrentMetrics] = useState<DataSetSchemaField[]>([]);
  const [currentStep, setCurrentStep] = useState(0);

  useEffect(() => {
    if (visible) {
      setCurrentStep(0);
      fetchDeliveryConfigs();
      fetchValidDataSets();
    }
  }, [visible]);

  const fetchDatasetDimensions = async (datasetId: number) => {
    try {
      const res = await getDataSetSchema(datasetId);
      const payload: any = res?.data ?? res;
      const rawDims = Array.isArray(payload?.dimensions) ? payload.dimensions : [];
      const rawMetrics = Array.isArray(payload?.metrics) ? payload.metrics : [];
      const dimensions: DataSetSchemaField[] = rawDims.filter((field: any) => field.bizName);
      const metrics: DataSetSchemaField[] = rawMetrics.filter((field: any) => field.bizName);
      setCurrentDimensions(dimensions);
      setCurrentMetrics(metrics);
      return dimensions;
    } catch (error) {
      console.error('Failed to load dataset schema', error);
      setCurrentDimensions([]);
      setCurrentMetrics([]);
      return [];
    }
  };

  const handleDatasetChange = async (datasetId: number) => {
    const ds = dataSets.find((d) => d.id === datasetId);
    form.setFieldsValue({
      queryGroups: [],
      queryAggregators: [],
      queryDimensionFilters: [],
      queryOrders: [],
      queryMetricFilters: [],
    });
    if (ds?.partitionDimension) {
      form.setFieldsValue({ dateField: ds.partitionDimension });
      setNoPartitionDim(false);
    } else {
      form.setFieldsValue({ dateField: undefined });
      setNoPartitionDim(true);
    }
    await fetchDatasetDimensions(datasetId);
  };

  const fetchValidDataSets = async () => {
    setLoadingDataSets(true);
    try {
      const res = await getValidDataSetList();
      const list = res?.data ?? res;
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
      const pageData = res?.data ?? res;
      const enabledConfigs = (pageData?.records || []).filter((c: DeliveryConfig) => c.enabled);
      setDeliveryConfigs(enabledConfigs);
    } catch (error) {
      console.error('Failed to load delivery configs', error);
    } finally {
      setLoadingConfigs(false);
    }
  };

  const parseQueryConfig = (qc: string | undefined): any => {
    if (!qc) return {};
    try {
      return JSON.parse(qc);
    } catch {
      return {};
    }
  };

  const parseDateInfoFromQueryConfig = (qc: string | undefined) => {
    if (!qc) return {};
    try {
      const dateInfo = JSON.parse(qc)?.dateInfo;
      if (!dateInfo) return {};
      const result: Record<string, any> = { dateField: dateInfo.dateField };
      if (dateInfo.dateMode === 'RECENT') {
        result.dateMode = 'RECENT';
        result.recentUnit = dateInfo.unit ?? 7;
        result.recentPeriod = dateInfo.period ?? 'DAY';
      } else if (dateInfo.dateMode === 'ALL') {
        result.dateMode = 'ALL';
      } else {
        result.dateMode = 'BETWEEN';
        if (dateInfo.startDate && dateInfo.endDate) {
          result.dateRange = [dayjs(dateInfo.startDate), dayjs(dateInfo.endDate)];
        }
      }
      return result;
    } catch {
      return {};
    }
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

  const parseAggregators = (qc: string | undefined): QueryAggregatorFormItem[] => {
    const parsed = parseQueryConfig(qc);
    if (!Array.isArray(parsed?.aggregators)) return [];
    return parsed.aggregators
      .map((item: any) => ({ column: item?.column, func: item?.func || 'SUM' }))
      .filter((item: QueryAggregatorFormItem) => item.column);
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

  useEffect(() => {
    if (visible) {
      if (record) {
        const configIds = record.deliveryConfigIds
          ? record.deliveryConfigIds
              .split(',')
              .map((id) => parseInt(id.trim(), 10))
              .filter((id) => !isNaN(id))
          : [];
        const dateFields = parseDateInfoFromQueryConfig(record.queryConfig);
        const parsedQC: any = parseQueryConfig(record.queryConfig);
        const mode = (dateFields.dateMode === 'ALL' ? 'BETWEEN' : dateFields.dateMode) || 'BETWEEN';
        const currentQueryType: QueryType =
          parsedQC.queryType === 'AGGREGATE' ? 'AGGREGATE' : 'DETAIL';
        setDateMode(mode);
        setQueryType(currentQueryType);
        const ds = dataSets.find((d) => d.id === record.datasetId);
        setNoPartitionDim(!ds?.partitionDimension);
        form.setFieldsValue({
          ...record,
          deliveryConfigIds: configIds,
          dateMode: mode,
          dateField: dateFields.dateField,
          dateRange: dateFields.dateRange,
          recentUnit: dateFields.recentUnit ?? 7,
          recentPeriod: dateFields.recentPeriod ?? 'DAY',
          queryType: currentQueryType,
          queryGroups: Array.isArray(parsedQC.groups) ? parsedQC.groups : undefined,
          queryLimit:
            typeof parsedQC.limit === 'number' && parsedQC.limit > 0
              ? parsedQC.limit
              : DEFAULT_DETAIL_LIMIT,
          queryDimensionFilters: parseDimensionFilters(record.queryConfig),
          queryAggregators: parseAggregators(record.queryConfig),
          queryOrders: parseOrders(record.queryConfig),
          queryMetricFilters: parseMetricFilters(record.queryConfig),
        });
      } else {
        setDateMode('BETWEEN');
        setQueryType('DETAIL');
        setCurrentDimensions([]);
        setCurrentMetrics([]);
        form.resetFields();
        form.setFieldsValue({
          retryCount: 3,
          retryInterval: 30,
          outputFormat: 'EXCEL',
          enabled: true,
          deliveryConfigIds: [],
          dateMode: 'BETWEEN',
          recentUnit: 7,
          recentPeriod: 'DAY',
          queryType: 'DETAIL',
          queryLimit: DEFAULT_DETAIL_LIMIT,
          queryDimensionFilters: [],
          queryAggregators: [],
          queryOrders: [],
          queryMetricFilters: [],
          ...(initialDatasetId !== undefined ? { datasetId: initialDatasetId } : {}),
        });
      }
    }
  }, [visible, record, initialDatasetId, form, dataSets]);

  useEffect(() => {
    if (!visible || dataSets.length === 0) return;
    const datasetId = record?.datasetId ?? initialDatasetId;
    if (!datasetId) return;
    handleDatasetChange(datasetId);
  }, [visible, dataSets, record?.datasetId, initialDatasetId]);

  const validateCurrentStep = async () => {
    const fieldNames = [...STEP_FIELD_NAMES[currentStep]].filter((f) => {
      if (currentStep !== 1) return true;
      if (f === 'dateRange' && dateMode !== 'BETWEEN') return false;
      if ((f === 'recentUnit' || f === 'recentPeriod') && dateMode !== 'RECENT') return false;
      return true;
    });
    await form.validateFields(fieldNames as string[]);
  };

  const handleNextStep = async () => {
    await validateCurrentStep();
    setCurrentStep((prev) => Math.min(prev + 1, STEP_ITEMS.length - 1));
  };

  const handleStepChange = async (nextStep: number) => {
    if (nextStep === currentStep) {
      return;
    }
    if (nextStep > currentStep) {
      await validateCurrentStep();
    }
    setCurrentStep(nextStep);
  };

  const handleOk = async () => {
    const values = await form.validateFields();

    const dateInfoObj: Record<string, any> = {
      dateMode: values.dateMode || 'BETWEEN',
      dateField: values.dateField,
    };
    if (values.dateMode === 'BETWEEN' && values.dateRange) {
      dateInfoObj.startDate = dayjs(values.dateRange[0]).format('YYYY-MM-DD');
      dateInfoObj.endDate = dayjs(values.dateRange[1]).format('YYYY-MM-DD');
      dateInfoObj.period = 'DAY';
    } else if (values.dateMode === 'RECENT') {
      dateInfoObj.unit = values.recentUnit ?? 7;
      dateInfoObj.period = values.recentPeriod ?? 'DAY';
    }

    const schemaDimensions =
      currentDimensions.length > 0 || !values.datasetId
        ? currentDimensions
        : await fetchDatasetDimensions(values.datasetId);

    const normalizedDimensions = schemaDimensions.map((field) => ({
      id: field.id,
      name: field.name,
      bizName: field.bizName,
    }));

    if (normalizedDimensions.length === 0) {
      message.error('当前数据集列信息获取失败，暂无法创建明细调度');
      return;
    }

    const groups =
      Array.isArray(values.queryGroups) && values.queryGroups.length > 0
        ? values.queryGroups
        : values.queryType === 'AGGREGATE'
        ? []
        : normalizedDimensions.map((d: any) => d?.bizName || d?.name).filter(Boolean);

    const aggregators = Array.isArray(values.queryAggregators)
      ? values.queryAggregators
          .filter((item: QueryAggregatorFormItem) => item?.column)
          .map((item: QueryAggregatorFormItem) => ({
            column: item.column,
            func: item.func || 'SUM',
          }))
      : [];

    if (values.queryType === 'AGGREGATE' && aggregators.length === 0) {
      message.error('聚合查询至少需要配置一个聚合指标');
      return;
    }
    if (
      values.queryType === 'AGGREGATE' &&
      new Set(aggregators.map((item: QueryAggregatorFormItem) => `${item.column}-${item.func}`))
        .size !== aggregators.length
    ) {
      message.error('聚合指标存在重复配置，请调整后再保存');
      return;
    }

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
            if (['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'GREATER_THAN')) {
              return true;
            }
            return !!item?.value?.trim();
          })
          .map((item: QueryMetricFilterFormItem) => {
            const isMultiValue = ['IN', 'NOT_IN'].includes(item.operator || 'GREATER_THAN');
            const isNoValueOperator = ['IS_NULL', 'IS_NOT_NULL'].includes(
              item.operator || 'GREATER_THAN',
            );
            const rawValue = item.value?.trim() || '';
            return {
              bizName: item.bizName,
              name: item.bizName,
              operator: item.operator || 'GREATER_THAN',
              value: isNoValueOperator
                ? null
                : isMultiValue
                ? rawValue
                    .split(',')
                    .map((part: string) => part.trim())
                    .filter(Boolean)
                : rawValue,
            };
          })
      : [];

    const finalQueryConfig: Record<string, any> = {
      queryType: values.queryType || 'DETAIL',
      dimensions: normalizedDimensions,
      groups,
      aggregators: values.queryType === 'AGGREGATE' ? aggregators : [],
      limit:
        typeof values.queryLimit === 'number' && values.queryLimit > 0
          ? values.queryLimit
          : DEFAULT_DETAIL_LIMIT,
      dimensionFilters: Array.isArray(values.queryDimensionFilters)
        ? values.queryDimensionFilters
            .filter((item: QueryDimensionFilterFormItem) => {
              if (!item?.bizName) return false;
              if (['IS_NULL', 'IS_NOT_NULL'].includes(item.operator || 'EQUALS')) {
                return true;
              }
              return !!item?.value?.trim();
            })
            .map((item: QueryDimensionFilterFormItem) => {
              const matchedDimension = normalizedDimensions.find(
                (dimension: any) => dimension?.bizName === item.bizName,
              );
              const isMultiValue = ['IN', 'NOT_IN'].includes(item.operator || 'EQUALS');
              const isNoValueOperator = ['IS_NULL', 'IS_NOT_NULL'].includes(
                item.operator || 'EQUALS',
              );
              const rawValue = item.value?.trim() || '';
              return {
                bizName: item.bizName,
                name: matchedDimension?.name || item.bizName,
                operator: item.operator || 'EQUALS',
                value: isNoValueOperator
                  ? null
                  : isMultiValue
                  ? rawValue
                      .split(',')
                      .map((part) => part.trim())
                      .filter(Boolean)
                  : rawValue,
              };
            })
        : [],
      dateInfo: dateInfoObj,
    };

    if (orders.length > 0) {
      finalQueryConfig.orders = orders;
    }
    if (metricFilters.length > 0) {
      finalQueryConfig.metricFilters = metricFilters;
    }

    const configIds = values.deliveryConfigIds;
    const submitValues = {
      name: values.name,
      datasetId: values.datasetId,
      queryConfig: JSON.stringify(finalQueryConfig),
      cronExpression: values.cronExpression,
      outputFormat: values.outputFormat,
      retryCount: values.retryCount,
      retryInterval: values.retryInterval,
      enabled: values.enabled,
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

  const renderStepHeading = (title: string, description: string) => (
    <div style={sectionHeaderStyle}>
      <Text strong style={{ display: 'block', fontSize: 16, color: '#111827' }}>
        {title}
      </Text>
      <Text type="secondary">{description}</Text>
    </div>
  );

  const renderBasicInfoStep = () => (
    <div style={sectionStyle}>
      {renderStepHeading('任务基础信息', '先选择数据集，再进入查询配置。')}
      <div style={formRowStyle}>
        <Form.Item
          name="name"
          label="任务名称"
          rules={[{ required: true, message: '请输入任务名称' }]}
          style={{ marginBottom: 0 }}
        >
          <Input placeholder="如: GMV 日报" />
        </Form.Item>
        <Form.Item
          name="datasetId"
          label="关联数据集"
          rules={[{ required: true, message: '请选择数据集' }]}
          style={{ marginBottom: 0 }}
        >
          <Select
            placeholder="请选择已配置的数据集"
            allowClear
            showSearch
            optionFilterProp="label"
            loading={loadingDataSets}
            onChange={handleDatasetChange}
            options={dataSets.map((d) => ({ label: `${d.name} (ID: ${d.id})`, value: d.id }))}
          />
        </Form.Item>
      </div>
      <Paragraph type="secondary" style={{ marginTop: 16, marginBottom: 0 }}>
        数据集决定可用的维度、指标和日期分区字段。切换数据集后，已填写的分组、聚合和筛选条件会自动清空。
      </Paragraph>
    </div>
  );

  const renderQueryStep = () => (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <div style={sectionStyle}>
        {renderStepHeading('日期范围', '先确定这份调度任务每次查询的数据窗口。')}

        {noPartitionDim && (
          <Alert
            type="warning"
            showIcon
            message="该数据集未配置日期分区，请手动输入日期字段名"
            style={{ marginBottom: 16 }}
          />
        )}

        {noPartitionDim ? (
          <Form.Item
            name="dateField"
            label="日期字段名"
            rules={[{ required: true, message: '请输入日期字段名' }]}
            style={{ marginBottom: 16 }}
          >
            <Input placeholder="如: imp_date, workday" />
          </Form.Item>
        ) : (
          <Form.Item name="dateField" hidden>
            <Input />
          </Form.Item>
        )}

        <Form.Item name="dateMode" label="日期模式" style={{ marginBottom: 16 }}>
          <Radio.Group
            onChange={(e) => setDateMode(e.target.value)}
            optionType="button"
            buttonStyle="solid"
          >
            <Radio.Button value="BETWEEN">固定区间</Radio.Button>
            <Radio.Button value="RECENT">最近 N 天</Radio.Button>
          </Radio.Group>
        </Form.Item>

        {dateMode === 'BETWEEN' && (
          <Form.Item
            name="dateRange"
            label="日期范围"
            rules={[{ required: true, message: '请选择日期范围' }]}
            style={{ marginBottom: 0 }}
          >
            <RangePicker style={{ width: '100%' }} />
          </Form.Item>
        )}

        {dateMode === 'RECENT' && (
          <div style={formRowStyle}>
            <Form.Item
              name="recentUnit"
              label="最近"
              rules={[{ required: true, message: '请输入天数' }]}
              style={{ marginBottom: 0 }}
            >
              <InputNumber min={1} max={365} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="recentPeriod" label="时间粒度" style={{ marginBottom: 0 }}>
              <Select
                options={[
                  { label: '天', value: 'DAY' },
                  { label: '周', value: 'WEEK' },
                  { label: '月', value: 'MONTH' },
                ]}
              />
            </Form.Item>
          </div>
        )}
      </div>

      <div style={sectionStyle}>
        {renderStepHeading(
          '查询规则',
          '按接近 Metabase 的顺序配置查询类型、分组、聚合、筛选和高级规则。',
        )}
        <QueryConfigFormSection
          form={form}
          queryType={queryType}
          setQueryType={setQueryType}
          currentDimensions={currentDimensions}
          currentMetrics={currentMetrics}
          queryTypeOptions={[
            { label: '明细导出', value: 'DETAIL' },
            { label: '聚合报表', value: 'AGGREGATE' },
          ]}
          groupsPlaceholder="选择分组字段，默认使用全部维度"
          metricsPlaceholder="选择指标"
          limitMax={100000}
          limitTooltip="默认 500。适合控制导出明细量，避免任务一次拉取过多数据。"
        />
      </div>
    </Space>
  );

  const renderExecutionStep = () => (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <div style={sectionStyle}>
        {renderStepHeading('执行策略', '设置任务何时运行、以什么格式导出，以及失败后的重试策略。')}
        <Form.Item
          name="cronExpression"
          label="调度频率"
          rules={[{ required: true, message: '请设置 Cron 表达式' }]}
        >
          <CronInput />
        </Form.Item>
        <div style={formRowStyle}>
          <Form.Item name="outputFormat" label="输出格式" style={{ marginBottom: 0 }}>
            <Select
              options={[
                { label: 'Excel', value: 'EXCEL' },
                { label: 'CSV', value: 'CSV' },
                { label: 'JSON', value: 'JSON' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="enabled"
            label="启用任务"
            valuePropName="checked"
            style={{ marginBottom: 0 }}
          >
            <Switch />
          </Form.Item>
          <Form.Item name="retryCount" label="重试次数" style={{ marginBottom: 0 }}>
            <InputNumber min={0} max={5} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="retryInterval" label="重试间隔(秒)" style={{ marginBottom: 0 }}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </div>
      </div>

      <div style={sectionStyle}>
        {renderStepHeading('推送配置', '选择报表生成后的投递渠道，可留空，后续也可以补充。')}
        <Form.Item
          name="deliveryConfigIds"
          label="推送渠道"
          extra="支持多选。生成完成后将自动投递到所选渠道。"
          style={{ marginBottom: 0 }}
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
      </div>
    </Space>
  );

  const renderStepContent = () => {
    if (currentStep === 0) {
      return renderBasicInfoStep();
    }
    if (currentStep === 1) {
      return renderQueryStep();
    }
    return renderExecutionStep();
  };

  const formValues = Form.useWatch([], form) || {};
  const currentDataSet = dataSets.find((item) => item.id === formValues.datasetId);
  const selectedDeliveryConfigs = Array.isArray(formValues.deliveryConfigIds)
    ? deliveryConfigs.filter((config) => formValues.deliveryConfigIds.includes(config.id))
    : [];
  const queryGroupsCount = Array.isArray(formValues.queryGroups)
    ? formValues.queryGroups.length
    : 0;
  const queryAggregatorCount = Array.isArray(formValues.queryAggregators)
    ? formValues.queryAggregators.filter((item: QueryAggregatorFormItem) => item?.column).length
    : 0;
  const dimensionFilterCount = Array.isArray(formValues.queryDimensionFilters)
    ? formValues.queryDimensionFilters.filter((item: QueryDimensionFilterFormItem) => item?.bizName)
        .length
    : 0;
  const orderCount = Array.isArray(formValues.queryOrders)
    ? formValues.queryOrders.filter((item: QueryOrderFormItem) => item?.column).length
    : 0;
  const metricFilterCount = Array.isArray(formValues.queryMetricFilters)
    ? formValues.queryMetricFilters.filter((item: QueryMetricFilterFormItem) => item?.bizName)
        .length
    : 0;

  const renderSummaryRow = (label: string, value: React.ReactNode) => (
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        gap: 12,
        alignItems: 'flex-start',
      }}
    >
      <Text type="secondary">{label}</Text>
      <Text style={{ textAlign: 'right', color: '#111827' }}>{value}</Text>
    </div>
  );

  const renderSummary = () => (
    <div style={summaryStyle}>
      {renderStepHeading('当前配置摘要', '不切回上一步，也能看到本次任务的关键配置。')}
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        {renderSummaryRow('任务名称', formValues.name || '未填写')}
        {renderSummaryRow('数据集', currentDataSet?.name || '未选择')}
        {renderSummaryRow(
          '查询类型',
          formValues.queryType === 'AGGREGATE' ? '聚合报表' : '明细导出',
        )}
        {renderSummaryRow('日期模式', formValues.dateMode === 'RECENT' ? '最近 N 天' : '固定区间')}
        {renderSummaryRow('分组字段', queryGroupsCount > 0 ? `${queryGroupsCount} 项` : '默认')}
        {renderSummaryRow(
          '聚合指标',
          queryAggregatorCount > 0 ? `${queryAggregatorCount} 项` : '无',
        )}
        {renderSummaryRow(
          '维度筛选',
          dimensionFilterCount > 0 ? `${dimensionFilterCount} 条` : '无',
        )}
        {renderSummaryRow('排序规则', orderCount > 0 ? `${orderCount} 条` : '无')}
        {renderSummaryRow('指标筛选', metricFilterCount > 0 ? `${metricFilterCount} 条` : '无')}
        {renderSummaryRow('调度频率', formValues.cronExpression || '未设置')}
        {renderSummaryRow(
          '推送渠道',
          selectedDeliveryConfigs.length > 0 ? `${selectedDeliveryConfigs.length} 个` : '未配置',
        )}
        {selectedDeliveryConfigs.length > 0 && (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
              已选渠道
            </Text>
            <Space size={[6, 6]} wrap>
              {selectedDeliveryConfigs.map((config) => {
                const typeInfo = DELIVERY_TYPE_MAP[config.deliveryType];
                return (
                  <Tag key={config.id} color={typeInfo?.color}>
                    {config.name}
                  </Tag>
                );
              })}
            </Space>
          </div>
        )}
      </Space>
    </div>
  );

  return (
    <Modal
      title={isEdit ? '编辑调度任务' : '创建调度任务'}
      open={visible}
      onCancel={onCancel}
      width={820}
      destroyOnClose
      bodyStyle={{ paddingTop: 16, maxHeight: '72vh', overflowY: 'auto' }}
      footer={
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            width: '100%',
          }}
        >
          <Text type="secondary">
            第 {currentStep + 1} 步，共 {STEP_ITEMS.length} 步
          </Text>
          <Space>
            {currentStep > 0 && (
              <Button onClick={() => setCurrentStep((prev) => Math.max(prev - 1, 0))}>
                上一步
              </Button>
            )}
            {currentStep < STEP_ITEMS.length - 1 ? (
              <Button type="primary" onClick={handleNextStep}>
                下一步
              </Button>
            ) : (
              <Button type="primary" onClick={handleOk}>
                {isEdit ? '保存' : '创建'}
              </Button>
            )}
          </Space>
        </div>
      }
    >
      <Space direction="vertical" size={20} style={{ width: '100%' }}>
        <div
          style={{
            borderRadius: 14,
            padding: '18px 20px',
            background:
              'linear-gradient(180deg, rgba(248,250,252,0.96) 0%, rgba(255,255,255,1) 100%)',
            border: '1px solid #e5e7eb',
          }}
        >
          <Space align="start" size={12} style={{ marginBottom: 16 }}>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 10,
                background: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {currentStep < 2 ? <CalendarOutlined /> : <SendOutlined />}
            </div>
            <div>
              <Text strong style={{ display: 'block', fontSize: 16, color: '#111827' }}>
                {STEP_ITEMS[currentStep].title}
              </Text>
              <Text type="secondary">{STEP_ITEMS[currentStep].description}</Text>
            </div>
          </Space>
          <Steps
            size="small"
            current={currentStep}
            items={STEP_ITEMS as any}
            onChange={handleStepChange}
          />
        </div>
        <div style={modalLayoutStyle}>
          <Form form={form} layout="vertical">
            {renderStepContent()}
          </Form>
          {renderSummary()}
        </div>
      </Space>
    </Modal>
  );
};

export default ScheduleForm;
