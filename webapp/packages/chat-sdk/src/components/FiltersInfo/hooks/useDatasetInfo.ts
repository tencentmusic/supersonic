import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '..';
import { useDataSetsInfo } from '../../../hooks/useDataSetsInfo';

export function useDatasetInfo(dataSetIdFromParam?: number) {
  const datasetIdFromContext = useContextSelector(FilterInfosContext, context => context.datasetId);

  const dataSetsInfo = useDataSetsInfo();

  const dataSetId = dataSetIdFromParam || datasetIdFromContext;

  const { dimensions, metrics } = dataSetsInfo.get(dataSetId!) ?? {
    dimensions: [],
    metrics: [],
  };

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
