import React from 'react';
import { ISemantic } from '../data';
import ModelTable from './ModelTable';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
};

const OverView: React.FC<Props> = ({ modelList, disabledEdit = false, onModelChange }) => {
  return (
    <div style={{ padding: '15px 20px' }}>
      <ModelTable modelList={modelList} disabledEdit={disabledEdit} onModelChange={onModelChange} />
    </div>
  );
};

export default OverView
