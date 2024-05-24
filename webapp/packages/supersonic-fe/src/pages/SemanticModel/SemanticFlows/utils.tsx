import type { NsGraph } from '@antv/xflow';
import { uuidv4 } from '@antv/xflow';
import { GraphApi } from './service';
import { NODE_WIDTH, NODE_HEIGHT } from './constant';
import moment from 'moment';
import { jsonParse } from '@/utils/utils';
import type { GraphConfigListItem, RelationListItem } from './data';

export const getEdgesNodesIds = (edges: NsGraph.IEdgeConfig[], type?: 'source' | 'target') => {
  const hasEdgesNodesIds = edges.reduce((nodesList: string[], item: NsGraph.IEdgeConfig) => {
    const { source, target } = item;
    if (!type) {
      nodesList.push(source, target);
    } else if (type === 'source') {
      nodesList.push(source);
    } else if (type === 'target') {
      nodesList.push(target);
    }

    return nodesList;
  }, []);
  const uniqueHasEdgesNodesIds = Array.from(new Set(hasEdgesNodesIds));
  return uniqueHasEdgesNodesIds;
};

export const computedSingerNodesEdgesPosition = ({ nodes, edges }: NsGraph.IGraphData) => {
  const hasEdgesNodesIds = getEdgesNodesIds(edges);
  const defaultXPostion = 100;
  const defaultYPostion = 100;
  const paddingSize = 50;
  let xPosistion = defaultXPostion;
  const yPostition = defaultYPostion;
  const positionNodes = nodes.reduce(
    (nodesList: NsGraph.INodeConfig[], item: NsGraph.INodeConfig, index: number) => {
      const { id, width, height = NODE_HEIGHT } = item;
      if (!hasEdgesNodesIds.includes(id)) {
        xPosistion = xPosistion + (width || NODE_WIDTH + paddingSize) * index;
      }
      nodesList.push({
        ...item,
        x: xPosistion,
        y: height > yPostition ? height + paddingSize : yPostition,
      });
      return nodesList;
    },
    [],
  );
  return { nodes: positionNodes, edges };
};

export const addClassInfoAsDataSourceParents = (
  { nodes = [], edges = [] }: NsGraph.IGraphData,
  domainManger: any,
) => {
  const { selectDomainId, selectDomainName } = domainManger;
  const sourceId = `classNodeId-${selectDomainId}`;
  const classNode = {
    ...GraphApi.NODE_COMMON_PROPS,
    id: sourceId,
    label: selectDomainName,
    ports: GraphApi.createPorts(sourceId),
  };
  const classEdges = nodes.reduce((edgesList: NsGraph.IEdgeConfig[], item: NsGraph.INodeConfig) => {
    const { id } = item;

    const sourcePortId = `${sourceId}-output-1`;
    const edge = {
      id: uuidv4(),
      source: sourceId,
      target: id,
      sourcePortId,
      targetPortId: `${id}-input-1`,
    };
    edgesList.push(edge);
    return edgesList;
  }, []);
  const graphData = {
    nodes: [classNode, ...nodes],
    edges: [...edges, ...classEdges],
  };
  return graphData;
};

export const addDataSourceInfoAsDimensionParents = (
  { nodes = [], edges = [] }: NsGraph.IGraphData,
  targetDataSource: NsGraph.INodeConfig,
) => {
  const { id: sourceId } = targetDataSource;
  const dimensionEdges = nodes.reduce(
    (edgesList: NsGraph.IEdgeConfig[], item: NsGraph.INodeConfig) => {
      const { id } = item;

      const sourcePortId = `${sourceId}-output-1`;
      const edge = {
        id: uuidv4(),
        source: sourceId,
        target: id,
        sourcePortId,
        targetPortId: `${id}-input-1`,
      };
      edgesList.push(edge);
      return edgesList;
    },
    [],
  );
  const graphData = {
    nodes: [targetDataSource, ...nodes],
    edges: [...edges, ...dimensionEdges],
  };
  return graphData;
};

export const getGraphConfigFromList = (configList: GraphConfigListItem[]) => {
  configList.sort((a, b) => moment(b.updatedAt).valueOf() - moment(a.updatedAt).valueOf());
  const targetConfig = configList[0];
  if (targetConfig) {
    const { config, id } = targetConfig;
    return {
      config: jsonParse(config),
      id,
    };
  }
  return;
};

export const getRelationConfigInfo = (
  fromDataSourceId: number,
  toDataSourceId: number,
  relationList: RelationListItem[],
) => {
  const relationConfig = relationList.filter((item: RelationListItem) => {
    const { datasourceFrom, datasourceTo } = item;
    return fromDataSourceId === datasourceFrom && toDataSourceId === datasourceTo;
  })[0];
  return relationConfig;
};
