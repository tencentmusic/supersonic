import React from 'react';
import { Table, Select, Checkbox, Input, Alert, Space, Tooltip } from 'antd';
import TableTitleTooltips from '../../components/TableTitleTooltips';
import { isUndefined } from 'lodash';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import Marquee from 'react-fast-marquee';
import {
  TYPE_OPTIONS,
  DATE_FORMATTER,
  AGG_OPTIONS,
  EnumDataSourceType,
  DATE_OPTIONS,
} from '../constants';
import styles from '../style.less';

type FieldItem = {
  bizName: string;
  sqlType: string;
  name: string;
  type: EnumDataSourceType;
  agg?: string;
  checked?: number;
  dateFormat?: string;
  timeGranularity?: string;
};

type Props = {
  fields: FieldItem[];
  onFieldChange: (fieldName: string, data: Partial<FieldItem>) => void;
};

const { Option } = Select;

const getCreateFieldName = (type: EnumDataSourceType) => {
  const isCreateName = [EnumDataSourceType.CATEGORICAL, EnumDataSourceType.TIME].includes(
    type as EnumDataSourceType,
  )
    ? 'isCreateDimension'
    : 'isCreateMetric';
  return isCreateName;
  // const editState = !isUndefined(record[isCreateName]) ? !!record[isCreateName] : true;
};

