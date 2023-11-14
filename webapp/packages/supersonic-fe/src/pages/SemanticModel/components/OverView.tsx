import React from 'react';
import { ISemantic } from '../data';
import { connect } from 'umi';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';
import ModelTable from './ModelTable';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
  domainManger: StateType;
  dispatch: Dispatch;
};

const OverView: React.FC<Props> = ({ modelList, disabledEdit = false, onModelChange }) => {
  return (
    <div style={{ padding: '20px' }}>
      <ModelTable modelList={modelList} disabledEdit={disabledEdit} onModelChange={onModelChange} />
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(OverView);
