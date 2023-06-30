import React, { useEffect, useState, useRef } from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
import type { Dispatch } from 'umi';
import { typeConfigs } from './utils';
import { message, Row, Col, Radio } from 'antd';
import { getDatasourceList, getDomainSchemaRela } from '../service';
import initToolBar from './components/ToolBar';
import initTooltips from './components/ToolTips';
import G6 from '@antv/g6';

type Props = {
  domainId: number;
  domainManger: StateType;
  dispatch: Dispatch;
};

const DomainManger: React.FC<Props> = ({ domainManger, domainId }) => {
  const ref = useRef(null);
  const [graphData, setGraphData] = useState<any>({});
  const [dataSourceListData, setDataSourceListData] = useState<any[]>([]);
  const [graphShowType, setGraphShowType] = useState<string>('dimension');
  const legendDataRef = useRef<any[]>([]);
  const graphRef = useRef<any>(null);
  const legendDataFilterFunctions = useRef<any>({});

  // const { dimensionList } = domainManger;

  const toggleNodeVisibility = (graph, node, visible) => {
    if (visible) {
      graph.showItem(node);
    } else {
      graph.hideItem(node);
    }
  };

  const toggleChildrenVisibility = (graph, node, visible) => {
    const model = node.getModel();
    if (model.children) {
      model.children.forEach((child) => {
        const childNode = graph.findById(child.id);
        toggleNodeVisibility(graph, childNode, visible);
        toggleChildrenVisibility(graph, childNode, visible);
      });
    }
  };

  const getDimensionChildren = (dimensions: any[], dataSourceId: string) => {
    const dimensionChildrenList = dimensions.reduce((dimensionChildren: any[], dimension: any) => {
      const {
        id: dimensionId,
        name: dimensionName,
        bizName,
        description,
        createdBy,
        updatedAt,
      } = dimension;
      // if (datasourceId === id) {
      dimensionChildren.push({
        nodeType: 'dimension',
        legendType: dataSourceId,
        id: `dimension-${dimensionId}`,
        name: dimensionName,
        bizName,
        description,
        createdBy,
        updatedAt,
        style: {
          lineWidth: 2,
          fill: '#f0f7ff',
          stroke: '#a6ccff',
        },
      });
      // }
      return dimensionChildren;
    }, []);
    return dimensionChildrenList;
  };

  const getMetricChildren = (metrics: any[], dataSourceId: string) => {
    const metricsChildrenList = metrics.reduce((metricsChildren: any[], dimension: any) => {
      const { id, name, bizName, description, createdBy, updatedAt } = dimension;
      metricsChildren.push({
        nodeType: 'metric',
        legendType: dataSourceId,
        id: `dimension-${id}`,
        name,
        bizName,
        description,
        createdBy,
        updatedAt,
        style: {
          lineWidth: 2,
          fill: '#f0f7ff',
          stroke: '#a6ccff',
        },
      });
      return metricsChildren;
    }, []);
    return metricsChildrenList;
  };

  const formatterRelationData = (dataSourceList: any[], type = graphShowType) => {
    const relationData = dataSourceList.reduce((relationList: any[], item: any) => {
      const { datasource, dimensions, metrics } = item;
      const { id, name } = datasource;
      const dataSourceId = `dataSource-${id}`;
      let childrenList = [];
      if (type === 'metirc') {
        childrenList = getMetricChildren(metrics, dataSourceId);
      }
      if (type === 'dimension') {
        childrenList = getDimensionChildren(dimensions, dataSourceId);
      }
      relationList.push({
        name,
        legendType: dataSourceId,
        id: dataSourceId,
        nodeType: 'datasource',
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

  const changeGraphData = (data: any, type?: string) => {
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
    setGraphData(graphRootData);
  };

  const queryDataSourceList = async (params: any) => {
    const { code, data, msg } = await getDomainSchemaRela(params.domainId);
    if (code === 200) {
      if (data) {
        changeGraphData(data);
        setDataSourceListData(data);
      }
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDataSourceList({ domainId });
  }, [domainId]);

  const getLegendDataFilterFunctions = () => {
    legendDataRef.current.map((item: any) => {
      const { id } = item;
      legendDataFilterFunctions.current = {
        ...legendDataFilterFunctions.current,
        [id]: (d) => {
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
  // const [visible, setVisible] = useState(false);
  useEffect(() => {
    if (!(Array.isArray(graphData.children) && graphData.children.length > 0)) {
      return;
    }

    const container = document.getElementById('semanticGraph');
    const width = container!.scrollWidth;
    const height = container!.scrollHeight || 500;

    // if (!graphRef.current) {
    getLegendDataFilterFunctions();

    const toolbar = initToolBar();
    const tooltip = initTooltips();
    const legend = new G6.Legend({
      // container: 'legendContainer',
      data: {
        nodes: legendDataRef.current,
      },
      align: 'center',
      layout: 'horizontal', // vertical
      position: 'bottom-right',
      vertiSep: 12,
      horiSep: 24,
      offsetY: -24,
      padding: [4, 16, 8, 16],
      containerStyle: {
        fill: '#ccc',
        lineWidth: 1,
      },
      title: '可见数据源',
      titleConfig: {
        position: 'center',
        offsetX: 0,
        offsetY: 12,
        style: {
          fontSize: 12,
          fontWeight: 500,
          fill: '#000',
        },
      },
      filter: {
        enable: true,
        multiple: true,
        trigger: 'click',
        graphActiveState: 'activeByLegend',
        graphInactiveState: 'inactiveByLegend',
        filterFunctions: {
          ...legendDataFilterFunctions.current,
        },
      },
    });
    // 我使用TreeGraph进行layout布局，采用{type: 'compactBox',direction: 'LR'}模式，如何使子节点与根节点的连线只连接到上下连接桩上

    graphRef.current = new G6.TreeGraph({
      container: 'semanticGraph',
      width,
      height,
      linkCenter: true,
      modes: {
        default: [
          {
            type: 'collapse-expand',
            onChange: function onChange(item, collapsed) {
              const data = item.get('model');
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
        type: 'cubic-horizontal',
        // type: 'flow-line',
        // type: 'polyline',
        // type: 'line',
        /* configure the bending radius and min distance to the end nodes */
        style: {
          radius: 10,
          offset: 30,
          endArrow: true,
          /* and other styles */
          // stroke: '#F6BD16',
        },
        // style: {
        //   stroke: '#A3B1BF',
        // },
      },
      layout: {
        type: 'mindmap',
        direction: 'H',
        getId: function getId(d) {
          return d.id;
        },
        getHeight: function getHeight() {
          return 16;
        },
        getWidth: function getWidth() {
          return 16;
        },
        getVGap: function getVGap() {
          return 30;
        },
        getHGap: function getHGap() {
          return 100;
        },
        // type: 'dendrogram',
        // direction: 'LR',
        // nodeSep: 200,
        // rankSep: 300,
        // radial: true,
      },
      plugins: [legend, tooltip, toolbar],
    });

    const legendCanvas = legend._cfgs.legendCanvas;

    // legend模式事件方法bindEvents会有点击图例空白清空选中的逻辑，在注册click事件前，先将click事件队列清空；
    legend._cfgs.legendCanvas._events.click = [];
    legendCanvas.on('click', (e) => {
      const shape = e.target;
      const shapeGroup = shape.get('parent');
      const shapeGroupId = shapeGroup?.cfg?.id;
      if (shapeGroupId) {
        const isActive = shapeGroup.get('active');
        const targetNode = graph.findById(shapeGroupId);
        // const model = targetNode.getModel();
        toggleNodeVisibility(graph, targetNode, isActive);
        toggleChildrenVisibility(graph, targetNode, isActive);
      }
    });

    const graph = graphRef.current;

    graph.node(function (node) {
      return {
        label: node.name,
        labelCfg: { style: { fill: '#3c3c3c' } },
      };
    });
    // graph.data(graphData);
    graph.changeData(graphData);
    graph.render();
    graph.fitView();

    setAllActiveLegend(legend);

    const rootNode = graph.findById('root');
    graph.hideItem(rootNode);
    if (typeof window !== 'undefined')
      window.onresize = () => {
        if (!graph || graph.get('destroyed')) return;
        if (!container || !container.scrollWidth || !container.scrollHeight) return;
        graph.changeSize(container.scrollWidth, container.scrollHeight);
      };
    // }
  }, [graphData]);

  return (
    <>
      <Row>
        <Col flex="auto" />
        <Col flex="100px">
          <Radio.Group
            buttonStyle="solid"
            size="small"
            value={graphShowType}
            onChange={(e) => {
              const { value } = e.target;
              setGraphShowType(value);
              changeGraphData(dataSourceListData, value);
            }}
          >
            <Radio.Button value="dimension">维度</Radio.Button>
            <Radio.Button value="metric">指标</Radio.Button>
          </Radio.Group>
        </Col>
      </Row>
      <div
        ref={ref}
        key={`${domainId}-${graphShowType}`}
        id="semanticGraph"
        style={{ width: '100%', height: '100%' }}
      />
    </>
  );
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManger);
