import React from 'react';
import { ISemantic } from '../data';
import DataSetTable from './components/DataSetTable';

type Props = {
  isCurrent: boolean;
  disabledEdit?: boolean;
  dataSetList: ISemantic.IDatasetItem[];
};

const View: React.FC<Props> = ({ isCurrent, dataSetList, disabledEdit = false }) => {
  return (
    <div style={{ padding: '15px 20px' }}>
      <DataSetTable isCurrent={isCurrent} disabledEdit={disabledEdit} dataSetList={dataSetList} />
    </div>
  );
};

export default View;
