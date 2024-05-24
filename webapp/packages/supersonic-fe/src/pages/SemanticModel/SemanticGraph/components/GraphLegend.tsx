import { Space, Checkbox } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import React, { useState, useEffect } from 'react';
import styles from '../style.less';

type Props = {
  legendOptions: LegendOptionsItem[];
  value?: string[];
  onChange?: (ids: CheckboxValueType[]) => void;
  defaultCheckAll?: boolean;
  [key: string]: any;
};

type LegendOptionsItem = {
  id: string;
  label: string;
};

const GraphLegend: React.FC<Props> = ({
  legendOptions,
  value,
  defaultCheckAll = false,
  onChange,
}) => {
  const [groupValue, setGroupValue] = useState<CheckboxValueType[]>(value || []);

  useEffect(() => {
    if (!defaultCheckAll) {
      return;
    }
    if (!Array.isArray(legendOptions)) {
      setGroupValue([]);
      return;
    }
    setGroupValue(
      legendOptions.map((item) => {
        return item.id;
      }),
    );
  }, [legendOptions]);

  useEffect(() => {
    if (!Array.isArray(value)) {
      setGroupValue([]);
      return;
    }
    setGroupValue(value);
  }, [value]);

  const handleChange = (checkedValues: CheckboxValueType[]) => {
    setGroupValue(checkedValues);
    onChange?.(checkedValues);
  };

  return (
    <div className={styles.graphLegend}>
      <Checkbox.Group style={{ width: '100%' }} onChange={handleChange} value={groupValue}>
        <div style={{ width: '100%', maxWidth: '450px' }}>
          <div className={styles.title}>可见模型</div>
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <Space wrap size={[8, 16]}>
              {legendOptions.map((item) => {
                return (
                  <Checkbox key={item.id} value={item.id} style={{ transform: 'scale(0.85)' }}>
                    {item.label}
                  </Checkbox>
                );
              })}
            </Space>
          </div>
        </div>
      </Checkbox.Group>
    </div>
  );
};

export default GraphLegend;
