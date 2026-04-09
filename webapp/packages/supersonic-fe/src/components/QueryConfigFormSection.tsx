import React from 'react';
import {
  Button,
  Collapse,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Space,
  Typography,
} from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { FormInstance } from 'antd/es/form';
import type { DataSetSchemaField } from '@/services/reportSchedule';

const { Text } = Typography;

export type QueryType = 'DETAIL' | 'AGGREGATE';

export type QueryAggregatorFormItem = {
  column?: string;
  func?: string;
};

export type QueryDimensionFilterFormItem = {
  bizName?: string;
  operator?: string;
  value?: string;
};

export type QueryOrderFormItem = {
  column?: string;
  direction?: string;
};

export type QueryMetricFilterFormItem = {
  bizName?: string;
  operator?: string;
  value?: string;
};

const AGGREGATOR_FUNCTIONS = [
  { label: '求和', value: 'SUM' },
  { label: '平均值', value: 'AVG' },
  { label: '最大值', value: 'MAX' },
  { label: '最小值', value: 'MIN' },
  { label: '计数', value: 'COUNT' },
  { label: '去重计数', value: 'COUNT_DISTINCT' },
];

const FILTER_OPERATORS = [
  { label: '=', value: 'EQUALS' },
  { label: '!=', value: 'NOT_EQUALS' },
  { label: '包含任一项', value: 'IN' },
  { label: '不包含任一项', value: 'NOT_IN' },
  { label: '包含', value: 'LIKE' },
  { label: '>', value: 'GREATER_THAN' },
  { label: '>=', value: 'GREATER_THAN_EQUALS' },
  { label: '<', value: 'MINOR_THAN' },
  { label: '<=', value: 'MINOR_THAN_EQUALS' },
  { label: '为空', value: 'IS_NULL' },
  { label: '不为空', value: 'IS_NOT_NULL' },
];

const NUMERIC_FILTER_OPERATORS = [
  'GREATER_THAN',
  'GREATER_THAN_EQUALS',
  'MINOR_THAN',
  'MINOR_THAN_EQUALS',
];

const ORDER_DIRECTIONS = [
  { label: '升序', value: 'ASC' },
  { label: '降序', value: 'DESC' },
];

type QueryConfigFormSectionProps = {
  form: FormInstance;
  queryType: QueryType;
  setQueryType: (value: QueryType) => void;
  currentDimensions: DataSetSchemaField[];
  currentMetrics: DataSetSchemaField[];
  queryTypeOptions: Array<{ label: string; value: QueryType }>;
  groupsPlaceholder: string;
  metricsPlaceholder: string;
  limitMax: number;
  limitTooltip: string;
};

