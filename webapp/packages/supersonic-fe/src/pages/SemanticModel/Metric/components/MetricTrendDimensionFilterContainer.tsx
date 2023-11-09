import { Form, Select, Space, Tag, Button, Tooltip } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import React, { useEffect, useState, useRef } from 'react';
import RemoteSelect, { RemoteSelectImperativeHandle } from '@/components/RemoteSelect';
import { queryDimValue } from '@/pages/SemanticModel/service';
import { OperatorEnum } from '@/pages/SemanticModel/enum';
import MetricTrendDimensionFilter from './MetricTrendDimensionFilter';
import type { FormData } from './MetricTrendDimensionFilter';
import { isString } from 'lodash';
import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  dimensionOptions: { value: string; label: string }[];
  modelId: number;
  value?: FormData;
  onChange?: (value: FormData) => void;
};

const MetricTrendDimensionFilterContainer: React.FC<Props> = ({
  dimensionOptions,
  modelId,
  value,
  onChange,
}) => {
  const [filterData, setFilterData] = useState<FormData[]>([]);
  return (
    <div>
      <MetricTrendDimensionFilter
        modelId={modelId}
        dimensionOptions={dimensionOptions}
        value={value}
        onChange={(value) => {
          setFilterData([...filterData, value]);
          onChange?.(value);
        }}
      />
      <div>
        <Tag>
          {filterData.map((item: FormData) => {
            return <Tag key={item.dimensionValue}>{item.dimensionValue}</Tag>;
          })}
        </Tag>
      </div>
    </div>
  );
};

export default MetricTrendDimensionFilterContainer;
