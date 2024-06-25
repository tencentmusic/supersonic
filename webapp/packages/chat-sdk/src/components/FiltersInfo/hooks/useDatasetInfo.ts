import { useRequest } from 'ahooks';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '..';
import { getDataSetInfo } from '../../../service';

export function useDatasetInfo() {
  const datasetId = useContextSelector(FilterInfosContext, context => context.datasetId);

  const { data: responseData } = useRequest(() => getDataSetInfo(datasetId!), {
    cacheKey: 'datasetInfo' + datasetId,
    debounceWait: 500,
    staleTime: 1000 * 60 * 10,
  });

  let dimensions: any[] = [];
  let metrics: any[] = [];

  // @ts-ignore
  if (responseData?.code === 200) {
    dimensions = responseData.data.dimensions;
    metrics = responseData.data.metrics;
  }

  const getTypeByBizName = (bizName: string) => {
    const dimension = dimensions.find((item: any) => item.bizName === bizName);
    if (dimension) return 'string';
    const metric = metrics.find((item: any) => item.bizName === bizName);
    if (metric) return 'number';
    return 'date';
  };

  const getFieldInfo = (bizName: string) => {
    const dimension = dimensions.find((item: any) => item.bizName === bizName);
    if (dimension) return dimension;
    const metric = metrics.find((item: any) => item.bizName === bizName);
    if (metric) return metric;
    return {};
  };

  return {
    dimensions,
    metrics,
    getTypeByBizName,
    getFieldInfo,
  };
}
