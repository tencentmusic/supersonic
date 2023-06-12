import React from 'react';
import { Table, Select, Checkbox, Input } from 'antd';
import type { FieldItem } from '../data';
import { isUndefined } from 'lodash';
import { TYPE_OPTIONS, DATE_FORMATTER, AGG_OPTIONS, EnumDataSourceType } from '../constants';

type Props = {
  fields: FieldItem[];
  onFieldChange: (fieldName: string, data: Partial<FieldItem>) => void;
};

const { Option } = Select;

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
              // handleFieldChange(record, 'type', value);
              onFieldChange(record.bizName, {
                ...record,
                type: value,
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
      width: 100,
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
          return (
            <Select
              placeholder="时间格式"
              value={dateFormat}
              onChange={(value) => {
                handleFieldChange(record, 'dateFormat', value);
              }}
              defaultValue={DATE_FORMATTER[0]}
              style={{ width: '100%' }}
            >
              {DATE_FORMATTER.map((item) => (
                <Option key={item} value={item}>
                  {item}
                </Option>
              ))}
            </Select>
          );
        }
        return <></>;
      },
    },
    {
      title: '快速创建',
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
          const isCreateName = [EnumDataSourceType.CATEGORICAL, EnumDataSourceType.TIME].includes(
            type as EnumDataSourceType,
          )
            ? 'isCreateDimension'
            : 'isCreateMetric';
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
                    [isCreateName]: value,
                  });
                } else {
                  handleFieldChange(record, isCreateName, value);
                }
              }}
            >
              <Input
                value={name}
                disabled={!editState}
                onChange={(e) => {
                  const value = e.target.value;
                  handleFieldChange(record, 'name', value);
                }}
                placeholder="请输入中文名"
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
      <Table<FieldItem>
        dataSource={fields}
        columns={columns}
        className="fields-table"
        rowKey="bizName"
        pagination={false}
        scroll={{ y: 500 }}
      />
    </>
  );
};

export default FieldForm;
