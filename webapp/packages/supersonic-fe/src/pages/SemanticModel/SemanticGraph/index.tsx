import React, { useEffect, useState, useRef } from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
import { IGroup } from '@antv/g-base';
import type { Dispatch } from 'umi';
import {
  typeConfigs,
  formatterRelationData,
  loopNodeFindDataSource,
  getNodeConfigByType,
  flatGraphDataNode,
} from './utils';
import { message } from 'antd';
import { getDomainSchemaRela } from '../service';
import { Item, TreeGraphData, NodeConfig, IItemBaseConfig } from '@antv/g6-core';
import initToolBar from './components/ToolBar';
import initTooltips from './components/ToolTips';
import initContextMenu from './components/ContextMenu';
import initLegend from './components/Legend';
import { SemanticNodeType } from '../enum';
import G6 from '@antv/g6';
import { ISemantic, IDataSource } from '../data';

import DimensionInfoModal from '../components/DimensionInfoModal';
import MetricInfoCreateForm from '../components/MetricInfoCreateForm';
import DeleteConfirmModal from './components/DeleteConfirmModal';
import { cloneDeep } from 'lodash';

type Props = {
  domainId: number;
  graphShowType: SemanticNodeType;
  domainManger: StateType;
  dispatch: Dispatch;
};

