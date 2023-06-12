import type { ColumnsState } from '@ant-design/pro-table';
import { useCallback, useEffect, useState } from 'react';
import type { DependencyList } from 'react';

export interface CommonSortParams {
  orderCondition: string;
  sort: 'asc' | 'desc';
}

/**
 * 转换 ProTable request 中传入的 sort 参数到接口统一参数格式
 */
export function transformSorter(
  sort: Record<string, 'ascend' | 'descend'> | undefined,
): Partial<CommonSortParams> {
  const sorter = { ...sort };
  const keys = Object.keys(sorter);

  if (keys.length === 0) {
    return {};
  }

  const key = keys.shift() as string;
  return {
    orderCondition: key,
    sort: sorter[key] === 'ascend' ? 'asc' : 'desc',
  };
}

type ColumnsStateMap = Record<string, ColumnsState>;

/**
 * 创建记录表格列表字段展示的 hook
 */
export function useRememberTableColumnsStateMap(key: string, defaultValue?: ColumnsStateMap) {
  const realKey = `inner:rememberTableColumnsStateMap:${key}`;
  const rememberValue = localStorage.getItem(realKey);

  let initValue: ColumnsStateMap = {};
  if (rememberValue) {
    try {
      const parsed = JSON.parse(rememberValue);
      if (typeof parsed === 'object' && parsed) {
        initValue = parsed;
      }
    } catch (e) {
      localStorage.removeItem(realKey);
    }
  } else if (defaultValue) {
    initValue = { ...defaultValue };
  }

  const [state, setState] = useState<ColumnsStateMap>(initValue);

  const handleChange = useCallback(
    (val: ColumnsStateMap) => {
      const remember = JSON.stringify(val);
      localStorage.setItem(realKey, remember);
      setState(val);
    },
    [setState],
  );

  return {
    columnsStateMap: state,
    handleColumnsStateChange: handleChange,
  };
}

interface UseFetchDataEffectParams<R = any> {
  fetcher: () => Promise<R>;
  updater: (res: R) => void;
  cleanup?: () => void;
}

/**
 * 组件内请求接口的副作用 hook
 */
export function useFetchDataEffect<R = any>(
  { fetcher, updater, cleanup }: UseFetchDataEffectParams<R>,
  deps?: DependencyList,
) {
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    let unmounted: boolean = false;

    setLoading(true);
    fetcher()
      .then((res) => {
        if (unmounted) {
          return;
        }
        updater(res);
      })
      .catch(() => null)
      .finally(() => {
        setLoading(false);
      });

    return () => {
      cleanup?.();
      unmounted = true;
    };
  }, deps);

  return { loading, setLoading };
}
