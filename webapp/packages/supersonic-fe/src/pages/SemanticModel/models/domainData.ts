import { ISemantic } from '../data';
import { useState, useEffect } from 'react';
import { useModel } from '@umijs/max';

export default function Domain() {
  const [selectDomain, setSelectDomain] = useState<ISemantic.IDomainItem | undefined>(
    {} as ISemantic.IDomainItem,
  );
  const [selectDataSet, setSelectDataSet] = useState<ISemantic.IDatasetItem>();
  const [domainList, setDomainList] = useState<ISemantic.IDomainItem[]>([]);

  const modelModel = useModel('SemanticModel.modelData');
  const metricModel = useModel('SemanticModel.metricData');
  const dimensionModel = useModel('SemanticModel.dimensionData');

  const { setSelectModel } = modelModel;
  const { setSelectDimension } = dimensionModel;
  const { setSelectMetric } = metricModel;

  useEffect(() => {
    setSelectModel(undefined);
    setSelectDimension(undefined);
    setSelectMetric(undefined);
    setSelectDataSet(undefined);
  }, [selectDomain]);

  return {
    selectDomain,
    selectDataSet,
    selectDomainId: selectDomain?.id,
    selectDomainName: selectDomain?.name,
    domainList,
    setSelectDomain,
    setSelectDataSet,
    setDomainList,
  };
}
