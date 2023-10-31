import { Select, Spin, InputNumber } from 'antd';
import { PREFIX_CLS } from '../../common/constants';
import { ChatContextType, FilterItemType } from '../../common/type';
import { useEffect, useMemo, useRef, useState } from 'react';
import { queryDimensionValues } from '../../service';
import { debounce, isArray } from 'lodash';
import SwicthEntity from './SwitchEntity';

type Props = {
  modelId: number;
  filters: FilterItemType[];
  filter: FilterItemType;
  chatContext: ChatContextType;
  agentId?: number;
  entityAlias?: string;
  integrateSystem?: string;
  onFiltersChange: (filters: FilterItemType[]) => void;
  onSwitchEntity: (entityId: string) => void;
};

const FilterItem: React.FC<Props> = ({
  modelId,
  filters,
  filter,
  chatContext,
  agentId,
  entityAlias,
  integrateSystem,
  onFiltersChange,
  onSwitchEntity,
}) => {
  const [options, setOptions] = useState<{ label: string; value: string | null }[]>([]);
  const [loading, setLoading] = useState(false);
  const fetchRef = useRef(0);

  const prefixCls = `${PREFIX_CLS}-filter-item`;

  const initData = async () => {
    const { data } = await queryDimensionValues(
      modelId,
      filter.bizName,
      agentId!,
      filter.elementID,
      ''
    );
    setOptions(
      data?.resultList.map((item: any) => ({
        label: item[filter.bizName],
        value: item[filter.bizName],
      })) || []
    );
  };

  useEffect(() => {
    if (
      (typeof filter.value === 'string' || isArray(filter.value)) &&
      options.length === 0 &&
      integrateSystem !== 'showcase'
    ) {
      initData();
    }
  }, []);

  const debounceFetcher = useMemo(() => {
    const loadOptions = (value: string) => {
      fetchRef.current += 1;
      const fetchId = fetchRef.current;
      setOptions([]);
      setLoading(true);
      queryDimensionValues(modelId, filter.bizName, agentId!, filter.elementID, value).then(
        newOptions => {
          if (fetchId !== fetchRef.current) {
            return;
          }
          setOptions(
            newOptions.data?.resultList.map((item: any) => ({
              label: item[filter.bizName],
              value: item[filter.bizName],
            })) || []
          );
          setLoading(false);
        }
      );
    };

    return debounce(loadOptions, 500);
  }, [queryDimensionValues]);

  const onOperatorChange = (value: string) => {
    const newFilters = filters.map(item => {
      if (item.bizName === filter.bizName) {
        item.operator = value;
      }
      return item;
    });
    onFiltersChange(newFilters);
  };

  const onChange = (value: string | string[] | number | null) => {
    const newFilters = filters.map(item => {
      if (item.bizName === filter.bizName) {
        item.value =
          typeof filter.value === 'number' || filter.value === null
            ? value
            : isArray(value)
            ? value
            : `${value}`;
        if (isArray(value)) {
          if (value.length === 1) {
            item.operator = '=';
            item.value = value[0];
          } else {
            item.operator = 'IN';
            item.value = value;
          }
        } else {
          item.value =
            typeof filter.value === 'number' || filter.value === null ? value : `${value}`;
        }
      }
      return item;
    });
    onFiltersChange(newFilters);
  };

  return (
    <span className={prefixCls}>
      <span className={`${prefixCls}-filter-name`}>{filter.name}：</span>
      {(typeof filter.value === 'number' || filter.value === null) &&
      !filter.bizName?.includes('_id') ? (
        <>
          <Select
            options={[
              { label: '大于', value: '>' },
              { label: '等于', value: '=' },
              { label: '小于', value: '<' },
            ]}
            className={`${prefixCls}-operator-control`}
            value={filter.operator}
            onChange={onOperatorChange}
          />
          <InputNumber
            className={`${prefixCls}-input-number-control`}
            value={filter.value}
            onChange={onChange}
          />
        </>
      ) : (typeof filter.value === 'string' || isArray(filter.value)) &&
        !filter.bizName?.includes('_id') ? (
        <Select
          value={filter.value}
          options={options.filter(option => option.value !== '' && option.value !== null)}
          className={`${prefixCls}-select-control`}
          onSearch={debounceFetcher}
          notFoundContent={loading ? <Spin size="small" /> : null}
          onChange={onChange}
          mode="multiple"
          showSearch
          allowClear
        />
      ) : entityAlias &&
        ['歌曲', '艺人'].includes(entityAlias) &&
        filter.bizName?.includes('_id') ? (
        <>
          <SwicthEntity
            entityName={filter.value}
            chatContext={chatContext}
            onSwitchEntity={onSwitchEntity}
          />
          <span className={`${prefixCls}-switch-entity-tip`}>
            (如未匹配到相关{entityAlias}，可点击{entityAlias === '艺人' ? '歌手' : entityAlias}
            ID切换)
          </span>
        </>
      ) : (
        <span className={`${prefixCls}-filter-value`}>{filter.value}</span>
      )}
    </span>
  );
};

export default FilterItem;
