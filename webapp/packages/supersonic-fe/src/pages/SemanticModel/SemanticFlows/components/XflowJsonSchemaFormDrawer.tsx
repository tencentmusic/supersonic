import React from 'react';
import { WorkspacePanel } from '@antv/xflow';
import type { NsJsonSchemaForm } from '@antv/xflow';
import XflowJsonSchemaFormDrawerForm from './XflowJsonSchemaFormDrawerForm';

export type CreateFormProps = {
  controlMapService?: any;
  formSchemaService?: any;
  formValueUpdateService?: any;
};

const XflowJsonSchemaFormDrawer: React.FC<CreateFormProps> = ({
  controlMapService,
  formSchemaService,
  formValueUpdateService,
}) => {
  const defaultFormValueUpdateService: NsJsonSchemaForm.IFormValueUpdateService = async () => {};
  const defaultFormSchemaService: NsJsonSchemaForm.IFormSchemaService = async () => {
    return { tabs: [] };
  };
  const defaultControlMapService: NsJsonSchemaForm.IControlMapService = (controlMap) => {
    return controlMap;
  };
  return (
    <WorkspacePanel position={{}}>
      <XflowJsonSchemaFormDrawerForm
        controlMapService={controlMapService || defaultControlMapService}
        formSchemaService={formSchemaService || defaultFormSchemaService}
        formValueUpdateService={formValueUpdateService || defaultFormValueUpdateService}
      />
    </WorkspacePanel>
  );
};

export default XflowJsonSchemaFormDrawer;
