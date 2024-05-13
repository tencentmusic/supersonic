import React from 'react';
import { ISemantic } from '../data';
import { connect } from 'umi';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';
import DataSetTable from './components/DataSetTable';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
  domainManger: StateType;
  dispatch: Dispatch;
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

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(View);
