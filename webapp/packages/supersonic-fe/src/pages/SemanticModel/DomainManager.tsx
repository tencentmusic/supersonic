import React, { useState } from 'react';
import { useParams, useModel } from '@umijs/max';
import DomainManagerTab from './components/DomainManagerTab';
import { toDomainList } from '@/pages/SemanticModel/utils';

type Props = {};

const DomainManager: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const domainModel = useModel('SemanticModel.domainData');

  const { selectDomainId } = domainModel;
  const menuKey = params.menuKey ? params.menuKey : defaultTabKey;

  const [activeKey, setActiveKey] = useState<string>(menuKey);

  return (
    <DomainManagerTab
      activeKey={activeKey}
      onMenuChange={(menuKey) => {
        setActiveKey(menuKey);
        toDomainList(selectDomainId, menuKey);
      }}
    />
  );
};

export default DomainManager;
