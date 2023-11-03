import { Form, Input, Select, Space, Row, Col, Switch, Tag } from 'antd';
import StandardFormRow from '@/components/StandardFormRow';
import TagSelect from '@/components/TagSelect';
import React, { useEffect, useState, useRef } from 'react';
import { SENSITIVE_LEVEL_OPTIONS } from '../../constant';
import { SearchOutlined } from '@ant-design/icons';
import RemoteSelect, { RemoteSelectImperativeHandle } from '@/components/RemoteSelect';
import { queryDimValue } from '@/pages/SemanticModel/service';
import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  dimensionOptions: { value: string; label: string }[];
  modelId: number;
  initFilterValues?: any;
  onFiltersChange: (_: any, values: any) => void;
};

const MetricTrendDimensionFilter: React.FC<Props> = ({
  dimensionOptions,
  modelId,
  initFilterValues = {},
  onFiltersChange,
}) => {
  // const [form] = Form.useForm();
  const dimensionValueSearchRef = useRef<RemoteSelectImperativeHandle>();
  const queryParams = useRef<{ dimensionBizName?: string }>({});
  const [dimensionValue, setDimensionValue] = useState<string>('');
  // const [queryParams, setQueryParams] = useState<any>({});
  const loadSiteName = async (searchValue: string) => {
    if (!queryParams.current?.dimensionBizName) {
      // return [];
      return;
    }
    const { dimensionBizName } = queryParams.current;
    const { code, data } = await queryDimValue({
      ...queryParams.current,
      value: searchValue,
      modelId,
      limit: 50,
    });
    if (code === 200 && Array.isArray(data?.resultList)) {
      return data.resultList.slice(0, 50).map((item: any) => ({
        value: item[dimensionBizName],
        label: item[dimensionBizName],
      }));
    }
    return [];
  };

  return (
    <Space>
      <Select
        style={{ minWidth: 150 }}
        options={dimensionOptions}
        showSearch
        filterOption={(input, option) =>
          ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
        }
        allowClear
        placeholder="请选择筛选维度"
        onChange={(value) => {
          queryParams.current = { dimensionBizName: value };
          if (value) {
            dimensionValueSearchRef.current?.emitSearch('');
          }
        }}
      />
      <Tag color="processing" style={{ margin: 0 }}>
        =
      </Tag>
      <RemoteSelect
        value={dimensionValue}
        onChange={(value) => {
          setDimensionValue(value);
        }}
        ref={dimensionValueSearchRef}
        style={{ minWidth: 150 }}
        mode={undefined}
        placeholder="维度值搜索"
        fetchOptions={loadSiteName}
      />
    </Space>
  );
};

export default MetricTrendDimensionFilter;
