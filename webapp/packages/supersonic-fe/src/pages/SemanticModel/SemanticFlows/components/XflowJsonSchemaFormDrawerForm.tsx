import React, { useEffect, useState } from 'react';
import { Drawer } from 'antd';
import { WorkspacePanel, useXFlowApp, useModelAsync, XFlowGraphCommands } from '@antv/xflow';
import { useJsonSchemaFormModel } from '@antv/xflow-extension/es/canvas-json-schema-form/service';
import { NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE } from '../ConfigModelService';
import { connect } from 'umi';
import { DATASOURCE_NODE_RENDER_ID } from '../constant';
import DataSourceRelationFormDrawer from './DataSourceRelationFormDrawer';
import { GraphApi } from '../service';
import type { StateType } from '../../model';
import DataSource from '../../Datasource';

export type CreateFormProps = {
  controlMapService: any;
  formSchemaService: any;
  formValueUpdateService: any;
  domainManger: StateType;
};

const XflowJsonSchemaFormDrawerForm: React.FC<CreateFormProps> = (props) => {
  const { domainManger } = props;
  const [visible, setVisible] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState<boolean>(false);
  const [dataSourceItem, setDataSourceItem] = useState<any>();
  const [nodeDataSource, setNodeDataSource] = useState<any>({
    sourceData: {},
    targetData: {},
  });

  const app = useXFlowApp();
  // 借用JsonSchemaForm钩子函数对元素状态进行监听
  const { state, commandService, modelService } = useJsonSchemaFormModel({
    ...props,
    targetType: ['node', 'edge', 'canvas', 'group'],
    position: {},
  });

  const [modalOpenState] = useModelAsync({
    getModel: async () => {
      return await modelService.awaitModel(NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE.ID);
    },
    initialState: false,
  });

  useEffect(() => {
    const { open } = modalOpenState as any;
    setVisible(open);
  }, [modalOpenState]);

  useEffect(() => {
    const { targetType, targetData } = state;
    if (targetType && ['node', 'edge'].includes(targetType)) {
      const { renderKey, payload } = targetData as any;
      if (renderKey === DATASOURCE_NODE_RENDER_ID) {
        setDataSourceItem(payload);
        setCreateModalVisible(true);
      } else {
        const { sourceNodeData, targetNodeData } = targetData as any;
        setNodeDataSource({
          sourceData: sourceNodeData.payload,
          targetData: targetNodeData.payload,
        });
        setVisible(true);
      }
    }
  }, [state]);

  const resetSelectedNode = async () => {
    const x6Graph = await app.graphProvider.getGraphInstance();
    x6Graph.resetSelection();
  };

  const handleDataSourceRelationDrawerClose = () => {
    resetSelectedNode();
    setVisible(false);
  };

  return (
    <WorkspacePanel position={{}}>
      <DataSourceRelationFormDrawer
        domainId={domainManger.selectDomainId}
        nodeDataSource={nodeDataSource}
        onClose={() => {
          handleDataSourceRelationDrawerClose();
        }}
        open={visible}
      />
      <Drawer
        width={'100%'}
        destroyOnClose
        title="数据源编辑"
        open={createModalVisible}
        onClose={() => {
          resetSelectedNode();
          setCreateModalVisible(false);
          setDataSourceItem(undefined);
        }}
        footer={null}
      >
        <DataSource
          initialValues={dataSourceItem}
          domainId={Number(domainManger?.selectDomainId)}
          onSubmitSuccess={(dataSourceInfo: any) => {
            setCreateModalVisible(false);
            const { targetCell, targetData } = state;
            targetCell?.setData({
              ...targetData,
              label: dataSourceInfo.name,
              payload: dataSourceInfo,
              id: `dataSource-${dataSourceInfo.id}`,
            });
            setDataSourceItem(undefined);
            commandService.executeCommand(XFlowGraphCommands.SAVE_GRAPH_DATA.id, {
              saveGraphDataService: (meta, graphData) => GraphApi.saveGraphData!(meta, graphData),
            });
          }}
        />
      </Drawer>
    </WorkspacePanel>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(XflowJsonSchemaFormDrawerForm);
