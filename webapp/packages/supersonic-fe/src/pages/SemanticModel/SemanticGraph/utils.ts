import { ISemantic } from '../data';
import { SemanticNodeType } from '../enum';
import { TreeGraphData } from '@antv/g6-core';

export const typeConfigs = {
  datasource: {
    type: 'circle',
    size: 10,
  },
};

export const getDimensionChildren = (
  dimensions: ISemantic.IDimensionItem[],
  dataSourceNodeId: string,
  limit: number = 999,
) => {
  const dimensionChildrenList = dimensions.reduce(
    (dimensionChildren: any[], dimension: ISemantic.IDimensionItem) => {
      const { id } = dimension;
      dimensionChildren.push({
        ...dimension,
        nodeType: SemanticNodeType.DIMENSION,
        legendType: dataSourceNodeId,
        id: `${dataSourceNodeId}-${SemanticNodeType.DIMENSION}-${id}`,
        uid: id,
        style: {
          lineWidth: 2,
          fill: '#f0f7ff',
          stroke: '#a6ccff',
        },
      });
      return dimensionChildren;
    },
    [],
  );
  return dimensionChildrenList.slice(0, limit);
};

export const getMetricChildren = (
  metrics: ISemantic.IMetricItem[],
  dataSourceNodeId: string,
  limit: number = 999,
) => {
  const metricsChildrenList = metrics.reduce(
    (metricsChildren: any[], metric: ISemantic.IMetricItem) => {
      const { id } = metric;
      metricsChildren.push({
        ...metric,
        nodeType: SemanticNodeType.METRIC,
        legendType: dataSourceNodeId,
        id: `${dataSourceNodeId}-${SemanticNodeType.METRIC}-${id}`,
        uid: id,
        style: {
          lineWidth: 2,
          fill: '#f0f7ff',
          stroke: '#a6ccff',
        },
      });
      return metricsChildren;
    },
    [],
  );
  return metricsChildrenList.slice(0, limit);
};

export const formatterRelationData = (params: {
  dataSourceList: ISemantic.IDomainSchemaRelaList;
  limit?: number;
  type?: SemanticNodeType;
  showDataSourceId?: string[];
}): TreeGraphData[] => {
  const { type, dataSourceList, limit, showDataSourceId } = params;
  const relationData = dataSourceList.reduce(
    (relationList: TreeGraphData[], item: ISemantic.IDomainSchemaRelaItem) => {
      const { model, dimensions, metrics } = item;
      const { id } = model;
      const dataSourceNodeId = `${SemanticNodeType.DATASOURCE}-${id}`;
      let childrenList = [];
      if (type === SemanticNodeType.METRIC) {
        childrenList = getMetricChildren(metrics, dataSourceNodeId, limit);
      }
      if (type === SemanticNodeType.DIMENSION) {
        childrenList = getDimensionChildren(dimensions, dataSourceNodeId, limit);
      }
      if (!type) {
        const dimensionList = getDimensionChildren(dimensions, dataSourceNodeId, limit);
        const metricList = getMetricChildren(metrics, dataSourceNodeId, limit);
        childrenList = [...dimensionList, ...metricList];
      }
      if (!showDataSourceId || showDataSourceId.includes(dataSourceNodeId)) {
        relationList.push({
          ...model,
          legendType: dataSourceNodeId,
          id: dataSourceNodeId,
          uid: id,
          nodeType: SemanticNodeType.DATASOURCE,
          size: 40,
          children: [...childrenList],
          style: {
            lineWidth: 2,
            fill: '#BDEFDB',
            stroke: '#5AD8A6',
          },
        });
      }
      return relationList;
    },
    [],
  );
  return relationData;
};

export const loopNodeFindDataSource: any = (node: any) => {
  const { model, parent } = node;
  if (model?.nodeType === SemanticNodeType.DATASOURCE) {
    return model;
  }
  const parentNode = parent?._cfg;
  if (parentNode) {
    return loopNodeFindDataSource(parentNode);
  }
  return false;
};

export const getNodeConfigByType = (nodeData: any, defaultConfig = {}) => {
  const { nodeType } = nodeData;
  const labelCfg = { style: { fill: '#3c3c3c' } };
  switch (nodeType) {
    case SemanticNodeType.DATASOURCE: {
      return {
        ...defaultConfig,
        labelCfg: { position: 'bottom', ...labelCfg },
        // type: 'rect',
        size: [80, 40],
        // linkPoints: {
        //   // top: true,
        //   right: true,
        //   // bottom: true,
        //   left: true,
        //   /* linkPoints' size, 8 by default */
        //   //   size: 5,
        //   /* linkPoints' style */
        //   //   fill: '#ccc',
        //   //   stroke: '#333',
        //   //   lineWidth: 2,
        // },
        // style: {
        //   fill: '#eee',
        //   stroke: '#ccc',
        // },
      };
    }
    case SemanticNodeType.DIMENSION:
      return {
        ...defaultConfig,
        labelCfg: { position: 'right', ...labelCfg },
      };
    case SemanticNodeType.METRIC:
      return {
        ...defaultConfig,
        style: {
          lineWidth: 2,
          fill: '#fffbe6',
          stroke: '#ffe58f',
        },
        labelCfg: { position: 'right', ...labelCfg },
      };
    default:
      return defaultConfig;
  }
};

export const flatGraphDataNode = (graphData: any[]) => {
  return graphData.reduce((nodeList: any[], item: any) => {
    const { children } = item;
    if (Array.isArray(children)) {
      nodeList.push(...children);
    }
    return nodeList;
  }, []);
};

interface Node {
  label: string;
  children?: Node[];
}

export const findNodesByLabel = (query: string, nodes: Node[]): Node[] => {
  const result: Node[] = [];

  for (const node of nodes) {
    let match = false;
    let children: Node[] = [];

    // 如果节点的label包含查询字符串，我们将其标记为匹配
    if (node.label.includes(query)) {
      match = true;
    }

    // 我们还需要在子节点中进行搜索
    if (node.children) {
      children = findNodesByLabel(query, node.children);
      if (children.length > 0) {
        match = true;
      }
    }

    // 如果节点匹配或者其子节点匹配，我们将其添加到结果中
    if (match) {
      result.push({ ...node, children });
    }
  }

  return result;
};
