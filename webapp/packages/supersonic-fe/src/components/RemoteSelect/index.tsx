import React, {
  useState,
  useRef,
  useMemo,
  useEffect,
  forwardRef,
  useImperativeHandle,
  Ref,
} from 'react';
import { Select, Spin, Empty } from 'antd';
import debounce from 'lodash/debounce';
// import type { ValueTextType } from '@/constants';
import isFunction from 'lodash/isFunction';

type Props = {
  fetchOptions: (...restParams: any[]) => Promise<{ label: any; value: any }[]>;
  debounceTimeout?: number;
  formatPropsValue?: (value: any) => any;
  formatFetchOptionsParams?: (inputValue: string, ctx?: any) => any[];
  formatOptions?: (data: any, ctx: any) => any[];
  autoInit?: boolean;
  disabledSearch?: boolean;
  [key: string]: any;
};
type SelectOptions = {
  label: string;
} & {
  text: string;
} & {
  value: any;
};

export type RemoteSelectImperativeHandle = {
  emitSearch: (value: string) => void;
};

const { Option } = Select;

const DebounceSelect = forwardRef(
  (
    {
      autoInit = false,
      fetchOptions,
      debounceTimeout = 500,
      formatPropsValue,
      formatFetchOptionsParams,
      formatOptions,
      disabledSearch = false,
      ...restProps
    }: Props,
    ref: Ref<any>,
  ) => {
    const props = { ...restProps };
    const { ctx, filterOption } = props;
    if (isFunction(formatPropsValue)) {
      props.value = formatPropsValue(props.value);
    }
    const [fetching, setFetching] = useState(false);
    const [options, setOptions] = useState(props.options || props.source || []);

    useImperativeHandle(ref, () => ({
      emitSearch: (value: string) => {
        loadOptions(value, true);
      },
    }));

    useEffect(() => {
      if (autoInit) {
        loadOptions('', true);
      }
    }, []);
    useEffect(() => {
      setOptions(props.source || []);
    }, [props.source]);

    const fetchRef = useRef(0);

    const loadOptions = (value: string, allowEmptyValue?: boolean) => {
      setOptions([]);
      if (disabledSearch) {
        return;
      }
      if (!allowEmptyValue && !value) return;
      fetchRef.current += 1;
      const fetchId = fetchRef.current;
      setFetching(true);
      const fetchParams = formatFetchOptionsParams ? formatFetchOptionsParams(value, ctx) : [value];
      // eslint-disable-next-line prefer-spread
      fetchOptions.apply(null, fetchParams).then((newOptions) => {
        if (fetchId !== fetchRef.current || !Array.isArray(newOptions)) {
          return;
        }
        let finalOptions = newOptions;
        if (formatOptions && isFunction(formatOptions)) {
          finalOptions = formatOptions(newOptions, ctx);
        }
        finalOptions =
          filterOption && Array.isArray(finalOptions)
            ? filterOption?.(finalOptions, ctx)
            : finalOptions;
        setOptions(finalOptions);
        setFetching(false);
      });
    };

    const debounceFetcher = useMemo(() => {
      return debounce(loadOptions, debounceTimeout, {
        trailing: true,
      });
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [fetchOptions, debounceTimeout]);

    return (
      <Select
        style={{ minWidth: '100px' }}
        showSearch
        allowClear
        mode="multiple"
        // onClear={() => {
        //   if (autoInit) {
        //     loadOptions('', true);
        //   } else {
        //     setOptions([]);
        //   }
        // }}
        onSearch={debounceFetcher}
        {...props}
        filterOption={false} // 保持对props中filterOption属性的复写，不可变更位置
        notFoundContent={
          fetching ? <Spin size="small" /> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        loading={fetching}
      >
        {options.map((option: SelectOptions) => (
          <Option value={option.value} key={option.value}>
            {option.text || option.label}
          </Option>
        ))}
      </Select>
    );
  },
);
export default DebounceSelect;
