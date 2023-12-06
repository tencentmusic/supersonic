/* eslint-disable @typescript-eslint/no-unused-vars */
import { DATASOURCE_NODE_RENDER_ID, NODE_WIDTH, NODE_HEIGHT } from './constant';
import { uuidv4, NsGraph, NsGraphStatusCommand } from '@antv/xflow';
import type { NsRenameNodeCmd } from './CmdExtensions/CmdRenameNodeModal';
import type { NsNodeCmd, NsEdgeCmd, NsGraphCmd } from '@antv/xflow';
import type { NsDeployDagCmd } from './CmdExtensions/CmdDeploy';
import { getRelationConfigInfo, addClassInfoAsDataSourceParents } from './utils';
import { cloneDeep } from 'lodash';
import type { IDataSource } from '../data';
import { SemanticNodeType } from '../enum';
import {
  getModelList,
  deleteDatasource,
  getDimensionList,
  createOrUpdateViewInfo,
  deleteDatasourceRela,
} from '../service';
import { message } from 'antd';

/** mock 后端接口调用 */
export namespace GraphApi {
  export const NODE_COMMON_PROPS = {
    renderKey: DATASOURCE_NODE_RENDER_ID,
    width: NODE_WIDTH,
    height: NODE_HEIGHT,
  } as const;

  /** 查图的meta元信息 */
  export const queryGraphMeta: NsGraphCmd.GraphMeta.IArgs['graphMetaService'] = async (args) => {
    return { ...args, flowId: args.meta.flowId };
  };
  export const createPorts = (nodeId: string, count = 1, layout = 'LR') => {
    const ports = [] as NsGraph.INodeAnchor[];
    Array(count)
      .fill(1)
      .forEach((item, idx) => {
        const portIdx = idx + 1;
        ports.push(
          ...[
            {
              id: `${nodeId}-input-${portIdx}`,
              type: NsGraph.AnchorType.INPUT,
              group: layout === 'TB' ? NsGraph.AnchorGroup.TOP : NsGraph.AnchorGroup.LEFT,
              tooltip: `输入桩-${portIdx}`,
            },
            {
              id: `${nodeId}-output-${portIdx}`,
              type: NsGraph.AnchorType.OUTPUT,
              group: layout === 'TB' ? NsGraph.AnchorGroup.BOTTOM : NsGraph.AnchorGroup.RIGHT,
              tooltip: `输出桩-${portIdx}`,
            },
          ],
        );
      });
    return ports;
  };

  export const createDataSourceNode = (dataSourceItem: IDataSource.IDataSourceItem) => {
    const { id, name } = dataSourceItem;
    const nodeId = `${SemanticNodeType.DATASOURCE}-${id}`;
    return {
      ...NODE_COMMON_PROPS,
      id: nodeId,
      label: `${name}-${id}`,
      ports: createPorts(nodeId),
      payload: dataSourceItem,
    };
  };

  /** 删除节点的api */
  export const delDataSource = async (nodeConfig: any) => {
    const dataSourceId = nodeConfig.targetData?.payload?.id;
    if (!dataSourceId) {
      // dataSourceId 不存在时，为未保存节点，直接返回true删除
      return true;
    }
    const { code, msg } = await deleteDatasource(dataSourceId);
    if (code === 200) {
      return true;
    }
    message.error(msg);
    return false;
  };

