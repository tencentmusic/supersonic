import { Flex, Input, InputNumber, Tooltip } from 'antd';
import { useMemo } from 'react';
import Select from '../components/Select';
import { useDatasetInfo } from '../hooks/useDatasetInfo';
import { useDimensionValueOptions } from '../hooks/useDimensionValueOptions';
import { useFieldOptions } from '../hooks/useFieldOptions';
import { useOptions } from '../hooks/useOptions';
import type { FilterCondition } from '../types';

export function getLabelReactNode({ label, type }: { value: string; label: string; type: string }) {
  const color = type === 'string' ? '#6699e0' : '#6cbda9';
  const prefixIcon = type === 'string' ? 'ABC' : '123';
  return (
    <>
      <span style={{ color, fontSize: '10px', fontWeight: 700 }}>{prefixIcon}</span> {label}
    </>
  );
}

type Props = {
  data: FilterCondition;
  onChange: (data: FilterCondition) => void;
};

export default function ConditionItemFilter({ data, onChange }: Props) {
  const stringOperatorOptions = useOptions('string');

  const numberOperatorOptions = useOptions('number');

  const { getTypeByBizName: getFieldType } = useDatasetInfo();

  const stringValueOptions = useDimensionValueOptions(data.field!);

  const fieldOptions = useFieldOptions();

  const fieldOptionsWithLabel = useMemo(
    () =>
      fieldOptions.map(option => ({
        ...option,
        originLabel: option.label,
        label: getLabelReactNode(option),
      })),
    [fieldOptions]
  );

  const handleFieldChange = (field: string) => {
    onChange({
      ...data,
      field,
      fieldType: getFieldType(field) as any,
      operator: data.fieldType === getFieldType(field) ? data.operator : undefined,
      value: null,
    });
  };

  const handleOperatorChange = (operator: string) => {
    onChange({
      ...data,
      operator,
      value: null,
    });
  };

  const handleValueChange = (value: string) => {
    // @ts-ignore
    onChange({
      ...data,
      value,
    });
  };

  const handleNumberValuesChange = (value: number | null) => {
    // @ts-ignore
    onChange({
      ...data,
      value,
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
            style={{ width: '150px' }}
            value={data.field}
            options={fieldOptionsWithLabel}
            optionFilterProp="originLabel"
            onChange={handleFieldChange}
          />
        </Tooltip>
      </div>
      {data.fieldType === 'string' && (
        <>
          <div className="condition-item-base-second">
            <Select
              disabled={!data.field}
              value={data.operator}
              size="middle"
              style={{ width: '120px' }}
              options={stringOperatorOptions}
              onChange={handleOperatorChange}
            />
          </div>
          <div className="condition-item-base-third">
            {['IN', 'NOT_IN'].includes(data.operator!) && (
              <Select
                disabled={!data.field || !data.operator}
                size="middle"
                style={{ width: '100%' }}
                value={data.value ?? []}
                mode="multiple"
                allowClear
                onChange={handleValueChange}
                options={stringValueOptions}
              />
            )}
            {['='].includes(data.operator!) && (
              <Select
                disabled={!data.field || !data.operator}
                size="middle"
                style={{ width: '100%' }}
                value={data.value}
                allowClear
                onChange={handleValueChange}
                options={stringValueOptions}
              />
            )}
            {['LIKE'].includes(data.operator!) && (
              <Input
                style={{ width: 120 }}
                value={data.value as string}
                onChange={e => onChange({ ...data, value: e.target.value })}
              />
            )}
          </div>
        </>
      )}
      {data.fieldType === 'number' && (
        <>
          <div className="condition-item-base-second">
            <Select
              disabled={!data.field}
              value={data.operator}
              size="middle"
              style={{ width: '120px' }}
              options={numberOperatorOptions}
              onChange={handleOperatorChange}
            />
          </div>
          <div className="condition-item-base-third">
            <InputNumber<number>
              disabled={!data.field || !data.operator}
              size="middle"
              style={{ width: '100%' }}
              value={data.value}
              onChange={handleNumberValuesChange}
            />
          </div>
        </>
      )}
    </Flex>
  );
}
