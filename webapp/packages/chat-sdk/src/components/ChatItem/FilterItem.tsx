import { Select, Spin } from 'antd';
import { PREFIX_CLS } from '../../common/constants';
import { FilterItemType } from '../../common/type';
import { useEffect, useMemo, useRef, useState } from 'react';
import { queryDimensionValues } from '../../service';
import debounce from 'lodash/debounce';
import isArray from 'lodash/isArray';

type Props = {
  modelId: number;
  filters: FilterItemType[];
  filter: FilterItemType;
  onFiltersChange: (filters: FilterItemType[]) => void;
};

const FilterItem: React.FC<Props> = ({ modelId, filters, filter, onFiltersChange }) => {
  const [options, setOptions] = useState<{ label: string; value: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const fetchRef = useRef(0);

  const prefixCls = `${PREFIX_CLS}-filter-item`;

  const initData = async () => {
    const { data } = await queryDimensionValues(modelId, filter.bizName, '');
    setOptions(
      data?.data?.resultList.map((item: any) => ({
        label: item[filter.bizName],
        value: item[filter.bizName],
      })) || []
    );
  };

  useEffect(() => {
    if ((typeof filter.value === 'string' || isArray(filter.value)) && options.length === 0) {
      initData();
    }
  }, []);

  const debounceFetcher = useMemo(() => {
    const loadOptions = (value: string) => {
      fetchRef.current += 1;
      const fetchId = fetchRef.current;
      setOptions([]);
      setLoading(true);

      queryDimensionValues(modelId, filter.bizName, value).then(newOptions => {
        if (fetchId !== fetchRef.current) {
          return;
        }

        setOptions(
          newOptions.data?.data?.resultList.map((item: any) => ({
            label: item[filter.bizName],
            value: item[filter.bizName],
          })) || []
        );
        setLoading(false);
      });
    };

    return debounce(loadOptions, 800);
  }, [queryDimensionValues]);

  const onChange = (value: string | string[]) => {
    if (isArray(value) && value.length === 0) {
      return;
    }
    const newFilters = filters.map(item => {
      if (item.bizName === filter.bizName) {
        item.value = isArray(value) ? value : `${value}`;
      }
      return item;
    });
    onFiltersChange(newFilters);
  };

  return (
    <span className={prefixCls}>
      {typeof filter.value === 'string' || isArray(filter.value) ? (
        <Select
          bordered={false}
          value={filter.value}
          options={options}
          className={`${prefixCls}-select-control`}
          onSearch={debounceFetcher}
          notFoundContent={loading ? <Spin size="small" /> : null}
          onChange={onChange}
          mode={isArray(filter.value) ? 'multiple' : undefined}
          showSearch
        />
      ) : (
        <span className={`${prefixCls}-filter-value`}>{filter.value}</span>
      )}
    </span>
  );
};

export default FilterItem;