const QueryConfigFormSection: React.FC<QueryConfigFormSectionProps> = ({
  form,
  queryType,
  setQueryType,
  currentDimensions,
  currentMetrics,
  queryTypeOptions,
  groupsPlaceholder,
  metricsPlaceholder,
  limitMax,
  limitTooltip,
}) => {
  const allColumns = [
    ...currentDimensions.map((f) => ({ label: `${f.name} (${f.bizName})`, value: f.bizName })),
    ...currentMetrics.map((f) => ({ label: `${f.name} (${f.bizName})`, value: f.bizName })),
  ];

  return (
    <Form.Item label="查询参数">
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        <Form.Item name="queryType" label="查询类型" style={{ marginBottom: 0 }}>
          <Radio.Group
            onChange={(e) => setQueryType(e.target.value)}
            optionType="button"
            buttonStyle="solid"
          >
            {queryTypeOptions.map((option) => (
              <Radio.Button key={option.value} value={option.value}>
                {option.label}
              </Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>

        <Form.Item name="queryGroups" label="分组字段" style={{ marginBottom: 0 }}>
          <Select
            mode="multiple"
            allowClear
            optionFilterProp="label"
            placeholder={currentDimensions.length > 0 ? groupsPlaceholder : '请先选择数据集'}
            options={currentDimensions.map((field) => ({
              label: `${field.name} (${field.bizName})`,
              value: field.bizName,
            }))}
          />
        </Form.Item>

        {queryType === 'AGGREGATE' && (
          <Form.List name="queryAggregators">
            {(fields, { add, remove }) => (
              <Space direction="vertical" style={{ width: '100%' }} size={8}>
                <Text>聚合指标</Text>
                {fields.map((field) => (
                  <Space
                    key={field.key}
                    align="start"
                    style={{ width: '100%', display: 'flex' }}
                    wrap
                  >
                    <Form.Item
                      {...field}
                      name={[field.name, 'column']}
                      rules={[
                        { required: true, message: '请选择指标' },
                        {
                          validator: (_, value) => {
                            const rows = form.getFieldValue('queryAggregators') || [];
                            const currentFunc =
                              form.getFieldValue(['queryAggregators', field.name, 'func']) || 'SUM';
                            const duplicateCount = rows.filter(
                              (item: QueryAggregatorFormItem) =>
                                item?.column === value && (item?.func || 'SUM') === currentFunc,
                            ).length;
                            if (value && duplicateCount > 1) {
                              return Promise.reject(new Error('相同指标和聚合方式无需重复添加'));
                            }
                            return Promise.resolve();
                          },
                        },
                      ]}
                      style={{ minWidth: 220, marginBottom: 0 }}
                    >
                      <Select
                        showSearch
                        optionFilterProp="label"
                        placeholder={currentMetrics.length > 0 ? metricsPlaceholder : '当前数据集暂无可用指标'}
                        options={currentMetrics.map((item) => ({
                          label: `${item.name} (${item.bizName})`,
                          value: item.bizName,
                        }))}
                      />
                    </Form.Item>
                    <Form.Item
                      {...field}
                      name={[field.name, 'func']}
                      initialValue="SUM"
                      rules={[{ required: true, message: '请选择聚合方式' }]}
                      style={{ width: 160, marginBottom: 0 }}
                    >
                      <Select options={AGGREGATOR_FUNCTIONS} />
                    </Form.Item>
                    <Button icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                  </Space>
                ))}
                <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({ func: 'SUM' })} block>
                  添加聚合指标
                </Button>
              </Space>
            )}
          </Form.List>
        )}

        <Form.Item
          name="queryLimit"
          label="返回行数上限"
          tooltip={limitTooltip}
          style={{ marginBottom: 0 }}
        >
          <InputNumber min={1} max={limitMax} style={{ width: '100%' }} />
        </Form.Item>

        <Form.List name="queryDimensionFilters">
          {(fields, { add, remove }) => (
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Text>筛选条件</Text>
              {fields.map((field) => (
                <Space
                  key={field.key}
                  align="start"
                  style={{ width: '100%', display: 'flex' }}
                  wrap
                >
                  <Form.Item
                    {...field}
                    name={[field.name, 'bizName']}
                    rules={[
                      { required: true, message: '请选择字段' },
                      {
                        validator: (_, value) => {
                          const rows = form.getFieldValue('queryDimensionFilters') || [];
                          const currentOperator =
                            form.getFieldValue(['queryDimensionFilters', field.name, 'operator']) ||
                            'EQUALS';
                          const currentValue =
                            form.getFieldValue(['queryDimensionFilters', field.name, 'value']) || '';
                          const duplicateCount = rows.filter(
                            (item: QueryDimensionFilterFormItem) =>
                              item?.bizName === value &&
                              (item?.operator || 'EQUALS') === currentOperator &&
                              (item?.value || '') === currentValue,
                          ).length;
                          if (value && duplicateCount > 1) {
                            return Promise.reject(new Error('重复的筛选条件无需重复添加'));
                          }
                          return Promise.resolve();
                        },
                      },
                    ]}
                    style={{ minWidth: 180, marginBottom: 0 }}
                  >
                    <Select
                      showSearch
                      optionFilterProp="label"
                      placeholder="选择字段"
                      options={currentDimensions.map((item) => ({
                        label: `${item.name} (${item.bizName})`,
                        value: item.bizName,
                      }))}
                    />
                  </Form.Item>
                  <Form.Item
                    {...field}
                    name={[field.name, 'operator']}
                    initialValue="EQUALS"
                    style={{ width: 140, marginBottom: 0 }}
                  >
                    <Select options={FILTER_OPERATORS} />
                  </Form.Item>
                  <Form.Item
                    noStyle
                    shouldUpdate={(prevValues, curValues) =>
                      prevValues?.queryDimensionFilters?.[field.name]?.operator !==
                      curValues?.queryDimensionFilters?.[field.name]?.operator
                    }
                  >
                    {() => {
                      const currentOperator =
                        form.getFieldValue(['queryDimensionFilters', field.name, 'operator']) ||
                        'EQUALS';
                      const noValueRequired = ['IS_NULL', 'IS_NOT_NULL'].includes(currentOperator);
                      const multiValueOperator = ['IN', 'NOT_IN'].includes(currentOperator);
                      return (
                        <Form.Item
                          {...field}
                          name={[field.name, 'value']}
                          rules={
                            noValueRequired
                              ? []
                              : [
                                  { required: true, message: '请输入筛选值' },
                                  {
                                    validator: (_, value) => {
                                      if (!value?.trim()) return Promise.resolve();
                                      if (
                                        multiValueOperator &&
                                        value
                                          .split(',')
                                          .map((item: string) => item.trim())
                                          .filter(Boolean).length === 0
                                      ) {
                                        return Promise.reject(new Error('请至少输入一个有效值'));
                                      }
                                      if (
                                        NUMERIC_FILTER_OPERATORS.includes(currentOperator) &&
                                        Number.isNaN(Number(value))
                                      ) {
                                        return Promise.reject(new Error('该操作符仅支持数字值'));
                                      }
                                      return Promise.resolve();
                                    },
                                  },
                                ]
                          }
                          style={{ flex: 1, minWidth: 220, marginBottom: 0 }}
                        >
                          <Input
                            disabled={noValueRequired}
                            placeholder={
                              noValueRequired
                                ? '当前操作符无需输入值'
                                : multiValueOperator
                                  ? '输入多个值，用逗号分隔'
                                  : '输入筛选值'
                            }
                          />
                        </Form.Item>
                      );
                    }}
                  </Form.Item>
                  <Button icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                </Space>
              ))}
              <Button type="dashed" icon={<PlusOutlined />} onClick={() => add({ operator: 'EQUALS' })} block>
                添加筛选条件
              </Button>
            </Space>
          )}
        </Form.List>

        <Collapse
          size="small"
          items={[
            {
              key: 'advancedQueryConfig',
              label: '高级设置（可选）',
              children: (
                <Space direction="vertical" style={{ width: '100%' }} size={12}>
                  <Form.List name="queryOrders">
                    {(fields, { add, remove }) => (
                      <Space direction="vertical" style={{ width: '100%' }} size={8}>
                        <Text>排序规则</Text>
                        {fields.map((field) => (
                          <Space
                            key={field.key}
                            align="start"
                            style={{ width: '100%', display: 'flex' }}
                            wrap
                          >
                            <Form.Item
                              {...field}
                              name={[field.name, 'column']}
                              rules={[{ required: true, message: '请选择排序字段' }]}
                              style={{ minWidth: 220, marginBottom: 0 }}
                            >
                              <Select
                                showSearch
                                optionFilterProp="label"
                                placeholder="选择排序字段"
                                options={allColumns}
                              />
                            </Form.Item>
                            <Form.Item
                              {...field}
                              name={[field.name, 'direction']}
                              initialValue="ASC"
                              style={{ width: 120, marginBottom: 0 }}
                            >
                              <Select options={ORDER_DIRECTIONS} />
                            </Form.Item>
                            <Button icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                          </Space>
                        ))}
                        <Button
                          type="dashed"
                          icon={<PlusOutlined />}
                          onClick={() => add({ direction: 'ASC' })}
                          block
                        >
                          添加排序规则
                        </Button>
                      </Space>
                    )}
                  </Form.List>

                  <Form.List name="queryMetricFilters">
                    {(fields, { add, remove }) => (
                      <Space direction="vertical" style={{ width: '100%' }} size={8}>
                        <Text>指标筛选</Text>
                        {fields.map((field) => (
                          <Space
                            key={field.key}
                            align="start"
                            style={{ width: '100%', display: 'flex' }}
                            wrap
                          >
                            <Form.Item
                              {...field}
                              name={[field.name, 'bizName']}
                              rules={[{ required: true, message: '请选择指标' }]}
                              style={{ minWidth: 180, marginBottom: 0 }}
                            >
                              <Select
                                showSearch
                                optionFilterProp="label"
                                placeholder="选择指标"
                                options={currentMetrics.map((item) => ({
                                  label: `${item.name} (${item.bizName})`,
                                  value: item.bizName,
                                }))}
                              />
                            </Form.Item>
                            <Form.Item
                              {...field}
                              name={[field.name, 'operator']}
                              initialValue="GREATER_THAN"
                              style={{ width: 140, marginBottom: 0 }}
                            >
                              <Select options={FILTER_OPERATORS} />
                            </Form.Item>
                            <Form.Item
                              noStyle
                              shouldUpdate={(prevValues, curValues) =>
                                prevValues?.queryMetricFilters?.[field.name]?.operator !==
                                curValues?.queryMetricFilters?.[field.name]?.operator
                              }
                            >
                              {() => {
                                const currentOperator =
                                  form.getFieldValue([
                                    'queryMetricFilters',
                                    field.name,
                                    'operator',
                                  ]) || 'GREATER_THAN';
                                const noValueRequired = ['IS_NULL', 'IS_NOT_NULL'].includes(
                                  currentOperator,
                                );
                                const multiValueOperator = ['IN', 'NOT_IN'].includes(
                                  currentOperator,
                                );
                                return (
                                  <Form.Item
                                    {...field}
                                    name={[field.name, 'value']}
                                    rules={
                                      noValueRequired
                                        ? []
                                        : [{ required: true, message: '请输入筛选值' }]
                                    }
                                    style={{ flex: 1, minWidth: 160, marginBottom: 0 }}
                                  >
                                    <Input
                                      disabled={noValueRequired}
                                      placeholder={
                                        noValueRequired
                                          ? '当前操作符无需输入值'
                                          : multiValueOperator
                                            ? '输入多个值，用逗号分隔'
                                            : '输入筛选值'
                                      }
                                    />
                                  </Form.Item>
                                );
                              }}
                            </Form.Item>
                            <Button icon={<DeleteOutlined />} onClick={() => remove(field.name)} />
                          </Space>
                        ))}
                        <Button
                          type="dashed"
                          icon={<PlusOutlined />}
                          onClick={() => add({ operator: 'GREATER_THAN' })}
                          block
                        >
                          添加指标筛选
                        </Button>
                      </Space>
                    )}
                  </Form.List>
                </Space>
              ),
            },
          ]}
        />
      </Space>
    </Form.Item>
  );
};

export default QueryConfigFormSection;
