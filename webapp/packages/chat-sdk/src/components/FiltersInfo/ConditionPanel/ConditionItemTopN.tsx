import { Flex, Input, InputNumber } from 'antd';
import type { ITopNCondition } from '../types';

type Props = {
  data: ITopNCondition;
  onChange: (data: ITopNCondition) => void;
};

export default function ConditionItemTopN({ data, onChange }: Props) {
  return (
    <Flex className="condition-item-base" gap={5}>
      <div className="condition-item-base-first">
        <Input size="middle" value="TopN" style={{ width: '150px' }} readOnly />
      </div>
      <div className="condition-item-base-second">
        <InputNumber
          value={data.value}
          onChange={value => onChange({ ...data, value })}
          size="middle"
          style={{ width: '120px' }}
        />
      </div>
      <div className="condition-item-base-third"></div>
    </Flex>
  );
}