  export const loadDataSourceData = async (args: NsGraph.IGraphMeta) => {
    const { domainManger, graphConfig } = args.meta;
    const { selectDomainId } = domainManger;
    const { code, data = [] } = await getModelList(selectDomainId);
    const dataSourceMap = data.reduce(
      (itemMap: Record<string, IDataSource.IDataSourceItem>, item: IDataSource.IDataSourceItem) => {
        const { id, name } = item;
        itemMap[`${SemanticNodeType.DATASOURCE}-${id}`] = item;

        itemMap[name] = item;
        return itemMap;
      },
      {},
    );
    if (code === 200) {
      // 如果config存在，将数据源信息进行merge
      if (graphConfig?.id && graphConfig?.config) {
        const { config } = graphConfig;
        const { nodes, edges } = config;
        const nodesMap = nodes.reduce(
          (itemMap: Record<string, NsGraph.INodeConfig>, item: NsGraph.INodeConfig) => {
            itemMap[item.id] = item;
            return itemMap;
          },
          {},
        );
        let mergeNodes = nodes;
        let mergeEdges = edges;
        if (Array.isArray(nodes)) {
          mergeNodes = data.reduce(
            (mergeNodeList: NsGraph.INodeConfig[], item: IDataSource.IDataSourceItem) => {
              const { id } = item;
              const targetDataSourceItem = nodesMap[`${SemanticNodeType.DATASOURCE}-${id}`];
              if (targetDataSourceItem) {
                mergeNodeList.push({
                  ...targetDataSourceItem,
                  payload: item,
                });
              } else {
                mergeNodeList.push(createDataSourceNode(item));
              }
              return mergeNodeList;
            },
            [],
          );
        }
        if (Array.isArray(edges)) {
          mergeEdges = edges.reduce(
            (mergeEdgeList: NsGraph.IEdgeConfig[], item: NsGraph.IEdgeConfig) => {
              const { source, target } = item;
              const sourceDataSourceItem = dataSourceMap[source];
              const targetDataSourceItem = dataSourceMap[target];
              if (sourceDataSourceItem && targetDataSourceItem) {
                const tempItem = { ...item };
                tempItem.sourceNodeData.payload = sourceDataSourceItem;
                tempItem.targetNodeData.payload = targetDataSourceItem;
                mergeEdgeList.push(tempItem);
              }
              return mergeEdgeList;
            },
            [],
          );
        }

        return { nodes: mergeNodes, edges: mergeEdges };
      }

      //  如果config不存在，进行初始化
      const nodes: NsGraph.INodeConfig[] = data.map((item: IDataSource.IDataSourceItem) => {
        return createDataSourceNode(item);
      });
      return addClassInfoAsDataSourceParents({ nodes, edges: [] }, domainManger);
    }
    return {};
  };

  export const loadDimensionData = async (args: NsGraph.IGraphMeta) => {
    const { domainManger } = args.meta;
    const { selectModelId } = domainManger;
    const { code, data } = await getDimensionList({ modelId: selectModelId });
    if (code === 200) {
      const { list } = data;
      const nodes: NsGraph.INodeConfig[] = list.map((item: any) => {
        const { id, name } = item;
        const nodeId = `${SemanticNodeType.DIMENSION}-${id}`;
        return {
          ...NODE_COMMON_PROPS,
          id: nodeId,
          label: `${name}-${id}`,
          ports: createPorts(nodeId),
          payload: item,
        };
      });
      return { nodes, edges: [] };
    }
    return {};
  };

  /** 保存图数据的api */
  export const saveGraphData: NsGraphCmd.SaveGraphData.IArgs['saveGraphDataService'] = async (
    graphMeta: NsGraph.IGraphMeta,
    graphData: NsGraph.IGraphData,
  ) => {
    const { commandService } = graphMeta;
    const initGraphCmdsState = commandService.getGlobal('initGraphCmdsSuccess');
    // 如果graph处于初始化阶段，则禁止配置文件保存操作
    if (!initGraphCmdsState) {
      return;
    }
    const tempGraphData = cloneDeep(graphData);
    const { edges, nodes } = tempGraphData;
    if (Array.isArray(nodes)) {
      tempGraphData.nodes = nodes.map((item: any) => {
        delete item.payload;
        return item;
      });
    }
    if (Array.isArray(edges)) {
      tempGraphData.edges = edges.map((item: any) => {
        delete item.sourceNodeData.payload;
        delete item.targetNodeData.payload;
        return item;
      });
    }
    const { domainManger, graphConfig } = graphMeta.meta;
    const { code, msg } = await createOrUpdateViewInfo({
      id: graphConfig?.id,
      modelId: domainManger.selectModelId,
      type: 'datasource',
      config: JSON.stringify(tempGraphData),
    });
    if (code !== 200) {
      message.error(msg);
    }
    return {
      success: true,
      data: graphData,
    };
  };
  /** 部署图数据的api */
  export const deployDagService: NsDeployDagCmd.IDeployDagService = async (
    meta: NsGraph.IGraphMeta,
    graphData: NsGraph.IGraphData,
  ) => {
    return {
      success: true,
      data: graphData,
    };
  };

