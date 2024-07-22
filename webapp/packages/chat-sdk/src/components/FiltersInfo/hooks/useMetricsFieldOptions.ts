import { uniqBy } from 'lodash';
import { useMemo } from 'react';
import { useDatasetInfo } from './useDatasetInfo';

export function useMetricsFieldOptions() {
  const { metrics } = useDatasetInfo();

  const options = useMemo(
    () =>
      uniqBy(
        metrics.map(d => ({
          value: d.bizName,
          label: d.name,
          type: 'number',
        })),
        'value'
      ),
    [metrics]
  );

  return options;
}
