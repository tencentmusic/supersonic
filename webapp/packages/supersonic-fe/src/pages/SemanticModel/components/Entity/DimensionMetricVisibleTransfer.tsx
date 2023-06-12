import { Transfer, Tag } from 'antd';
import React, { useEffect, useState } from 'react';

interface RecordType {
  key: string;
  name: string;
  type: 'dimension' | 'metric';
}

type Props = {
  sourceList: any[];
  targetList: string[];
  titles?: string[];
  onChange?: (params?: any) => void;
  transferProps?: Record<string, any>;
};

const DimensionMetricVisibleTransfer: React.FC<Props> = ({
  sourceList = [],
  targetList = [],
  titles,
  transferProps = {},
  onChange,
}) => {
  const [transferData, setTransferData] = useState<RecordType[]>([]);
  const [targetKeys, setTargetKeys] = useState<string[]>(targetList);

  useEffect(() => {
    setTransferData(
      sourceList.map(({ id, name, type }: any) => {
        return {
          key: id,
          name,
          type,
        };
      }),
    );
  }, [sourceList]);

  useEffect(() => {
    setTargetKeys(targetList);
  }, [targetList]);

  const handleChange = (newTargetKeys: string[]) => {
    setTargetKeys(newTargetKeys);
    onChange?.(newTargetKeys);
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center' }}>
      <Transfer
        dataSource={transferData}
        showSearch
        titles={titles || ['不可见维度', '可见维度']}
        listStyle={{
          width: 430,
          height: 500,
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
        render={(item) => (
          <div style={{ display: 'flex' }}>
            <span style={{ flex: '1' }}>{item.name}</span>
            <span style={{ flex: '0 1 40px' }}>
              {item.type === 'dimension' ? (
                <Tag color="blue">{'维度'}</Tag>
              ) : item.type === 'metric' ? (
                <Tag color="orange">{'指标'}</Tag>
              ) : (
                <></>
              )}
            </span>
          </div>
        )}
        {...transferProps}
      />
    </div>
  );
};

export default DimensionMetricVisibleTransfer;
