import { Space, Tag } from 'antd';
import React, { useEffect, useState } from 'react';
import MetricTrendDimensionFilter from './MetricTrendDimensionFilter';
import type { FormData } from './MetricTrendDimensionFilter';

type Props = {
  dimensionOptions: OptionsItem[];
  modelId: number;
  value?: FormData;
  periodDate?: { startDate: string; endDate: string; dateField: string };
  onChange?: (value: FormData[]) => void;
};

type DimensionOptionsMapItem = {
  dimensionBizName: string;
  dimensionName: string;
};

const MetricTrendDimensionFilterContainer: React.FC<Props> = ({
  dimensionOptions,
  modelId,
  periodDate,
  value,
  onChange,
}) => {
  const [filterData, setFilterData] = useState<FormData[]>([]);
  const [dimensionOptionsMap, setDimensionOptionsMap] = useState<
    Record<string, DimensionOptionsMapItem>
  >({});

  useEffect(() => {
    if (Array.isArray(dimensionOptions)) {
      const dimensionDataMap = dimensionOptions.reduce(
        (dimensionMap: Record<string, DimensionOptionsMapItem>, item: OptionsItem) => {
          dimensionMap[item.value] = {
            dimensionBizName: `${item.value}`,
            dimensionName: item.label,
          };
          return dimensionMap;
        },
        {},
      );
      setDimensionOptionsMap(dimensionDataMap);
    }
  }, [dimensionOptions]);

  return (
    <div>
      <MetricTrendDimensionFilter
        modelId={modelId}
        dimensionOptions={dimensionOptions}
        // value={value}
        periodDate={periodDate}
        onChange={(value) => {
          const data = [...filterData, value];
          setFilterData(data);
          onChange?.(data);
        }}
      />
      <Space size={8} wrap style={{ marginTop: 10 }}>
        {filterData.map((item: FormData, index: number) => {
          const { dimensionBizName, dimensionValue, operator } = item;
          return (
            // eslint-disable-next-line react/no-array-index-key
            <Space key={`${dimensionBizName}-${dimensionValue}-${operator}-${Math.random()}`}>
              <Tag
                color="blue"
                style={{ padding: 5 }}
                closable
                onClose={() => {
                  const newData = [...filterData];
                  newData.splice(index, 1);
                  setFilterData(newData);
                  onChange?.(newData);
                }}
              >
                <span style={{ marginRight: 5 }}>
                  {dimensionOptionsMap?.[dimensionBizName]?.dimensionName}[{operator}]:
                </span>
                <Tag color="purple">
                  {Array.isArray(dimensionValue) ? dimensionValue.join('、') : dimensionValue}
                </Tag>
              </Tag>
              {index !== filterData.length - 1 && (
                <Tag color="blue" style={{ marginRight: 10 }}>
                  <span style={{ height: 32, display: 'flex', alignItems: 'center' }}>AND</span>
                </Tag>
              )}
            </Space>
          );
        })}
      </Space>
    </div>
  );
};

export default MetricTrendDimensionFilterContainer;
