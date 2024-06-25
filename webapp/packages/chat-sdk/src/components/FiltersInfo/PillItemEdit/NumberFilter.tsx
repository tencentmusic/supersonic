import { InputNumber, Space } from 'antd';
import { memo } from 'react';
import Select from '../components/Select';
import { useMetricsFieldOptions } from '../hooks/useMetricsFieldOptions';
import { useOptions } from '../hooks/useOptions';
import { INumberFilterPill } from '../types';

type Props = {
  value: INumberFilterPill;
  onChange: (value: INumberFilterPill) => void;
};

function NumberFilter({ value, onChange }: Props) {
  const operatorOptions = useOptions('number');

  const fieldOptions = useMetricsFieldOptions();

  const handleFieldSelect = v => onChange({ ...value, field: v });

  const handleValueChange = (v: number | null) => onChange({ ...value, value: v });

  return (
    <Space>
      <Select
        value={value.field}
        style={{ width: 120 }}
        options={fieldOptions}
        onChange={handleFieldSelect}
      />

      <Select
        value={value.operator}
        style={{ width: 120 }}
        options={operatorOptions}
        onChange={v => onChange({ ...value, operator: v })}
      />

      <InputNumber<number>
        style={{ width: 120 }}
        value={value.value}
        onChange={handleValueChange}
      />
    </Space>
  );
}

export default memo(NumberFilter);
