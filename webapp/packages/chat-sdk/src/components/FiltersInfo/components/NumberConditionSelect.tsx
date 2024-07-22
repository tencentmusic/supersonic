import Select from './Select';
import { memo, useMemo } from 'react';
import { Checkbox, InputNumber, Radio, RadioChangeEvent, Space } from 'antd';
import {
  getNumberValue,
  getNumberFilterOperator,
  getOperatorConfig,
  getNumberFilterShowText,
  getNumberFilterValue,
} from '../utils';

type Props = {
  value: number | number[] | null;
  operator: string;
  width?: string;
  onChange: (values: { value: number | Array<number | null> | null; operator: string }) => void;
};

function NumberConditionSelect({ value, operator, width = '200px', onChange }: Props) {
  const showValue = getNumberFilterShowText(value, operator);

  const { typeMeaning, includeMin, includeMax } = useMemo(() => {
    const config = getOperatorConfig(operator);
    return (
      config ?? {
        typeMeaning: 'between',
        includeMin: false,
        includeMax: false,
      }
    );
  }, [operator]);

  const [number1, number2] = useMemo(() => getNumberValue(value, operator), [value, operator]);

  const handleTypeMeaningChange = (e: RadioChangeEvent) => {
    const newTypeMeaning = e.target.value;
    const operator = getNumberFilterOperator(newTypeMeaning, includeMin, includeMax);
    let newValue: any = value;
    if (!Array.isArray(value) && newTypeMeaning === 'between') {
      if (typeMeaning === 'least') newValue = [value, null];
      if (typeMeaning === 'most') newValue = [null, value];
    }
    if (Array.isArray(value) && newTypeMeaning !== 'between') {
      if (newTypeMeaning === 'least') newValue = value[0];
      if (newTypeMeaning === 'most') newValue = value[1];
    }
    onChange({ operator, value: newValue });
  };

  const handleIncludeMinChange = (e: any) => {
    onChange({
      operator: getNumberFilterOperator(typeMeaning, e.target.checked, includeMax),
      value,
    });
  };

  const handleIncludeMaxChange = (e: any) => {
    onChange({
      operator: getNumberFilterOperator(typeMeaning, includeMin, e.target.checked),
      value,
    });
  };

  const handleNumber1Change = (v: number | null) => {
    const _operator = operator || getNumberFilterOperator(typeMeaning, includeMin, includeMax);
    onChange({ operator: _operator, value: getNumberFilterValue([v, number2], _operator) });
  };

  const handleNumber2Change = (v: number | null) => {
    const _operator = operator || getNumberFilterOperator(typeMeaning, includeMin, includeMax);
    onChange({ operator: _operator, value: getNumberFilterValue([number1, v], _operator) });
  };

  return (
    <Select
      value={showValue}
      style={{ width }}
      options={[]}
      onChange={() => {}}
      dropdownStyle={{ width: 300 }}
      dropdownRender={() => (
        <div onClick={e => e.stopPropagation()} style={{ padding: 15 }}>
          <Radio.Group value={typeMeaning} onChange={handleTypeMeaningChange}>
            <Space>
              <Radio value="between">范围</Radio>
              <Radio value="least">至少</Radio>
              <Radio value="most">至多</Radio>
            </Space>
          </Radio.Group>
          <table>
            <tr>
              <td>
                <InputNumber
                  disabled={typeMeaning === 'most'}
                  value={number1}
                  onChange={handleNumber1Change}
                  style={{ width: '100%' }}
                  placeholder="输入最小值"
                />
              </td>
              <td>-</td>
              <td>
                <InputNumber
                  disabled={typeMeaning === 'least'}
                  value={number2}
                  onChange={handleNumber2Change}
                  style={{ width: '100%' }}
                  placeholder="输入最大值"
                />
              </td>
            </tr>
            <tr>
              <td>
                <Checkbox
                  disabled={typeMeaning === 'most'}
                  checked={includeMin}
                  onChange={handleIncludeMinChange}
                >
                  包含最小值
                </Checkbox>
              </td>
              <td></td>
              <td>
                <Checkbox
                  disabled={typeMeaning === 'least'}
                  checked={includeMax}
                  onChange={handleIncludeMaxChange}
                >
                  包含最大值
                </Checkbox>
              </td>
            </tr>
          </table>
        </div>
      )}
    />
  );
}

export default memo(NumberConditionSelect);
