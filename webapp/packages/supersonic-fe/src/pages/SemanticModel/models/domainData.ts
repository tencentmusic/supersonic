import { ISemantic } from '../data';
import { useState } from 'react';

export default function Domain() {
  const [selectDomain, setSelectDomain] = useState<ISemantic.IDomainItem>();
  const [domainList, setDomainList] = useState<ISemantic.IDomainItem[]>([]);

  return {
    selectDomain,
    selectDomainId: selectDomain?.id,
    selectDomainName: selectDomain?.name,
    domainList,
    setSelectDomain,
    setDomainList,
  };
}
