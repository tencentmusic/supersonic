import { Flex, Tooltip } from 'antd';
import { useMemo } from 'react';
import Select from '../components/Select';
import { useDatasetInfo } from '../hooks/useDatasetInfo';
import { useFieldOptions } from '../hooks/useFieldOptions';
import { useOptions } from '../hooks/useOptions';
import type { ColumnCondition } from '../types';
import { getLabelReactNode } from './ConditionItemFilter';

type Props = {
  data: ColumnCondition;
  onChange: (data: ColumnCondition) => void;
};

const columnOperatorOptions = [
  {
    label: '聚合',
    value: 'agg',
  },
  {
    label: '分组',
    value: 'group',
  },
];

export default function ConditionItemColumn({ data, onChange }: Props) {
  const numberAggOptions = useOptions('number-aggregation');

  const stringAggOptions = useOptions('string-aggregation');

  const { getTypeByBizName } = useDatasetInfo();

  const fieldOptions = useFieldOptions();

  const fieldOptionsWithLabel = useMemo(
    () =>
      fieldOptions.map(option => ({
        ...option,
        label: getLabelReactNode(option),
      })),
    [fieldOptions]
  );

  const handleFieldChange = (field: string) => {
    const fieldType: any = getTypeByBizName(field);
    onChange({
      ...data,
      field,
      fieldType,
      operator: fieldType === 'number' ? 'agg' : 'group',
      value: null,
    });
  };

  const getLabel = (value: string) => {
    const option = fieldOptions.find(option => option.value === value);
    return option?.label;
  };

  return (
    <Flex className="condition-item-base" gap={5}>
      <div className="condition-item-base-first">
        <Tooltip title={getLabel(data.field!)}>
          <Select
            size="middle"
            value={data.field}
            style={{ width: '150px' }}
            options={fieldOptionsWithLabel}
            onChange={handleFieldChange}
          />
        </Tooltip>
      </div>
      <div className="condition-item-base-second">
        <Select
          size="middle"
          style={{ width: '120px' }}
          value={data.operator}
          disabled={!data.field}
          onChange={operator => onChange({ ...data, operator })}
          options={columnOperatorOptions}
        />
      </div>
      {data.operator === 'agg' && (
        <div className="condition-item-base-third">
          <Select
            value={data.value}
            size="middle"
            style={{ width: '100%' }}
            options={data.fieldType === 'number' ? numberAggOptions : stringAggOptions}
            onChange={value => onChange({ ...data, value })}
          />
        </div>
      )}
    </Flex>
  );
}
