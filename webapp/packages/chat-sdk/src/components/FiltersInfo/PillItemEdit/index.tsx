import { memo, useCallback } from 'react';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '../';
import { IPill } from '../types';
import Aggregation from './Aggregation';
import DateFilter from './DateFilter';
import Group from './Group';
import NumberFilter from './NumberFilter';
import TextFilter from './TextFilter';
import TopN from './TopN';

function getPillItemEditComponent(type: IPill['type']) {
  const ComponentMap = {
    'number-filter': NumberFilter,
    'text-filter': TextFilter,
    'date-filter': DateFilter,
    group: Group,
    aggregation: Aggregation,
    'top-n': TopN,
  };

  return ComponentMap[type];
}

type Props = {
  idx: number;
};
function PillItemEdit({ idx }: Props) {
  const data = useContextSelector(FilterInfosContext, context => context.pillData[idx]);
  const updatePillItem = useContextSelector(FilterInfosContext, context => context.updatePillItem);

  const Component = getPillItemEditComponent(data.type);

  const handleChange = useCallback(
    (data: IPill) => {
      updatePillItem(idx, data);
    },
    [idx, updatePillItem]
  );

  return (
    <Component
      // @ts-ignore
      value={data}
      onChange={handleChange}
    />
  );
}

export default memo(PillItemEdit);
