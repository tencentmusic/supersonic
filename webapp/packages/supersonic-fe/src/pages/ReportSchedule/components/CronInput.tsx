import React, { useState, useEffect } from 'react';
import { Input, Radio, Select, TimePicker, Space, Typography } from 'antd';
import dayjs from 'dayjs';

const { Text } = Typography;

interface CronInputProps {
  value?: string;
  onChange?: (value: string) => void;
}

const WEEKDAYS = [
  { label: '周一', value: '2' },
  { label: '周二', value: '3' },
  { label: '周三', value: '4' },
  { label: '周四', value: '5' },
  { label: '周五', value: '6' },
  { label: '周六', value: '7' },
  { label: '周日', value: '1' },
];

const parseCron = (cron?: string) => {
  if (!cron) return null;
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) return null;
  const [, m, h, day, , week] = parts;
  const hour = parseInt(h, 10);
  const minute = parseInt(m, 10);
  if (isNaN(hour) || isNaN(minute)) return null;
  const t = dayjs().hour(hour).minute(minute).second(0);
  if (week !== '?' && week !== '*') {
    return { frequency: 'weekly' as const, time: t, weekday: week, monthDay: 1 };
  }
  if (day !== '*' && day !== '?') {
    const d = parseInt(day, 10);
    return { frequency: 'monthly' as const, time: t, weekday: '2', monthDay: isNaN(d) ? 1 : d };
  }
  return { frequency: 'daily' as const, time: t, weekday: '2', monthDay: 1 };
};

const CronInput: React.FC<CronInputProps> = ({ value, onChange }) => {
  const parsed = parseCron(value);
  const [mode, setMode] = useState<'simple' | 'advanced'>('simple');
  const [frequency, setFrequency] = useState<'daily' | 'weekly' | 'monthly'>(parsed?.frequency ?? 'daily');
  const [time, setTime] = useState<dayjs.Dayjs>(parsed?.time ?? dayjs().hour(9).minute(0));
  const [weekday, setWeekday] = useState(parsed?.weekday ?? '2');
  const [monthDay, setMonthDay] = useState(parsed?.monthDay ?? 1);
  const [initialized, setInitialized] = useState(false);

  const generateCron = () => {
    const h = time.hour();
    const m = time.minute();
    switch (frequency) {
      case 'daily':
        return `0 ${m} ${h} * * ?`;
      case 'weekly':
        return `0 ${m} ${h} ? * ${weekday}`;
      case 'monthly':
        return `0 ${m} ${h} ${monthDay} * ?`;
      default:
        return `0 ${m} ${h} * * ?`;
    }
  };

  // Sync from external value changes (e.g. form.setFieldsValue)
  useEffect(() => {
    const p = parseCron(value);
    if (p) {
      setFrequency(p.frequency);
      setTime(p.time);
      setWeekday(p.weekday);
      setMonthDay(p.monthDay);
    }
    setInitialized(true);
  }, [value]);

  useEffect(() => {
    if (initialized && mode === 'simple' && onChange) {
      onChange(generateCron());
    }
  }, [frequency, time, weekday, monthDay, mode]);

  const getNextExecutions = (cron: string): string[] => {
    // Simple preview: generate next 5 execution times based on pattern
    const now = dayjs();
    const times: string[] = [];
    for (let i = 0; i < 5; i++) {
      times.push(now.add(i + 1, 'day').hour(time.hour()).minute(time.minute()).second(0).format('YYYY-MM-DD HH:mm:ss'));
    }
    return times;
  };

  const currentCron = mode === 'simple' ? generateCron() : (value || '');

  return (
    <div>
      <Radio.Group value={mode} onChange={(e) => setMode(e.target.value)} style={{ marginBottom: 12 }}>
        <Radio value="simple">简易模式</Radio>
        <Radio value="advanced">高级模式</Radio>
      </Radio.Group>

      {mode === 'simple' ? (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Space>
            <Select
              value={frequency}
              onChange={setFrequency}
              style={{ width: 120 }}
              options={[
                { label: '每天', value: 'daily' },
                { label: '每周', value: 'weekly' },
                { label: '每月', value: 'monthly' },
              ]}
            />
            {frequency === 'weekly' && (
              <Select
                value={weekday}
                onChange={setWeekday}
                style={{ width: 100 }}
                options={WEEKDAYS}
              />
            )}
            {frequency === 'monthly' && (
              <Select
                value={monthDay}
                onChange={setMonthDay}
                style={{ width: 100 }}
                options={Array.from({ length: 28 }, (_, i) => ({
                  label: `${i + 1}日`,
                  value: i + 1,
                }))}
              />
            )}
            <TimePicker
              value={time}
              onChange={(val) => val && setTime(val)}
              format="HH:mm"
            />
          </Space>
          <Text type="secondary">Cron 表达式: {currentCron}</Text>
        </Space>
      ) : (
        <Space direction="vertical" style={{ width: '100%' }}>
          <Input
            value={value}
            onChange={(e) => onChange?.(e.target.value)}
            placeholder="0 9 * * ? (秒 分 时 日 月 周)"
          />
        </Space>
      )}

      <div style={{ marginTop: 8 }}>
        <Text type="secondary">下次执行预览:</Text>
        {getNextExecutions(currentCron).map((t, i) => (
          <div key={i}>
            <Text type="secondary" style={{ fontSize: 12 }}>  {t}</Text>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CronInput;
