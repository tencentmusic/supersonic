import { memo } from 'react';
import { useContextSelector } from 'use-context-selector';
import { FilterInfosContext } from '.';
import PillItem from './PillItem';

function PillList() {
  const data = useContextSelector(FilterInfosContext, context => context.pillData);

  return (
    <div className="pill-list-wrap">
      {data.map((_, idx) => (
        <PillItem key={idx} idx={idx} />
      ))}
    </div>
  );
}

export default memo(PillList);
