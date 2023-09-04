import React, { useEffect, useState, useRef } from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
// import { IGroup } from '@antv/g-base';
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
// import initLegend from './components/Legend';
import { SemanticNodeType } from '../enum';
import G6 from '@antv/g6';
import { ISemantic, IDataSource } from '../data';
import NodeInfoDrawer from './components/NodeInfoDrawer';
import DimensionInfoModal from '../components/DimensionInfoModal';
import MetricInfoCreateForm from '../components/MetricInfoCreateForm';
import DeleteConfirmModal from './components/DeleteConfirmModal';
import ClassDataSourceTypeModal from '../components/ClassDataSourceTypeModal';
import GraphToolBar from './components/GraphToolBar';
import GraphLegend from './components/GraphLegend';
import GraphLegendVisibleModeItem from './components/GraphLegendVisibleModeItem';

// import { cloneDeep } from 'lodash';

type Props = {
  // graphShowType?: SemanticNodeType;
  domainManger: StateType;
  dispatch: Dispatch;
};

const DomainManger: React.FC<Props> = ({
  domainManger,
  // graphShowType = SemanticNodeType.DIMENSION,
  // graphShowType,
  dispatch,
}) => {
  const ref = useRef(null);
  const dataSourceRef = useRef<ISemantic.IDomainSchemaRelaList>([]);
  const [graphData, setGraphData] = useState<TreeGraphData>();
  const [createDimensionModalVisible, setCreateDimensionModalVisible] = useState<boolean>(false);
  const [createMetricModalVisible, setCreateMetricModalVisible] = useState<boolean>(false);
  const [infoDrawerVisible, setInfoDrawerVisible] = useState<boolean>(false);

  const [currentNodeData, setCurrentNodeData] = useState<any>();

  const legendDataRef = useRef<any[]>([]);
  const graphRef = useRef<any>(null);
  // const legendDataFilterFunctions = useRef<any>({});
  const [dimensionItem, setDimensionItem] = useState<ISemantic.IDimensionItem>();

  const [metricItem, setMetricItem] = useState<ISemantic.IMetricItem>();

  const [nodeDataSource, setNodeDataSource] = useState<any>();

  const [dataSourceInfoList, setDataSourceInfoList] = useState<IDataSource.IDataSourceItem[]>([]);

  const { dimensionList, metricList, selectModelId: modelId, selectDomainId } = domainManger;

  const dimensionListRef = useRef<ISemantic.IDimensionItem[]>([]);
  const metricListRef = useRef<ISemantic.IMetricItem[]>([]);

  const [confirmModalOpenState, setConfirmModalOpenState] = useState<boolean>(false);
  const [createDataSourceModalOpen, setCreateDataSourceModalOpen] = useState(false);

  const visibleModeOpenRef = useRef<boolean>(false);
  const [visibleModeOpen, setVisibleModeOpen] = useState<boolean>(false);

  const graphShowTypeRef = useRef<SemanticNodeType>();
  const [graphShowTypeState, setGraphShowTypeState] = useState<SemanticNodeType>();

  const graphLegendDataSourceIds = useRef<string[]>();

  useEffect(() => {
    dimensionListRef.current = dimensionList;
    metricListRef.current = metricList;
  }, [dimensionList, metricList]);

  const handleSeachNode = (text: string) => {
    const filterData = dataSourceRef.current.reduce(
      (data: ISemantic.IDomainSchemaRelaList, item: ISemantic.IDomainSchemaRelaItem) => {
        const { dimensions, metrics } = item;
        const dimensionsList = dimensions.filter((dimension) => {
          return dimension.name.includes(text);
        });
        const metricsList = metrics.filter((metric) => {
          return metric.name.includes(text);
        });
        data.push({
          ...item,
          dimensions: dimensionsList,
          metrics: metricsList,
        });
        return data;
      },
      [],
    );
    const rootGraphData = changeGraphData(filterData);
    refreshGraphData(rootGraphData);
  };

  const changeGraphData = (dataSourceList: ISemantic.IDomainSchemaRelaList): TreeGraphData => {
    const relationData = formatterRelationData({
      dataSourceList,
      type: graphShowTypeRef.current,
      limit: 20,
      showDataSourceId: graphLegendDataSourceIds.current,
    });

    const graphRootData = {
      id: 'root',
      name: domainManger.selectDomainName,
      children: relationData,
    };
    return graphRootData;
  };

  const initLegendData = (graphRootData: TreeGraphData) => {
    const legendList = graphRootData?.children?.map((item: any) => {
      const { id, name } = item;
      return {
        id,
        label: name,
        order: 4,
        ...typeConfigs.datasource,
      };
    });
    legendDataRef.current = legendList as any;
  };

  const queryDataSourceList = async (params: {
    modelId: number;
    graphShowType?: SemanticNodeType;
  }) => {
    const { code, data } = await getDomainSchemaRela(params.modelId);
    if (code === 200) {
      if (data) {
        setDataSourceInfoList(
          data.map((item: ISemantic.IDomainSchemaRelaItem) => {
            return item.datasource;
          }),
        );
        const graphRootData = changeGraphData(data);
        dataSourceRef.current = data;
        initLegendData(graphRootData);
        setGraphData(graphRootData);
        return graphRootData;
      }
      return false;
    } else {
      return false;
    }
  };

  useEffect(() => {
    graphLegendDataSourceIds.current = undefined;
    graphRef.current = null;
    queryDataSourceList({ modelId });
  }, [modelId]);

  // const getLegendDataFilterFunctions = () => {
  //   legendDataRef.current.map((item: any) => {
  //     const { id } = item;
  //     legendDataFilterFunctions.current = {
  //       ...legendDataFilterFunctions.current,
  //       [id]: (d: any) => {
  //         if (d.legendType === id) {
  //           return true;
  //         }
  //         return false;
  //       },
  //     };
  //   });
  // };

  // const setAllActiveLegend = (legend: any) => {
  //   const legendCanvas = legend._cfgs.legendCanvas;
  //   if (!legendCanvas) {
  //     return;
  //   }
  //   // 从图例中找出node-group节点;
  //   const group = legendCanvas.find((e: any) => e.get('name') === 'node-group');
  //   // 数据源的图例节点在node-group中的children中；
  //   const groups = group.get('children');
  //   groups.forEach((itemGroup: any) => {
  //     const labelText = itemGroup.find((e: any) => e.get('name') === 'circle-node-text');
  //     // legend中activateLegend事件触发在图例节点的Text上，方法中存在向上溯源的逻辑：const shapeGroup = shape.get('parent');
  //     // 因此复用实例方法时，在这里不能直接将图例节点传入，需要在节点的children中找任意一个元素作为入参；
  //     legend.activateLegend(labelText);
  //   });
  // };

  const handleContextMenuClickEdit = (item: IItemBaseConfig) => {
    const targetData = item.model;
    if (!targetData) {
      return;
    }
    const datasource = loopNodeFindDataSource(item);
    if (datasource) {
      setNodeDataSource({
        ...datasource,
        id: datasource.uid,
      });
    }
    if (targetData.nodeType === SemanticNodeType.DATASOURCE) {
      setCreateDataSourceModalOpen(true);
      return;
    }
    if (targetData.nodeType === SemanticNodeType.DIMENSION) {
      const targetItem = dimensionListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setDimensionItem({ ...targetItem });
        setCreateDimensionModalVisible(true);
      } else {
        message.error('获取维度初始化数据失败');
      }
      return;
    }
    if (targetData.nodeType === SemanticNodeType.METRIC) {
      const targetItem = metricListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setMetricItem({ ...targetItem });
        setCreateMetricModalVisible(true);
      } else {
        message.error('获取指标初始化数据失败');
      }
      return;
    }
  };

  const handleContextMenuClickCreate = (item: IItemBaseConfig, key: string) => {
    const datasource = item.model;
    if (!datasource) {
      return;
    }
    setNodeDataSource({
      ...datasource,
      id: datasource.uid,
    });
    if (key === 'createDimension') {
      setCreateDimensionModalVisible(true);
    }
    if (key === 'createMetric') {
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
    if (targetData.nodeType === SemanticNodeType.DATASOURCE) {
      setCurrentNodeData({
        ...targetData,
        id: targetData.uid,
      });
      setConfirmModalOpenState(true);
      return;
    }
    if (targetData.nodeType === SemanticNodeType.DIMENSION) {
      const targetItem = dimensionListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setCurrentNodeData({ ...targetData, ...targetItem });
        setConfirmModalOpenState(true);
      } else {
        message.error('获取维度初始化数据失败');
      }
    }
    if (targetData.nodeType === SemanticNodeType.METRIC) {
      const targetItem = metricListRef.current.find((item) => item.id === targetData.uid);
      if (targetItem) {
        setCurrentNodeData({ ...targetData, ...targetItem });
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
      case 'editDatasource':
        handleContextMenuClickEdit(item._cfg);
        break;
      case 'delete':
      case 'deleteDatasource':
        handleContextMenuClickDelete(item._cfg);
        break;
      case 'createDimension':
      case 'createMetric':
        handleContextMenuClickCreate(item._cfg, key);
        break;
      default:
        break;
    }
  };

  const handleNodeTypeClick = (nodeData: any) => {
    setCurrentNodeData(nodeData);
    setInfoDrawerVisible(true);
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

  function handleToolBarClick(code: string) {
    if (code === 'visibleMode') {
      visibleModeOpenRef.current = !visibleModeOpenRef.current;
      setVisibleModeOpen(visibleModeOpenRef.current);
      return;
    }
    visibleModeOpenRef.current = false;
    setVisibleModeOpen(false);
  }

  const lessNodeZoomRealAndMoveCenter = () => {
    const bbox = graphRef.current.get('group').getBBox();

    // 计算图形的中心点
    const centerX = (bbox.minX + bbox.maxX) / 2;
    const centerY = (bbox.minY + bbox.maxY) / 2;

    // 获取画布的中心点
    const canvasWidth = graphRef.current.get('width');
    const canvasHeight = graphRef.current.get('height');
    const canvasCenterX = canvasWidth / 2;
    const canvasCenterY = canvasHeight / 2;

    // 计算画布需要移动的距离
    const dx = canvasCenterX - centerX;
    const dy = canvasCenterY - centerY;

    // 将画布移动到中心点
    graphRef.current.translate(dx, dy);

    // 将缩放比例设置为 1，以画布中心点为中心进行缩放
    graphRef.current.zoomTo(1, { x: canvasCenterX, y: canvasCenterY });
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

      // getLegendDataFilterFunctions();
      const toolbar = initToolBar({ onSearch: handleSeachNode, onClick: handleToolBarClick });
      const tooltip = initTooltips();
      const contextMenu = initContextMenu({
        onMenuClick: handleContextMenuClick,
      });
      // const legend = initLegend({
      //   nodeData: legendDataRef.current,
      //   filterFunctions: { ...legendDataFilterFunctions.current },
      // });

      graphRef.current = new G6.TreeGraph({
        container: 'semanticGraph',
        width,
        height,
        modes: {
          default: [
            // {
            //   type: 'collapse-expand',
            //   onChange: function onChange(item, collapsed) {
            //     const data = item!.get('model');
            //     data.collapsed = collapsed;
            //     return true;
            //   },
            // },
            'drag-node',
            'drag-canvas',
            // 'activate-relations',
            {
              type: 'zoom-canvas',
              sensitivity: 0.3, // 设置缩放灵敏度，值越小，缩放越不敏感，默认值为 1
            },
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
        plugins: [tooltip, toolbar, contextMenu],
        // plugins: [legend, tooltip, toolbar, contextMenu],
      });
      graphRef.current.set('initGraphData', graphData);
      graphRef.current.set('initDataSource', dataSourceRef.current);

      // const legendCanvas = legend._cfgs.legendCanvas;

      // legend模式事件方法bindEvents会有点击图例空白清空选中的逻辑，在注册click事件前，先将click事件队列清空；
      // legend._cfgs.legendCanvas._events.click = [];
      // legendCanvas.on('click', () => {
      //   // @ts-ignore findLegendItemsByState为Legend的 private方法，忽略ts校验
      //   const activedNodeList = legend.findLegendItemsByState('active');
      //   // 获取当前所有激活节点后进行数据遍历筛选；
      //   const activedNodeIds = activedNodeList.map((item: IGroup) => {
      //     return item.cfg.id;
      //   });
      //   const graphDataClone = cloneDeep(graphData);
      //   const filterGraphDataChildren = Array.isArray(graphDataClone?.children)
      //     ? graphDataClone.children.reduce((children: TreeGraphData[], item: TreeGraphData) => {
      //         if (activedNodeIds.includes(item.id)) {
      //           children.push(item);
      //         }
      //         return children;
      //       }, [])
      //     : [];
      //   graphDataClone.children = filterGraphDataChildren;
      //   refreshGraphData(graphDataClone);
      // });

      graphRef.current.node(function (node: NodeConfig) {
        return getNodeConfigByType(node, {
          label: node.name,
        });
      });
      graphRef.current.data(graphData);
      graphRef.current.render();

      const nodeCount = graphRef.current.getNodes().length;
      if (nodeCount < 10) {
        lessNodeZoomRealAndMoveCenter();
      } else {
        graphRef.current.fitView([80, 80]);
      }

      graphRef.current.on('node:click', (evt: any) => {
        const item = evt.item; // 被操作的节点 item
        const itemData = item?._cfg?.model;
        if (itemData) {
          const { nodeType } = itemData;
          if (
            [
              SemanticNodeType.DIMENSION,
              SemanticNodeType.METRIC,
              SemanticNodeType.DATASOURCE,
            ].includes(nodeType)
          ) {
            handleNodeTypeClick(itemData);
            return;
          }
        }
      });

      graphRef.current.on('canvas:click', () => {
        setInfoDrawerVisible(false);
      });

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

  const updateGraphData = async (params?: { graphShowType?: SemanticNodeType }) => {
    const graphRootData = await queryDataSourceList({
      modelId,
      graphShowType: params?.graphShowType,
    });
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
      <GraphLegend
        legendOptions={legendDataRef.current}
        defaultCheckAll={true}
        onChange={(nodeIds: string[]) => {
          graphLegendDataSourceIds.current = nodeIds;
          const rootGraphData = changeGraphData(dataSourceRef.current);
          refreshGraphData(rootGraphData);
        }}
      />
      {visibleModeOpen && (
        <GraphLegendVisibleModeItem
          value={graphShowTypeState}
          onChange={(showType) => {
            graphShowTypeRef.current = showType;
            setGraphShowTypeState(showType);
            const rootGraphData = changeGraphData(dataSourceRef.current);
            refreshGraphData(rootGraphData);
          }}
        />
      )}

      <GraphToolBar
        onClick={({ eventName }: { eventName: string }) => {
          setNodeDataSource(undefined);
          if (eventName === 'createDatabase') {
            setCreateDataSourceModalOpen(true);
          }
          if (eventName === 'createDimension') {
            setCreateDimensionModalVisible(true);
            setDimensionItem(undefined);
          }
          if (eventName === 'createMetric') {
            setCreateMetricModalVisible(true);
            setMetricItem(undefined);
          }
        }}
      />
      <div
        ref={ref}
        key={`${modelId}`}
        id="semanticGraph"
        style={{ width: '100%', height: 'calc(100vh - 175px)', position: 'relative' }}
      />
      <NodeInfoDrawer
        nodeData={currentNodeData}
        placement="right"
        onClose={() => {
          setInfoDrawerVisible(false);
        }}
        open={infoDrawerVisible}
        mask={false}
        getContainer={false}
        onEditBtnClick={(nodeData: any) => {
          handleContextMenuClickEdit({ model: nodeData });
          setInfoDrawerVisible(false);
        }}
        onNodeChange={({ eventName }: { eventName: string }) => {
          updateGraphData();
          setInfoDrawerVisible(false);
          if (eventName === SemanticNodeType.METRIC) {
            dispatch({
              type: 'domainManger/queryMetricList',
              payload: {
                modelId,
              },
            });
          }
          if (eventName === SemanticNodeType.DIMENSION) {
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
              },
            });
          }
        }}
      />

      {createDimensionModalVisible && (
        <DimensionInfoModal
          modelId={modelId}
          bindModalVisible={createDimensionModalVisible}
          dimensionItem={dimensionItem}
          dataSourceList={nodeDataSource ? [nodeDataSource] : dataSourceInfoList}
          onSubmit={() => {
            setCreateDimensionModalVisible(false);
            updateGraphData();
            dispatch({
              type: 'domainManger/queryDimensionList',
              payload: {
                modelId,
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
          domainId={selectDomainId}
          modelId={modelId}
          key={metricItem?.id}
          datasourceId={nodeDataSource?.id}
          createModalVisible={createMetricModalVisible}
          metricItem={metricItem}
          onSubmit={() => {
            setCreateMetricModalVisible(false);
            updateGraphData();
            dispatch({
              type: 'domainManger/queryMetricList',
              payload: {
                modelId,
              },
            });
          }}
          onCancel={() => {
            setCreateMetricModalVisible(false);
          }}
        />
      )}
      {
        <ClassDataSourceTypeModal
          open={createDataSourceModalOpen}
          onCancel={() => {
            setNodeDataSource(undefined);
            setCreateDataSourceModalOpen(false);
          }}
          dataSourceItem={nodeDataSource}
          onSubmit={() => {
            updateGraphData();
          }}
        />
      }
      {
        <DeleteConfirmModal
          open={confirmModalOpenState}
          onOkClick={() => {
            setConfirmModalOpenState(false);
            updateGraphData();
            graphShowTypeState === SemanticNodeType.DIMENSION
              ? dispatch({
                  type: 'domainManger/queryDimensionList',
                  payload: {
                    modelId,
                  },
                })
              : dispatch({
                  type: 'domainManger/queryMetricList',
                  payload: {
                    modelId,
                  },
                });
          }}
          onCancelClick={() => {
            setConfirmModalOpenState(false);
          }}
          nodeData={currentNodeData}
        />
      }
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManger);
