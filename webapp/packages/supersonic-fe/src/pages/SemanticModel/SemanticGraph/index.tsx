import React, { useEffect, useState, useRef } from 'react';
import { connect } from 'umi';
import type { StateType } from '../model';
import type { Dispatch } from 'umi';
import { typeConfigs } from './utils';
import { message } from 'antd';
import { getDatasourceList, getDomainSchemaRela } from '../service';
import initToolBar from './components/ToolBar';
import G6 from '@antv/g6';

type Props = {
  domainId: number;
  domainManger: StateType;
  dispatch: Dispatch;
};

const DomainManger: React.FC<Props> = ({ domainManger, domainId }) => {
  const ref = useRef(null);
  const [graphData, setGraphData] = useState<any>({});
  const legendDataRef = useRef<any[]>([]);
  const graphRef = useRef<any>(null);
  const legendDataFilterFunctions = useRef<any>({});

  const { dimensionList } = domainManger;

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

  const formatterRelationData = (dataSourceList: any[]) => {
    const relationData = dataSourceList.reduce((relationList: any[], item: any) => {
      const { id, name } = item;
      const dataSourceId = `dataSource-${id}`;
      const dimensionChildrenList = dimensionList.reduce(
        (dimensionChildren: any[], dimension: any) => {
          const { id: dimensionId, name: dimensionName, datasourceId } = dimension;
          if (datasourceId === id) {
            dimensionChildren.push({
              nodeType: 'dimension',
              legendType: dataSourceId,
              id: `dimension-${dimensionId}`,
              name: dimensionName,
              style: {
                lineWidth: 2,
                fill: '#f0f7ff',
                stroke: '#a6ccff',
              },
            });
          }
          return dimensionChildren;
        },
        [],
      );
      relationList.push({
        name,
        legendType: dataSourceId,
        id: dataSourceId,
        nodeType: 'datasource',
        size: 40,
        children: [...dimensionChildrenList],
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

  const queryDataSourceList = async (params: any) => {
    getDomainSchemaRela(params.domainId);
    const { code, data, msg } = await getDatasourceList({ ...params });
    if (code === 200) {
      const relationData = formatterRelationData(data);
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
      setGraphData({
        id: 'root',
        name: domainManger.selectDomainName,
        children: relationData,
      });
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDataSourceList({ domainId });
  }, []);

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

    if (!graphRef.current) {
      getLegendDataFilterFunctions();

      const toolbar = initToolBar();
      // const toolbar = new G6.ToolBar({
      //   getContent: (graph) => {
      //     const searchIcon = document.createElement('i');
      //     searchIcon.className = 'g6-toolbar-search-icon';
      //     searchIcon.style.cssText = `
      //       display: inline-block;
      //       width: 16px;
      //       height: 16px;
      //       background-image: url(https://gw.alipayobjects.com/zos/rmsportal/wzQIcOMRTkQwFgaaDIFs.svg);
      //       background-size: 16px 16px;
      //       margin-right: 8px;
      //       cursor: pointer;
      //     `;

      //     searchIcon.addEventListener('click', () => {
      //       setVisible((prevVisible) => !prevVisible);
      //     });

      //     const ul = document.createElement('ul');
      //     ul.className = 'g6-component-toolbar';
      //     ul.appendChild(searchIcon);

      //     return ul;
      //   },
      // });

      const tooltip = new G6.Tooltip({
        offsetX: 10,
        offsetY: 10,
        fixToNode: [1, 0.5],
        // the types of items that allow the tooltip show up
        // 允许出现 tooltip 的 item 类型
        // itemTypes: ['node', 'edge'],
        itemTypes: ['node'],
        // custom the tooltip's content
        // 自定义 tooltip 内容
        getContent: (e) => {
          const outDiv = document.createElement('div');
          outDiv.style.width = 'fit-content';
          outDiv.style.height = 'fit-content';
          const model = e.item.getModel();
          if (e.item.getType() === 'node') {
            outDiv.innerHTML = `${model.name}`;
          }
          //  else {
          // const source = e.item.getSource();
          // const target = e.item.getTarget();
          // outDiv.innerHTML = `来源：${source.getModel().name}<br/>去向：${
          //   target.getModel().name
          // }`;
          // }
          return outDiv;
        },
      });
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
            position: 'bottom',
            style: {
              stroke: '#fff',
              lineWidth: 4,
            },
          },
        },

        layout: {
          type: 'dendrogram',
          direction: 'LR',
          nodeSep: 200,
          rankSep: 300,
          radial: true,
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

      graph.data(graphData);
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
    }
  }, [domainId, graphData]);

  return <div ref={ref} id="semanticGraph" style={{ width: '100%', height: '100%' }} />;
};
export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(DomainManger);