const FieldForm: React.FC<Props> = ({ fields, onFieldChange }) => {
  const handleFieldChange = (record: FieldItem, fieldName: string, value: any) => {
    onFieldChange(record.bizName, {
      ...record,
      [fieldName]: value,
    });
  };

  const columns = [
    {
      title: '字段名称',
      dataIndex: 'bizName',
      width: 100,
    },
    {
      title: '数据类型',
      dataIndex: 'sqlType',
      width: 80,
    },
    {
      title: '字段类型',
      dataIndex: 'type',
      width: 100,
      render: (_: any, record: FieldItem) => {
        const type = fields.find((field) => field.bizName === record.bizName)?.type;
        return (
          <Select
            placeholder="字段类型"
            value={type}
            onChange={(value) => {
              let defaultParams = {};
              if (value === EnumDataSourceType.MEASURES) {
                defaultParams = {
                  agg: AGG_OPTIONS[0].value,
                };
              } else if (value === EnumDataSourceType.TIME) {
                defaultParams = {
                  dateFormat: DATE_FORMATTER[0],
                  timeGranularity: 'day',
                };
              } else {
                defaultParams = {
                  agg: undefined,
                  dateFormat: undefined,
                  timeGranularity: undefined,
                };
              }
              const isCreateName = getCreateFieldName(value);
              const editState = !isUndefined(record[isCreateName]) ? !!record[isCreateName] : true;
              // handleFieldChange(record, 'type', value);
              onFieldChange(record.bizName, {
                ...record,
                type: value,
                name: '',
                [isCreateName]: editState,
                ...defaultParams,
              });
            }}
            style={{ width: '100%' }}
          >
            {TYPE_OPTIONS.map((item) => (
              <Option key={item.label} value={item.value}>
                {item.label}
              </Option>
            ))}
          </Select>
        );
      },
    },
    {
      title: '扩展配置',
      dataIndex: 'extender',
      width: 180,
      render: (_: any, record: FieldItem) => {
        const { type } = record;
        if (type === EnumDataSourceType.MEASURES) {
          const agg = fields.find((field) => field.bizName === record.bizName)?.agg;
          return (
            <Select
              placeholder="度量算子"
              value={agg}
              onChange={(value) => {
                handleFieldChange(record, 'agg', value);
              }}
              defaultValue={AGG_OPTIONS[0].value}
              style={{ width: '100%' }}
            >
              {AGG_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          );
        }
        if (type === EnumDataSourceType.TIME) {
          const dateFormat = fields.find((field) => field.bizName === record.bizName)?.dateFormat;
          const timeGranularity = fields.find(
            (field) => field.bizName === record.bizName,
          )?.timeGranularity;
          return (
            <Space>
              <Select
                placeholder="时间格式"
                value={dateFormat}
                onChange={(value) => {
                  handleFieldChange(record, 'dateFormat', value);
                }}
                defaultValue={DATE_FORMATTER[0]}
                style={{ minWidth: 150 }}
              >
                {DATE_FORMATTER.map((item) => (
                  <Option key={item} value={item}>
                    {item}
                  </Option>
                ))}
              </Select>
              <Tooltip title="请选择数据库中时间字段对应格式">
                <ExclamationCircleOutlined />
              </Tooltip>
              <span>时间粒度:</span>
              <Select
                placeholder="时间粒度"
                value={timeGranularity}
                onChange={(value) => {
                  handleFieldChange(record, 'timeGranularity', value);
                }}
                defaultValue={DATE_OPTIONS[0]}
                style={{ minWidth: 50 }}
              >
                {DATE_OPTIONS.map((item) => (
                  <Option key={item} value={item}>
                    {item}
                  </Option>
                ))}
              </Select>
            </Space>
          );
        }
        return <></>;
      },
    },
    {
      title: (
        <TableTitleTooltips
          title="快速创建"
          tooltips="若勾选快速创建并填写名称，将会把该维度/指标直接创建到维度/指标列表"
        />
      ),
      dataIndex: 'fastCreate',
      width: 100,
      render: (_: any, record: FieldItem) => {
        const { type, name } = record;
        if (
          [
            EnumDataSourceType.PRIMARY,
            EnumDataSourceType.FOREIGN,
            EnumDataSourceType.CATEGORICAL,
            EnumDataSourceType.TIME,
            EnumDataSourceType.MEASURES,
          ].includes(type as EnumDataSourceType)
        ) {
          const isCreateName = getCreateFieldName(type);
          const editState = !isUndefined(record[isCreateName]) ? !!record[isCreateName] : true;
          return (
            <Checkbox
              checked={editState}
              onChange={(e) => {
                const value = e.target.checked ? 1 : 0;
                if (!value) {
                  onFieldChange(record.bizName, {
                    ...record,
                    name: '',
                    checked: value,
                    [isCreateName]: value,
                  });
                } else {
                  // handleFieldChange(record, isCreateName, value);
                  onFieldChange(record.bizName, {
                    ...record,
                    checked: value,
                    [isCreateName]: value,
                  });
                }
              }}
            >
              <Input
                className={!name && styles.dataSourceFieldsName}
                value={name}
                disabled={!editState}
                onChange={(e) => {
                  const value = e.target.value;
                  // handleFieldChange(record, 'name', value);
                  onFieldChange(record.bizName, {
                    ...record,
                    name: value,
                    [isCreateName]: 1,
                  });
                }}
                placeholder="请填写名称"
              />
            </Checkbox>
          );
        }
        return <></>;
      },
    },
  ];

  return (
    <>
      <Alert
        style={{ marginBottom: '10px' }}
        banner
        message={
          <div>
            为了保障同一个模型下维度/指标列表唯一，消除歧义，若本模型下的多个数据源存在相同的字段名并且都勾选了快速创建，系统默认这些相同字段的指标维度是同一个，同时列表中将只显示最后一次创建的指标/维度。
          </div>
          // <Marquee pauseOnHover gradient={false}>
          //   为了保障同一个主题域下维度/指标列表唯一，消除歧义，若本主题域下的多个数据源存在相同的字段名并且都勾选了快速创建，系统默认这些相同字段的指标维度是同一个，同时列表中将只显示最后一次创建的指标/维度。
          // </Marquee>
        }
      />
      <Table<FieldItem>
        dataSource={fields}
        columns={columns}
        rowKey="bizName"
        pagination={false}
        scroll={{ y: 500 }}
      />
    </>
  );
};

export default FieldForm;
