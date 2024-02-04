import G6 from '@antv/g6';

const initLegend = ({ nodeData, filterFunctions }) => {
  const legend = new G6.Legend({
    data: {
      nodes: nodeData,
    },
    align: 'center',
    layout: 'horizontal', // vertical
    position: 'bottom-right',
    vertiSep: 12,
    horiSep: 24,
    offsetY: -24,
    padding: [10, 50, 10, 50],
    containerStyle: {
      fill: '#a6ccff',
      lineWidth: 1,
    },
    title: '可见模型',
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
      filterFunctions,
      legendStateStyles: {
        active: {
          lineWidth: 2,
          fill: '#f0f7ff',
          stroke: '#a6ccff',
        },
      },
    },
  });

  return legend;
};
export default initLegend;
