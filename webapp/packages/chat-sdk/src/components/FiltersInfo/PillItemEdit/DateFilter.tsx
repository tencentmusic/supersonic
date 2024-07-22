import { DatePicker, Space } from 'antd';
import dayjs from 'dayjs';
import { useMemo } from 'react';
import { IDateFilterPill } from '../types';

type Props = {
  value: IDateFilterPill;
  onChange: (value: IDateFilterPill) => void;
};

export default function DateFilter({ value, onChange }: Props) {
  const { startDate, endDate } = useMemo(() => {
    return {
      startDate: value.value?.[0],
      endDate: value.value?.[1],
    };
  }, [value]);

  const handleChange = (dateRange: any) => {
    onChange({
      ...value,
      value: [dayjs(dateRange[0]).format('YYYY-MM-DD'), dayjs(dateRange[1]).format('YYYY-MM-DD')],
    });
  };

  return (
    <Space>
      <DatePicker.RangePicker
        value={[dayjs(startDate), dayjs(endDate)]}
        onChange={handleChange}
        getPopupContainer={trigger => trigger.parentNode as HTMLElement}
        allowClear={false}
      />
    </Space>
  );
}
