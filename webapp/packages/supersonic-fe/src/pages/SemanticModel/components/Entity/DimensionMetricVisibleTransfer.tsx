import React, { useEffect, useState } from 'react';
import { IChatConfig } from '../../data';
import DimensionMetricVisibleTableTransfer from './DimensionMetricVisibleTableTransfer';

interface RecordType {
  key: React.Key;
  name: string;
  type: 'dimension' | 'metric';
}

type Props = {
  sourceList: any[];
  targetList: React.Key[];
  titles?: (React.ReactNode | string)[];
  onChange?: (params?: any) => void;
  [key: string]: any;
};

const DimensionMetricVisibleTransfer: React.FC<Props> = ({
  sourceList = [],
  targetList = [],
  titles,
  onChange,
  ...rest
}) => {
  const [transferData, setTransferData] = useState<RecordType[]>([]);
  const [targetKeys, setTargetKeys] = useState<React.Key[]>(targetList);

  useEffect(() => {
    setTransferData(
      sourceList.map(({ key, id, name, bizName, transType, modelName, isTag }) => {
        return {
          key,
          name,
          bizName,
          id,
          modelName,
          isTag,
          type: transType,
        };
      }),
    );
    const keyList: React.Key[] = sourceList.map((item) => item.key);
    const filterTargetList = targetList.filter((key: React.Key) => {
      return keyList.includes(key);
    });
    setTargetKeys(filterTargetList);
  }, [sourceList, targetList]);

  const handleChange = (newTargetKeys: string[]) => {
    setTargetKeys(newTargetKeys);
    onChange?.(newTargetKeys);
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center' }}>
      <DimensionMetricVisibleTableTransfer
        dataSource={transferData}
        showSearch
        titles={titles || ['不可见维度', '可见维度']}
        listStyle={{
          width: 720,
          height: 600,
        }}
        filterOption={(inputValue: string, item: any) => {
          const { name } = item;
          if (name.includes(inputValue)) {
            return true;
          }
          return false;
        }}
        targetKeys={targetKeys}
        onChange={handleChange}
        {...rest}
      />
    </div>
  );
};

export default DimensionMetricVisibleTransfer;
