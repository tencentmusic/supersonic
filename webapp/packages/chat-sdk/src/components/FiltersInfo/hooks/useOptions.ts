import { useState } from 'react';
import { getOptions } from '../utils';

export function useOptions(
  type: 'string' | 'number' | 'aggregation' | 'number-aggregation' | 'string-aggregation'
) {
  const [options] = useState(() => {
    if (type === 'number-aggregation') return getOptions('aggregation');
    if (type === 'string-aggregation')
      return getOptions('aggregation').filter(v => ['COUNT', 'COUNT_DISTINCT'].includes(v.value));
    return getOptions(type);
  });
  return options;
}