  /** 添加节点api */
  export const addNode: NsNodeCmd.AddNode.IArgs['createNodeService'] = async (
    args: NsNodeCmd.AddNode.IArgs,
  ) => {
    console.info('addNode service running, add node:', args);

    const { id, ports = createPorts(id, 1), groupChildren } = args.nodeConfig;
    const nodeId = id || uuidv4();
    /** 这里添加连线桩 */
    const node: NsNodeCmd.AddNode.IArgs['nodeConfig'] = {
      ...NODE_COMMON_PROPS,
      ...args.nodeConfig,
      id: nodeId,
      ports: ports,
    };
    /** group没有链接桩 */
    if (groupChildren && groupChildren.length) {
      node.ports = [];
    }
    return node;
  };

  /** 更新节点 name，可能依赖接口判断是否重名，返回空字符串时，不更新 */
  export const renameNode: NsRenameNodeCmd.IUpdateNodeNameService = async (
    name,
    node,
    graphMeta,
  ) => {
    return { err: null, nodeName: name };
  };

  /** 删除节点的api */
  export const delNode: NsNodeCmd.DelNode.IArgs['deleteNodeService'] = async (args: any) => {
    const { type } = args.nodeConfig;
    switch (type) {
      case 'dataSource':
        return await delDataSource(args.nodeConfig);
      case 'class':
        return true;
      default:
        return true;
    }
  };

  /** 添加边的api */
  export const addEdge: NsEdgeCmd.AddEdge.IArgs['createEdgeService'] = async (args) => {
    console.info('addEdge service running, add edge:', args);
    const { edgeConfig } = args;
    return {
      ...edgeConfig,
      id: uuidv4(),
    };
  };

  /** 删除边的api */
  export const delEdge: NsEdgeCmd.DelEdge.IArgs['deleteEdgeService'] = async (args) => {
    console.info('delEdge service running, del edge:', args);
    const { commandService, edgeConfig } = args;
    if (!edgeConfig?.sourceNodeData || !edgeConfig?.targetNodeData) {
      return true;
    }
    const { sourceNodeData, targetNodeData } = edgeConfig as any;
    const sourceDataId = sourceNodeData.payload.id;
    const targetDataId = targetNodeData.payload.id;
    const { getGlobal } = commandService as any;
    const dataSourceRelationList = getGlobal('dataSourceRelationList');
    const relationConfig = getRelationConfigInfo(
      sourceDataId,
      targetDataId,
      dataSourceRelationList,
    );
    if (!relationConfig) {
      // 如果配置不存在则直接删除
      return true;
    }
    const { code, msg } = await deleteDatasourceRela(relationConfig.id);
    if (code === 200) {
      return true;
    }
    message.error(msg);
    return false;
  };

  let runningNodeId = 0;
  const statusMap = {} as NsGraphStatusCommand.IStatusInfo['statusMap'];
  let graphStatus: NsGraphStatusCommand.StatusEnum = NsGraphStatusCommand.StatusEnum.DEFAULT;
  export const graphStatusService: NsGraphStatusCommand.IArgs['graphStatusService'] = async () => {
    if (runningNodeId < 4) {
      statusMap[`node${runningNodeId}`] = { status: NsGraphStatusCommand.StatusEnum.SUCCESS };
      statusMap[`node${runningNodeId + 1}`] = {
        status: NsGraphStatusCommand.StatusEnum.PROCESSING,
      };
      runningNodeId += 1;
      graphStatus = NsGraphStatusCommand.StatusEnum.PROCESSING;
    } else {
      runningNodeId = 0;
      statusMap.node4 = { status: NsGraphStatusCommand.StatusEnum.SUCCESS };
      graphStatus = NsGraphStatusCommand.StatusEnum.SUCCESS;
    }
    return {
      graphStatus: graphStatus,
      statusMap: statusMap,
    };
  };
  export const stopGraphStatusService: NsGraphStatusCommand.IArgs['graphStatusService'] =
    async () => {
      Object.entries(statusMap).forEach(([, val]) => {
        const { status } = val as { status: NsGraphStatusCommand.StatusEnum };
        if (status === NsGraphStatusCommand.StatusEnum.PROCESSING) {
          val.status = NsGraphStatusCommand.StatusEnum.ERROR;
        }
      });
      return {
        graphStatus: NsGraphStatusCommand.StatusEnum.ERROR,
        statusMap: statusMap,
      };
    };
}
