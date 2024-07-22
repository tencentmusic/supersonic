import { uniqBy } from 'lodash';
import { useMemo } from 'react';
import { useDatasetInfo } from './useDatasetInfo';

export function useDemensionFieldOptions() {
  const { dimensions } = useDatasetInfo();

  const options = useMemo(
    () =>
      uniqBy(
        dimensions.map(d => ({
          value: d.bizName,
          label: d.name,
          type: 'string',
        })),
        'value'
      ),
    [dimensions]
  );

  return options;
}
