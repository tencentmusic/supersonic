import React, { useState } from 'react';
import { history, useParams, useModel } from '@umijs/max';
import DomainManagerTab from './components/DomainManagerTab';

type Props = {};

const DomainManager: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const domainModel = useModel('SemanticModel.domainData');

  const { selectDomainId } = domainModel;
  const menuKey = params.menuKey ? params.menuKey : defaultTabKey;

  const [activeKey, setActiveKey] = useState<string>(menuKey);

  const pushUrlMenu = (domainId: number, menuKey: string) => {
    history.push(`/model/domain/${domainId}/${menuKey}`);
  };

  return (
    <DomainManagerTab
      activeKey={activeKey}
      onMenuChange={(menuKey) => {
        setActiveKey(menuKey);
        pushUrlMenu(selectDomainId, menuKey);
      }}
    />
  );
};

export default DomainManager;
