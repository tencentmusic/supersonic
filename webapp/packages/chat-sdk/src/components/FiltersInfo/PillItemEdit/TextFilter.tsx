import { Input, Space } from 'antd';
import Select from '../components/Select';
import SelectWithCustomOption from '../components/SelectWithCustomOption';
import { useDemensionFieldOptions } from '../hooks/useDimensionFieldOptions';
import { useDimensionValueOptions } from '../hooks/useDimensionValueOptions';
import { useOptions } from '../hooks/useOptions';
import { ITextFilterPill } from '../types';

type Props = {
  value: ITextFilterPill;
  onChange: (value: ITextFilterPill) => void;
};

export default function TextFilter({ value, onChange }: Props) {
  const fieldOptions = useDemensionFieldOptions();

  const operatorOptions = useOptions('string');

  const valueOptions = useDimensionValueOptions(value.field);

  const handleFieldSelect = v =>
    onChange({
      ...value,
      field: v,
      fieldName: fieldOptions.find(o => o.value === v)?.label ?? '',
      value: null,
    });

  const mode = ['IN', 'NOT_IN'].includes(value.operator) ? 'multiple' : undefined;

  const selectMode = ['IN', 'NOT_IN', '='].includes(value.operator);

  const inputMode = ['LIKE'].includes(value.operator);

  return (
    <Space>
      <Select
        value={value.field}
        style={{ width: 150 }}
        options={fieldOptions}
        optionFilterProp="label"
        onChange={handleFieldSelect}
      />

      <Select
        value={value.operator}
        style={{ width: 120 }}
        options={operatorOptions}
        onChange={v => onChange({ ...value, operator: v, value: null })}
      />

      {selectMode && (
        <SelectWithCustomOption
          style={{ width: 200 }}
          value={value.value}
          options={valueOptions}
          mode={mode}
          allowClear
          onChange={v => onChange({ ...value, value: v })}
        />
      )}

      {inputMode && (
        <Input
          style={{ width: 200 }}
          defaultValue={value.value as string}
          // value={value.value as string}
          onChange={e => onChange({ ...value, value: e.target.value })}
        />
      )}
    </Space>
  );
}
