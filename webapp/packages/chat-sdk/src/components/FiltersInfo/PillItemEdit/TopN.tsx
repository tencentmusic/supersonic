import { InputNumber, Space } from 'antd';
import { ITopNPill } from '../types';

type Props = {
  value: ITopNPill;
  onChange: (value: ITopNPill) => void;
};

export default function Group({ value, onChange }: Props) {
  return (
    <Space direction="vertical">
      <InputNumber value={value.value} onChange={v => onChange({ ...value, value: v ?? 1 })} />
    </Space>
  );
}
