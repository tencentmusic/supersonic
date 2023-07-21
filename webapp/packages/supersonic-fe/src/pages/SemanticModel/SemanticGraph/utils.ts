import { ISemantic, IDataSource } from '../data';
import { SemanticNodeType } from '../enum';

export const typeConfigs = {
  datasource: {
    type: 'circle',
    size: 10,
  },
};

export const getDimensionChildren = (
  dimensions: ISemantic.IDimensionItem[],
  dataSourceNodeId: string,
) => {
  const dimensionChildrenList = dimensions.reduce(
    (dimensionChildren: any[], dimension: ISemantic.IDimensionItem) => {
      const { id } = dimension;
      dimensionChildren.push({
        ...dimension,
        nodeType: SemanticNodeType.DIMENSION,
        legendType: dataSourceNodeId,
        id: `${SemanticNodeType.DIMENSION}-${id}`,
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
  return dimensionChildrenList;
};

export const getMetricChildren = (metrics: ISemantic.IMetricItem[], dataSourceNodeId: string) => {
  const metricsChildrenList = metrics.reduce(
    (metricsChildren: any[], metric: ISemantic.IMetricItem) => {
      const { id } = metric;
      metricsChildren.push({
        ...metric,
        nodeType: SemanticNodeType.METRIC,
        legendType: dataSourceNodeId,
        id: `${SemanticNodeType.METRIC}-${id}`,
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
  return metricsChildrenList;
};

export const formatterRelationData = (
  dataSourceList: IDataSource.IDataSourceItem[],
  type: SemanticNodeType = SemanticNodeType.DIMENSION,
) => {
  const relationData = dataSourceList.reduce((relationList: any[], item: any) => {
    const { datasource, dimensions, metrics } = item;
    const { id } = datasource;
    const dataSourceNodeId = `${SemanticNodeType.DATASOURCE}-${id}`;
    let childrenList = [];
    if (type === SemanticNodeType.METRIC) {
      childrenList = getMetricChildren(metrics, dataSourceNodeId);
    }
    if (type === SemanticNodeType.DIMENSION) {
      childrenList = getDimensionChildren(dimensions, dataSourceNodeId);
    }
    relationList.push({
      ...datasource,
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
    return relationList;
  }, []);
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
