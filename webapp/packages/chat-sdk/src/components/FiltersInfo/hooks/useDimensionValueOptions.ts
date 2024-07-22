import { useEffect, useState } from 'react';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '..';
import { queryDimensionValues } from '../../../service';
import { useDatasetInfo } from './useDatasetInfo';

export function useDimensionValueOptions(bizName: string | undefined) {
  const [options, setOptions] = useState<{ value: string; label: string }[]>([]);

  const agentId = useContextSelector(FilterInfosContext, context => context.agentId);

  const { dimensions, metrics } = useDatasetInfo();

  const elementId = [...dimensions, ...metrics].find(item => item.bizName === bizName)?.id;

  useEffect(() => {
    if (!bizName || !agentId || !elementId) return;

    const fetchOptions = async () => {
      const { data } = await queryDimensionValues(undefined, bizName, agentId!, elementId, '');
      setOptions(
        data?.resultList?.map((item: any) => ({
          label: item[bizName],
          value: item[bizName],
        })) || []
      );
    };

    fetchOptions();
  }, [bizName, agentId, elementId]);

  return options;
}
