import React from 'react';
import { ISemantic } from '../data';
import DataSetTable from './components/DataSetTable';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
};

const View: React.FC<Props> = ({ modelList, disabledEdit = false, onModelChange }) => {
  return (
    <div style={{ padding: '15px 20px' }}>
      <DataSetTable
        modelList={modelList}
        disabledEdit={disabledEdit}
        onModelChange={onModelChange}
      />
    </div>
  );
};

export default View
