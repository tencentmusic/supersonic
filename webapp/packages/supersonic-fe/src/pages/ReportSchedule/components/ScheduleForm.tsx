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
  Radio,
  DatePicker,
  Alert,
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
import {
  getConfigList,
  DeliveryConfig,
  DELIVERY_TYPE_MAP,
} from '@/services/deliveryConfig';

const { RangePicker } = DatePicker;
const DEFAULT_DETAIL_LIMIT = 500;

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
  const [noPartitionDim, setNoPartitionDim] = useState(false);
  const [currentDimensions, setCurrentDimensions] = useState<DataSetSchemaField[]>([]);

  useEffect(() => {
    if (visible) {
      fetchDeliveryConfigs();
      fetchValidDataSets();
    }
  }, [visible]);

  const fetchDatasetDimensions = async (datasetId: number) => {
    try {
      const res = await getDataSetSchema(datasetId);
      const payload: any = res?.data ?? res;
      // DataSetSchema returns { dimensions: SchemaElement[], metrics: SchemaElement[], ... }
      const rawDims = Array.isArray(payload?.dimensions) ? payload.dimensions : [];
      const dimensions: DataSetSchemaField[] = rawDims.filter((field: any) => field.bizName);
      setCurrentDimensions(dimensions);
      return dimensions;
    } catch (error) {
      console.error('Failed to load dataset schema', error);
      setCurrentDimensions([]);
      return [];
    }
  };

  /** Auto-populate dateField when dataset changes */
  const handleDatasetChange = async (datasetId: number) => {
    const ds = dataSets.find((d) => d.id === datasetId);
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
      const enabledConfigs = (pageData?.records || []).filter(
        (c: DeliveryConfig) => c.enabled,
      );
      setDeliveryConfigs(enabledConfigs);
    } catch (error) {
      console.error('Failed to load delivery configs', error);
    } finally {
      setLoadingConfigs(false);
    }
  };

  /** Extract dateInfo from existing queryConfig JSON */
  const parseDateInfoFromQueryConfig = (queryConfig: string | undefined) => {
    if (!queryConfig) return {};
    try {
      const parsed = JSON.parse(queryConfig);
      const dateInfo = parsed?.dateInfo;
      if (!dateInfo) return {};
      const result: Record<string, any> = {
        dateField: dateInfo.dateField,
      };
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

  /** Remove dateInfo from queryConfig JSON (we build it from form fields) */
  const stripDateInfoFromQueryConfig = (queryConfig: string | undefined): any => {
    if (!queryConfig) return {};
    try {
      const parsed = JSON.parse(queryConfig);
      const { dateInfo, ...rest } = parsed;
      return rest;
    } catch {
      return {};
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
        const dateFields = parseDateInfoFromQueryConfig(record.queryConfig);
        const mode = (dateFields.dateMode === 'ALL' ? 'BETWEEN' : dateFields.dateMode) || 'BETWEEN';
        setDateMode(mode);
        // Check if dataset has partition dimension
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
        });
      } else {
        setDateMode('BETWEEN');
        setCurrentDimensions([]);
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
          ...(initialDatasetId !== undefined ? { datasetId: initialDatasetId } : {}),
        });
      }
    }
  }, [visible, record, initialDatasetId, form, dataSets]);

  useEffect(() => {
    if (!visible || dataSets.length === 0) {
      return;
    }
    const datasetId = record?.datasetId ?? initialDatasetId;
    if (!datasetId) {
      return;
    }
    handleDatasetChange(datasetId);
  }, [visible, dataSets, record?.datasetId, initialDatasetId]);

  const handleOk = async () => {
    const values = await form.validateFields();

    // Build dateInfo from form fields
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

    // Merge dateInfo into queryConfig
    const baseConfig = stripDateInfoFromQueryConfig(values.queryConfig);
    const schemaDimensions =
      currentDimensions.length > 0 || !values.datasetId
        ? currentDimensions
        : await fetchDatasetDimensions(values.datasetId);
    const normalizedDimensions =
      Array.isArray(baseConfig.dimensions) && baseConfig.dimensions.length > 0
        ? baseConfig.dimensions
        : schemaDimensions.map((field) => ({
            id: field.id,
            name: field.name,
            bizName: field.bizName,
          }));

    if (normalizedDimensions.length === 0) {
      message.error('当前数据集列信息获取失败，暂无法创建明细调度');
      return;
    }

    const groups =
      Array.isArray(baseConfig.groups) && baseConfig.groups.length > 0
        ? baseConfig.groups
        : normalizedDimensions
            .map((dimension: any) => dimension?.bizName || dimension?.name)
            .filter(Boolean);

    const finalQueryConfig = {
      ...baseConfig,
      queryType: baseConfig.queryType || 'DETAIL',
      dimensions: normalizedDimensions,
      groups,
      limit:
        typeof baseConfig.limit === 'number' && baseConfig.limit > 0
          ? baseConfig.limit
          : DEFAULT_DETAIL_LIMIT,
      dateInfo: dateInfoObj,
    };

    // Convert deliveryConfigIds array to comma-separated string
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
        Array.isArray(configIds) && configIds.length > 0
          ? configIds.join(',')
          : undefined,
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
      title={isEdit ? '编辑调度任务' : '创建调度任务'}
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      width={640}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="任务名称"
          rules={[{ required: true, message: '请输入任务名称' }]}
        >
          <Input placeholder="如: GMV 日报" />
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
            onChange={handleDatasetChange}
            options={dataSets.map((d) => ({ label: `${d.name} (ID: ${d.id})`, value: d.id }))}
          />
        </Form.Item>

        <Divider>
          <Space>
            <CalendarOutlined />
            <span>查询日期配置</span>
          </Space>
        </Divider>

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
          >
            <Input placeholder="如: imp_date, workday" />
          </Form.Item>
        ) : (
          <Form.Item name="dateField" hidden>
            <Input />
          </Form.Item>
        )}

        <Form.Item name="dateMode" label="日期模式">
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
          >
            <RangePicker style={{ width: '100%' }} />
          </Form.Item>
        )}

        {dateMode === 'RECENT' && (
          <Space style={{ width: '100%' }}>
            <Form.Item
              name="recentUnit"
              label="最近"
              rules={[{ required: true, message: '请输入天数' }]}
            >
              <InputNumber min={1} max={365} />
            </Form.Item>
            <Form.Item name="recentPeriod" label="时间粒度">
              <Select style={{ width: 100 }}>
                <Select.Option value="DAY">天</Select.Option>
                <Select.Option value="WEEK">周</Select.Option>
                <Select.Option value="MONTH">月</Select.Option>
              </Select>
            </Form.Item>
          </Space>
        )}

        <Form.Item
          name="queryConfig"
          label="其他查询参数 (JSON, 可选)"
          extra="可配置 dimensions、aggregators、dimensionFilters 等，dateInfo 会自动合并"
        >
          <Input.TextArea
            rows={3}
            placeholder='{"groups": ["city"], "aggregators": [], "limit": 10000}'
          />
        </Form.Item>

        <Form.Item
          name="cronExpression"
          label="调度频率"
          rules={[{ required: true, message: '请设置 Cron 表达式' }]}
        >
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
