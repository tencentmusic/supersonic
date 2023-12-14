import React from 'react';
import { connect } from 'umi';
import type { StateType } from './model';
import OverviewContainer from './OverviewContainer';
import type { Dispatch } from 'umi';

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};
const DomainManager: React.FC<Props> = () => {
  return (
    <>
      <OverviewContainer mode={'domain'} />
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManager);
