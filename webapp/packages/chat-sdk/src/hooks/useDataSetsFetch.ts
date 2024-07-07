import { useRequest } from 'ahooks';
import { useEffect, useState } from 'react';
import { getDataSetInfo } from '../service';

export function useDataSetsFetch(ids: number[]) {
  const [dataSets, setDataSets] = useState<Map<number, any>>(() => new Map<number, any>());

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

  useEffect(() => {
    if (responseData && responseData.length) {
      let map = new Map<number, any>();
      responseData.forEach((res: any, index: number) => {
        let dimensions: any[] = [];
        let metrics: any[] = [];
        if (res?.code === 200) {
          dimensions = res.data.dimensions;
          metrics = res.data.metrics;
        }

        map.set(ids[index], { dimensions, metrics });
      });

      setDataSets(map);
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
