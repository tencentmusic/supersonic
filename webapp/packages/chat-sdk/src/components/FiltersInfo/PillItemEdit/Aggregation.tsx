import { DeleteOutlined } from '@ant-design/icons';
import { Button, Flex, Space, Tooltip } from 'antd';
import Select from '../components/Select';
import { useDatasetInfo } from '../hooks/useDatasetInfo';
import { useFieldOptions } from '../hooks/useFieldOptions';
import { useOptions } from '../hooks/useOptions';
import { IAggregationPill } from '../types';

type Props = {
  value: IAggregationPill;
  onChange: (value: IAggregationPill) => void;
};

export default function Aggregation({ value, onChange }: Props) {
  const { getTypeByBizName } = useDatasetInfo();

  const options = useFieldOptions();

  const stringOperatorOptions = useOptions('string-aggregation');

  const numberOperatorOptions = useOptions('number-aggregation');

  const handleFieldSelect = (field: string) => v => {
    onChange({
      ...value,
      fields: value.fields.map(f => {
        if (f.field === field) {
          return {
            ...f,
            field: v,
            fieldName: options.find(o => o.value === v)?.label ?? '',
            operator: '',
          };
        }
        return f;
      }),
    });
  };

  const handleFieldOperator = (field: string) => v => {
    onChange({
      ...value,
      fields: value.fields.map(f => {
        if (f.field === field) {
          return {
            ...f,
            operator: v,
          };
        }
        return f;
      }),
    });
  };

  const onClickDelete = (field: string) => {
    onChange({
      ...value,
      fields: value.fields.filter(f => f.field !== field),
    });
  };

  const getLabelByOperatorValue = (value: string) => {
    return (
      [...stringOperatorOptions, ...numberOperatorOptions].find(option => option.value === value)
        ?.label ?? ''
    );
  };

  const getOptions = (field: string) => {
    if (field === '*') return [...options, { value: '*', label: '记录数' }];
    return options;
  };

  return (
    <Space direction="vertical">
      {value.fields.map(field => {
        return (
          <Flex gap={'small'} justify="space-between" align="center">
            <Space>
              <Select
                value={field.field}
                key={field.field}
                style={{ width: 120 }}
                options={getOptions(field.field)}
                optionFilterProp="label"
                onChange={handleFieldSelect(field.field)}
              />
              <Select
                value={field.operator}
                style={{ width: 120 }}
                options={
                  getTypeByBizName(field.field) === 'string'
                    ? stringOperatorOptions
                    : getTypeByBizName(field.field) === 'number'
                    ? numberOperatorOptions
                    : [{ value: field.operator, label: getLabelByOperatorValue(field.operator) }]
                }
                onChange={handleFieldOperator(field.field)}
              />
            </Space>
            {value.fields.length > 1 && (
              <Tooltip title="删除">
                <Button
                  className="condition-item-remove-btn"
                  type="link"
                  size="small"
                  icon={<DeleteOutlined />}
                  onClick={e => {
                    e.stopPropagation();
                    onClickDelete(field.field);
                  }}
                />
              </Tooltip>
            )}
          </Flex>
        );
      })}
    </Space>
  );
}
