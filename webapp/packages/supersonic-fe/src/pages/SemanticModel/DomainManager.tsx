import React from 'react';
import OverviewContainer from './OverviewContainer';

type Props = {};
const DomainManager: React.FC<Props> = () => {
  return (
    <>
      <OverviewContainer mode={'domain'} />
    </>
  );
};

export default DomainManager;
