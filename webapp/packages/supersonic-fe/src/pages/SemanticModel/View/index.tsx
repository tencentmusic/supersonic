import React from 'react';
import { ISemantic } from '../data';
import DataSetTable from './components/DataSetTable';

type Props = {
  disabledEdit?: boolean;
};

const View: React.FC<Props> = ({ disabledEdit = false }) => {
  return (
    <div style={{ padding: '15px 20px' }}>
      <DataSetTable disabledEdit={disabledEdit} />
    </div>
  );
};

export default View;
