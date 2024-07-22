import { uniqBy } from 'lodash';
import { useMemo } from 'react';
import { useDatasetInfo } from './useDatasetInfo';

export function useFieldOptions() {
  const { metrics, dimensions } = useDatasetInfo();

  const options = useMemo(
    () => [
      ...uniqBy(
        dimensions.map(d => ({
          value: d.bizName,
          label: d.name,
          type: 'string',
        })),
        'value'
      ),
      ...uniqBy(
        metrics.map(d => ({
          value: d.bizName,
          label: d.name,
          type: 'number',
        })),
        'value'
      ),
    ],
    [metrics, dimensions]
  );

  return options;
}