const DomainManger: React.FC<Props> = ({
  domainManger,
  domainId,
  graphShowType = SemanticNodeType.DIMENSION,
  dispatch,
}) => {
  const ref = useRef(null);
  const [graphData, setGraphData] = useState<TreeGraphData>();
  const [createDimensionModalVisible, setCreateDimensionModalVisible] = useState<boolean>(false);
  const [createMetricModalVisible, setCreateMetricModalVisible] = useState<boolean>(false);

  const legendDataRef = useRef<any[]>([]);
  const graphRef = useRef<any>(null);
  const legendDataFilterFunctions = useRef<any>({});
  const [dimensionItem, setDimensionItem] = useState<ISemantic.IDimensionItem>();

  const [metricItem, setMetricItem] = useState<ISemantic.IMetricItem>();

  const [nodeDataSource, setNodeDataSource] = useState<any>();

  const { dimensionList, metricList } = domainManger;

  const dimensionListRef = useRef<ISemantic.IDimensionItem[]>([]);
  const metricListRef = useRef<ISemantic.IMetricItem[]>([]);

  const [confirmModalOpenState, setConfirmModalOpenState] = useState<boolean>(false);

  // const toggleNodeVisibility = (graph: Graph, node: Item, visible: boolean) => {
  //   if (visible) {
  //     graph.showItem(node);
  //   } else {
  //     graph.hideItem(node);
  //   }
  // };

  useEffect(() => {
    dimensionListRef.current = dimensionList;
    metricListRef.current = metricList;
  }, [dimensionList, metricList]);

  // const toggleChildrenVisibility = (graph: Graph, node: Item, visible: boolean) => {
  //   const model = node.getModel();
  //   if (Array.isArray(model.children)) {
  //     model.children.forEach((child) => {
  //       const childNode = graph.findById(child.id);
  //       toggleNodeVisibility(graph, childNode, visible);
  //       toggleChildrenVisibility(graph, childNode, visible);
  //     });
  //   }
  // };

  const changeGraphData = (data: IDataSource.IDataSourceItem[], type: SemanticNodeType) => {
    const relationData = formatterRelationData(data, type);
    const legendList = relationData.map((item: any) => {
      const { id, name } = item;
      return {
        id,
        label: name,
        order: 4,
        ...typeConfigs.datasource,
      };
    });
    legendDataRef.current = legendList;
    const graphRootData = {
      id: 'root',
      name: domainManger.selectDomainName,
      children: relationData,
    };
    //
    return graphRootData;
  };

  const queryDataSourceList = async (params: {
    domainId: number;
    graphShowType?: SemanticNodeType;
  }) => {
    const { code, data } = await getDomainSchemaRela(params.domainId);
    if (code === 200) {
      if (data) {
        const graphRootData = changeGraphData(data, params.graphShowType || graphShowType);
        setGraphData(graphRootData);
        return graphRootData;
      }
      return false;
    } else {
      return false;
    }
  };

  useEffect(() => {
    graphRef.current = null;
    queryDataSourceList({ domainId });
  }, [domainId, graphShowType]);

  const getLegendDataFilterFunctions = () => {
    legendDataRef.current.map((item: any) => {
      const { id } = item;
      legendDataFilterFunctions.current = {
        ...legendDataFilterFunctions.current,
        [id]: (d: any) => {
          if (d.legendType === id) {
            return true;
          }
          return false;
        },
      };
    });
  };

  const setAllActiveLegend = (legend: any) => {
    const legendCanvas = legend._cfgs.legendCanvas;
    if (!legendCanvas) {
      return;
    }
    // 从图例中找出node-group节点;
    const group = legendCanvas.find((e: any) => e.get('name') === 'node-group');
    // 数据源的图例节点在node-group中的children中；
    const groups = group.get('children');
    groups.forEach((itemGroup: any) => {
      const labelText = itemGroup.find((e: any) => e.get('name') === 'circle-node-text');
      // legend中activateLegend事件触发在图例节点的Text上，方法中存在向上溯源的逻辑：const shapeGroup = shape.get('parent');
      // 因此复用实例方法时，在这里不能直接将图例节点传入，需要在节点的children中找任意一个元素作为入参；
      legend.activateLegend(labelText);
    });
  };

  const handleContextMenuClickEdit = (item: IItemBaseConfig) => {
    const targetData = item.model;
    if (!targetData) {
      return;
    }

    const datasource = loopNodeFindDataSource(item);
    if (datasource) {
      setNodeDataSource({
        id: datasource.uid,
        name: datasource.name,
      });
    }
    if (targetData.nodeType === SemanticNodeType.DIMENSION) {
      const targetItem = dimensionListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setDimensionItem({ ...targetItem });
        setCreateDimensionModalVisible(true);
      } else {
        message.error('获取维度初始化数据失败');
      }
    }
    if (targetData.nodeType === SemanticNodeType.METRIC) {
      const targetItem = metricListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setMetricItem({ ...targetItem });
        setCreateMetricModalVisible(true);
      } else {
        message.error('获取指标初始化数据失败');
      }
    }
  };

  const handleContextMenuClickCreate = (item: IItemBaseConfig) => {
    const datasource = item.model;
    if (!datasource) {
      return;
    }
    setNodeDataSource({
      id: datasource.uid,
      name: datasource.name,
    });
    if (graphShowType === SemanticNodeType.DIMENSION) {
      setCreateDimensionModalVisible(true);
    }
    if (graphShowType === SemanticNodeType.METRIC) {
      setCreateMetricModalVisible(true);
    }
    setDimensionItem(undefined);
    setMetricItem(undefined);
  };

  const handleContextMenuClickDelete = (item: IItemBaseConfig) => {
    const targetData = item.model;
    if (!targetData) {
      return;
    }
    if (targetData.nodeType === SemanticNodeType.DIMENSION) {
      const targetItem = dimensionListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setDimensionItem({ ...targetItem });
        setConfirmModalOpenState(true);
      } else {
        message.error('获取维度初始化数据失败');
      }
    }
    if (targetData.nodeType === SemanticNodeType.METRIC) {
      const targetItem = metricListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setMetricItem({ ...targetItem });
        setConfirmModalOpenState(true);
      } else {
        message.error('获取指标初始化数据失败');
      }
    }
  };

  const handleContextMenuClick = (key: string, item: Item) => {
    if (!item?._cfg) {
      return;
    }
    switch (key) {
      case 'edit':
        handleContextMenuClickEdit(item._cfg);
        break;
      case 'delete':
        handleContextMenuClickDelete(item._cfg);
        break;
      case 'create':
        handleContextMenuClickCreate(item._cfg);
        break;
      default:
        break;
    }
  };

  const graphConfigMap = {
    dendrogram: {
      defaultEdge: {
        type: 'cubic-horizontal',
      },
      layout: {
        type: 'dendrogram',
        direction: 'LR',
        animate: false,
        nodeSep: 200,
        rankSep: 300,
        radial: true,
      },
    },
    mindmap: {
      defaultEdge: {
        type: 'polyline',
      },
      layout: {
        type: 'mindmap',
        animate: false,
        direction: 'H',
        getHeight: () => {
          return 50;
        },
        getWidth: () => {
          return 50;
        },
        getVGap: () => {
          return 10;
        },
        getHGap: () => {
          return 50;
        },
      },
    },
  };

  useEffect(() => {
    if (!Array.isArray(graphData?.children)) {
      return;
    }
    const container = document.getElementById('semanticGraph');
    const width = container!.scrollWidth;
    const height = container!.scrollHeight || 500;

    const graph = graphRef.current;

    if (!graph && graphData) {
      const graphNodeList = flatGraphDataNode(graphData.children);
      const graphConfigKey = graphNodeList.length > 20 ? 'dendrogram' : 'mindmap';

      getLegendDataFilterFunctions();
      const toolbar = initToolBar({ refreshGraphData });
      const tooltip = initTooltips();
      const contextMenu = initContextMenu({
        graphShowType,
        onMenuClick: handleContextMenuClick,
      });
      const legend = initLegend({
        nodeData: legendDataRef.current,
        filterFunctions: { ...legendDataFilterFunctions.current },
      });

      graphRef.current = new G6.TreeGraph({
        container: 'semanticGraph',
        width,
        height,
        modes: {
          default: [
            {
              type: 'collapse-expand',
              onChange: function onChange(item, collapsed) {
                const data = item!.get('model');
                data.collapsed = collapsed;
                return true;
              },
            },
            'drag-node',
            'drag-canvas',
            // 'activate-relations',
            'zoom-canvas',
            {
              type: 'activate-relations',
              trigger: 'mouseenter', // 触发方式，可以是 'mouseenter' 或 'click'
              resetSelected: true, // 点击空白处时，是否取消高亮
            },
          ],
        },
        defaultNode: {
          size: 26,
          anchorPoints: [
            [0, 0.5],
            [1, 0.5],
          ],
          labelCfg: {
            position: 'right',
            offset: 5,
            style: {
              stroke: '#fff',
              lineWidth: 4,
            },
          },
        },
        defaultEdge: {
          type: graphConfigMap[graphConfigKey].defaultEdge.type,
        },
        layout: {
          ...graphConfigMap[graphConfigKey].layout,
        },
        plugins: [legend, tooltip, toolbar, contextMenu],
      });
      graphRef.current.set('initGraphData', graphData);
      const legendCanvas = legend._cfgs.legendCanvas;

      // legend模式事件方法bindEvents会有点击图例空白清空选中的逻辑，在注册click事件前，先将click事件队列清空；
      legend._cfgs.legendCanvas._events.click = [];
      // legendCanvas.on('click', (e) => {
      //   const shape = e.target;
      //   const shapeGroup = shape.get('parent');
      //   const shapeGroupId = shapeGroup?.cfg?.id;
      //   if (shapeGroupId) {
      //     const isActive = shapeGroup.get('active');
      //     const targetNode = graphRef.current.findById(shapeGroupId);
      //     toggleNodeVisibility(graphRef.current, targetNode, isActive);
      //     toggleChildrenVisibility(graphRef.current, targetNode, isActive);
      //   }
      // });
      legendCanvas.on('click', () => {
        // @ts-ignore findLegendItemsByState为Legend的 private方法，忽略ts校验
        const activedNodeList = legend.findLegendItemsByState('active');
        // 获取当前所有激活节点后进行数据遍历筛选；
        const activedNodeIds = activedNodeList.map((item: IGroup) => {
          return item.cfg.id;
        });
        const graphDataClone = cloneDeep(graphData);
        const filterGraphDataChildren = Array.isArray(graphDataClone?.children)
          ? graphDataClone.children.reduce((children: TreeGraphData[], item: TreeGraphData) => {
              if (activedNodeIds.includes(item.id)) {
                children.push(item);
              }
              return children;
            }, [])
          : [];
        graphDataClone.children = filterGraphDataChildren;
        refreshGraphData(graphDataClone);
      });

      graphRef.current.node(function (node: NodeConfig) {
        return getNodeConfigByType(node, {
          label: node.name,
        });
      });

      graphRef.current.data(graphData);
      graphRef.current.render();
      graphRef.current.fitView([80, 80]);

      setAllActiveLegend(legend);

      const rootNode = graphRef.current.findById('root');
      graphRef.current.hideItem(rootNode);
      if (typeof window !== 'undefined')
        window.onresize = () => {
          if (!graphRef.current || graphRef.current.get('destroyed')) return;
          if (!container || !container.scrollWidth || !container.scrollHeight) return;
          graphRef.current.changeSize(container.scrollWidth, container.scrollHeight);
        };
    }
  }, [graphData]);

  const updateGraphData = async () => {
    const graphRootData = await queryDataSourceList({ domainId });
    if (graphRootData) {
      refreshGraphData(graphRootData);
    }
  };

  const refreshGraphData = (graphRootData: TreeGraphData) => {
    graphRef.current.changeData(graphRootData);
    const rootNode = graphRef.current.findById('root');
    graphRef.current.hideItem(rootNode);
    graphRef.current.fitView();
  };

  return (
    <>
      <div
        ref={ref}
        key={`${domainId}-${graphShowType}`}
        id="semanticGraph"
        style={{ width: '100%', height: '100%' }}
      />
      {createDimensionModalVisible && (
        <DimensionInfoModal
          domainId={domainId}
          bindModalVisible={createDimensionModalVisible}
          dimensionItem={dimensionItem}
          dataSourceList={nodeDataSource ? [nodeDataSource] : []}
          onSubmit={() => {
            setCreateDimensionModalVisible(false);
            updateGraphData();
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                domainId,
              },
            });
          }}
          onCancel={() => {
            setCreateDimensionModalVisible(false);
          }}
        />
      )}
      {createMetricModalVisible && (
        <MetricInfoCreateForm
          domainId={domainId}
          key={metricItem?.id}
          datasourceId={nodeDataSource.id}
          createModalVisible={createMetricModalVisible}
          metricItem={metricItem}
          onSubmit={() => {
            setCreateMetricModalVisible(false);
            updateGraphData();
            dispatch({
              type: 'domainManger/queryMetricList',
              payload: {
                domainId,
              },
            });
          }}
          onCancel={() => {
            setCreateMetricModalVisible(false);
          }}
        />
      )}
      {
        <DeleteConfirmModal
          open={confirmModalOpenState}
          onOkClick={() => {
            setConfirmModalOpenState(false);
            updateGraphData();
            graphShowType === SemanticNodeType.DIMENSION
              ? dispatch({
                  type: 'domainManger/queryDimensionList',
                  payload: {
                    domainId,
                  },
                })
              : dispatch({
                  type: 'domainManger/queryMetricList',
                  payload: {
                    domainId,
                  },
                });
          }}
          onCancelClick={() => {
            setConfirmModalOpenState(false);
          }}
          nodeType={graphShowType}
          nodeData={graphShowType === SemanticNodeType.DIMENSION ? dimensionItem : metricItem}
        />
      }
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManger);
