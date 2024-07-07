import React, { memo } from 'react';
import ConditionList, { IConditionListHandle } from './ConditionList';
import ConditionFooter from './ConditionFooter';
import { ConditionContextProvider } from './Context';

type Props = {
  listRef: React.RefObject<IConditionListHandle>;
};

const index = ({ listRef }: Props) => {
  return (
    <div className="condition-panel-wrap">
      <ConditionContextProvider>
        <ConditionList ref={listRef} />
        <ConditionFooter />
      </ConditionContextProvider>
    </div>
  );
};

export default memo(index);
