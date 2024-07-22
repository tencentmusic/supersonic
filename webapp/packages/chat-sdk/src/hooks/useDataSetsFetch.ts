import { useRequest } from 'ahooks';
import { useEffect, useMemo } from 'react';
import { getDataSetInfo } from '../service';

export function useDataSetsFetch(ids: number[]) {
  const {
    data: responseData,
    loading,
    error,
    run,
  } = useRequest(() => Promise.all(ids.map(id => getDataSetInfo(id))), {
    cacheKey: 'datasetInfo' + ids.join(','),
    debounceWait: 500,
    staleTime: 1000 * 60 * 10,
  });

  const dataSets = useMemo(() => {
    if (responseData && responseData.length) {
      let map = new Map<number, any>();
      responseData.forEach((res: any, index: number) => {
        let data = {
          dimensions: [],
          metrics: [],
        };
        if (res?.code === 200) {
          data = res.data;
        }

        map.set(ids[index], data);
      });
      return map;
    } else {
      return new Map();
    }
  }, [responseData]);

  useEffect(() => {
    run();
  }, [ids]);

  return {
    loading,
    error,
    dataSets,
  };
}
