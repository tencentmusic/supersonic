export const typeConfigs = {
  datasource: {
    type: 'circle',
    size: 5,
    style: {
      fill: '#5B8FF9',
    },
  },
  dimension: {
    type: 'circle',
    size: 20,
    style: {
      fill: '#5AD8A6',
    },
  },
  metric: {
    type: 'rect',
    size: [10, 10],
    style: {
      fill: '#5D7092',
    },
  },
  // eType1: {
  //   type: 'line',
  //   style: {
  //     width: 20,
  //     stroke: '#F6BD16',
  //   },
  // },
  // eType2: {
  //   type: 'cubic',
  // },
  // eType3: {
  //   type: 'quadratic',
  //   style: {
  //     width: 25,
  //     stroke: '#6F5EF9',
  //   },
  // },
};
export const legendData = {
  nodes: [
    {
      id: 'type1',
      label: 'node-type1',
      order: 4,
      ...typeConfigs.datasource,
    },
    {
      id: 'type2',
      label: 'node-type2',
      order: 0,
      ...typeConfigs.dimension,
    },
    {
      id: 'type3',
      label: 'node-type3',
      order: 2,
      ...typeConfigs.metric,
    },
  ],
  // edges: [
  //   {
  //     id: 'eType1',
  //     label: 'edge-type1',
  //     order: 2,
  //     ...typeConfigs.eType1,
  //   },
  //   {
  //     id: 'eType2',
  //     label: 'edge-type2',
  //     ...typeConfigs.eType2,
  //   },
  //   {
  //     id: 'eType3',
  //     label: 'edge-type3',
  //     ...typeConfigs.eType3,
  //   },
  // ],
};
