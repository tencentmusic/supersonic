import React from 'react';
import { ISemantic } from '../data';
import DataSetTable from './components/DataSetTable';

type Props = {
  disabledEdit?: boolean;
  dataSetList: ISemantic.IDatasetItem[];
};

const View: React.FC<Props> = ({ dataSetList, disabledEdit = false }) => {
  return (
    <div style={{ padding: '15px 20px' }}>
      <DataSetTable disabledEdit={disabledEdit} dataSetList={dataSetList} />
    </div>
  );
};

export default View;
