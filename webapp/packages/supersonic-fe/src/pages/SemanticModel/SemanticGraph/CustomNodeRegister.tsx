import G6 from '@antv/g6';

const colors = {
  B: '#5B8FF9',
  R: '#F46649',
  Y: '#EEBC20',
  G: '#5BD8A6',
  DI: '#A7A7A7',
};

// 自定义节点、边
export const flowRectNodeRegister = () => {
  /**
   * 自定义节点
   */
  G6.registerNode(
    'flow-rect',
    {
      shapeType: 'flow-rect',
      draw(cfg, group) {
        const {
          name = '',
          variableName,
          variableValue,
          variableUp,
          label,
          collapsed,
          currency,
          status,
          rate,
        } = cfg;

        const grey = '#CED4D9';
        const rectConfig = {
          width: 202,
          height: 60,
          lineWidth: 1,
          fontSize: 12,
          fill: '#fff',
          radius: 4,
          stroke: grey,
          opacity: 1,
        };

        const nodeOrigin = {
          x: -rectConfig.width / 2,
          y: -rectConfig.height / 2,
        };

        const textConfig = {
          textAlign: 'left',
          textBaseline: 'bottom',
        };

        const rect = group.addShape('rect', {
          attrs: {
            x: nodeOrigin.x,
            y: nodeOrigin.y,
            ...rectConfig,
          },
        });

        const rectBBox = rect.getBBox();

        // label title
        group.addShape('text', {
          attrs: {
            ...textConfig,
            x: 12 + nodeOrigin.x,
            y: 20 + nodeOrigin.y,
            text: name.length > 28 ? name.substr(0, 28) + '...' : name,
            fontSize: 12,
            opacity: 0.85,
            fill: '#000',
            cursor: 'pointer',
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'name-shape',
        });

        // price
        const price = group.addShape('text', {
          attrs: {
            ...textConfig,
            x: 12 + nodeOrigin.x,
            y: rectBBox.maxY - 12,
            text: label,
            fontSize: 16,
            fill: '#000',
            opacity: 0.85,
          },
        });

        // label currency
        group.addShape('text', {
          attrs: {
            ...textConfig,
            x: price.getBBox().maxX + 5,
            y: rectBBox.maxY - 12,
            text: currency,
            fontSize: 12,
            fill: '#000',
            opacity: 0.75,
          },
        });

        // percentage
        const percentText = group.addShape('text', {
          attrs: {
            ...textConfig,
            x: rectBBox.maxX - 8,
            y: rectBBox.maxY - 12,
            text: `${((variableValue || 0) * 100).toFixed(2)}%`,
            fontSize: 12,
            textAlign: 'right',
            fill: colors[status],
          },
        });

        // percentage triangle
        const symbol = variableUp ? 'triangle' : 'triangle-down';
        const triangle = group.addShape('marker', {
          attrs: {
            ...textConfig,
            x: percentText.getBBox().minX - 10,
            y: rectBBox.maxY - 12 - 6,
            symbol,
            r: 6,
            fill: colors[status],
          },
        });

        // variable name
        group.addShape('text', {
          attrs: {
            ...textConfig,
            x: triangle.getBBox().minX - 4,
            y: rectBBox.maxY - 12,
            text: variableName,
            fontSize: 12,
            textAlign: 'right',
            fill: '#000',
            opacity: 0.45,
          },
        });

        // bottom line background
        const bottomBackRect = group.addShape('rect', {
          attrs: {
            x: nodeOrigin.x,
            y: rectBBox.maxY - 4,
            width: rectConfig.width,
            height: 4,
            radius: [0, 0, rectConfig.radius, rectConfig.radius],
            fill: '#E0DFE3',
          },
        });

        // bottom percent
        const bottomRect = group.addShape('rect', {
          attrs: {
            x: nodeOrigin.x,
            y: rectBBox.maxY - 4,
            width: rate * rectBBox.width,
            height: 4,
            radius: [0, 0, 0, rectConfig.radius],
            fill: colors[status],
          },
        });

        // collapse rect
        if (cfg.children && cfg.children.length) {
          group.addShape('rect', {
            attrs: {
              x: rectConfig.width / 2 - 8,
              y: -8,
              width: 16,
              height: 16,
              stroke: 'rgba(0, 0, 0, 0.25)',
              cursor: 'pointer',
              fill: '#fff',
            },
            // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
            name: 'collapse-back',
            modelId: cfg.id,
          });

          // collpase text
          group.addShape('text', {
            attrs: {
              x: rectConfig.width / 2,
              y: -1,
              textAlign: 'center',
              textBaseline: 'middle',
              text: collapsed ? '+' : '-',
              fontSize: 16,
              cursor: 'pointer',
              fill: 'rgba(0, 0, 0, 0.25)',
            },
            // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
            name: 'collapse-text',
            modelId: cfg.id,
          });
        }

        this.drawLinkPoints(cfg, group);
        return rect;
      },
      update(cfg, item) {
        const { level, status, name } = cfg;
        const group = item.getContainer();
        let mask = group.find((ele) => ele.get('name') === 'mask-shape');
        let maskLabel = group.find((ele) => ele.get('name') === 'mask-label-shape');
        if (level === 0) {
          group.get('children').forEach((child) => {
            if (child.get('name')?.includes('collapse')) return;
            child.hide();
          });
          if (!mask) {
            mask = group.addShape('rect', {
              attrs: {
                x: -101,
                y: -30,
                width: 202,
                height: 60,
                opacity: 0,
                fill: colors[status],
              },
              // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
              name: 'mask-shape',
            });
            maskLabel = group.addShape('text', {
              attrs: {
                fill: '#fff',
                fontSize: 20,
                x: 0,
                y: 10,
                text: name.length > 28 ? name.substr(0, 16) + '...' : name,
                textAlign: 'center',
                opacity: 0,
              },
              // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
              name: 'mask-label-shape',
            });
            const collapseRect = group.find((ele) => ele.get('name') === 'collapse-back');
            const collapseText = group.find((ele) => ele.get('name') === 'collapse-text');
            collapseRect?.toFront();
            collapseText?.toFront();
          } else {
            mask.show();
            maskLabel.show();
          }
          mask.animate({ opacity: 1 }, 200);
          maskLabel.animate({ opacity: 1 }, 200);
          return mask;
        } else {
          group.get('children').forEach((child) => {
            if (child.get('name')?.includes('collapse')) return;
            child.show();
          });
          mask?.animate(
            { opacity: 0 },
            {
              duration: 200,
              callback: () => mask.hide(),
            },
          );
          maskLabel?.animate(
            { opacity: 0 },
            {
              duration: 200,
              callback: () => maskLabel.hide(),
            },
          );
        }
        this.updateLinkPoints(cfg, group);
      },
      setState(name, value, item) {
        if (name === 'collapse') {
          const group = item.getContainer();
          const collapseText = group.find((e) => e.get('name') === 'collapse-text');
          if (collapseText) {
            if (!value) {
              collapseText.attr({
                text: '-',
              });
            } else {
              collapseText.attr({
                text: '+',
              });
            }
          }
        }
      },
      getAnchorPoints() {
        return [
          [0, 0.5],
          [1, 0.5],
        ];
      },
    },
    'rect',
  );

  G6.registerEdge(
    'flow-cubic',
    {
      getControlPoints(cfg) {
        let controlPoints = cfg.controlPoints; // 指定controlPoints
        if (!controlPoints || !controlPoints.length) {
          const { startPoint, endPoint, sourceNode, targetNode } = cfg;
          const {
            x: startX,
            y: startY,
            coefficientX,
            coefficientY,
          } = sourceNode ? sourceNode.getModel() : startPoint;
          const { x: endX, y: endY } = targetNode ? targetNode.getModel() : endPoint;
          let curveStart = (endX - startX) * coefficientX;
          let curveEnd = (endY - startY) * coefficientY;
          curveStart = curveStart > 40 ? 40 : curveStart;
          curveEnd = curveEnd < -30 ? curveEnd : -30;
          controlPoints = [
            { x: startPoint.x + curveStart, y: startPoint.y },
            { x: endPoint.x + curveEnd, y: endPoint.y },
          ];
        }
        return controlPoints;
      },
      getPath(points) {
        const path = [];
        path.push(['M', points[0].x, points[0].y]);
        path.push([
          'C',
          points[1].x,
          points[1].y,
          points[2].x,
          points[2].y,
          points[3].x,
          points[3].y,
        ]);
        return path;
      },
    },
    'single-line',
  );
};

const COLLAPSE_ICON = function COLLAPSE_ICON(x, y, r) {
  return [
    ['M', x - r, y],
    ['a', r, r, 0, 1, 0, r * 2, 0],
    ['a', r, r, 0, 1, 0, -r * 2, 0],
    ['M', x - r + 4, y],
    ['L', x - r + 2 * r - 4, y],
  ];
};
const EXPAND_ICON = function EXPAND_ICON(x, y, r) {
  return [
    ['M', x - r, y],
    ['a', r, r, 0, 1, 0, r * 2, 0],
    ['a', r, r, 0, 1, 0, -r * 2, 0],
    ['M', x - r + 4, y],
    ['L', x - r + 2 * r - 4, y],
    ['M', x - r + r, y - r + 4],
    ['L', x, y + r - 4],
  ];
};

export const cardNodeRegister = (graph) => {
  const ERROR_COLOR = '#F5222D';
  const getNodeConfig = (node) => {
    if (node.nodeError) {
      return {
        basicColor: ERROR_COLOR,
        fontColor: '#FFF',
        borderColor: ERROR_COLOR,
        bgColor: '#E66A6C',
      };
    }
    let config = {
      basicColor: '#5B8FF9',
      fontColor: '#5B8FF9',
      borderColor: '#5B8FF9',
      bgColor: '#C6E5FF',
    };
    switch (node.type) {
      case 'root': {
        config = {
          basicColor: '#E3E6E8',
          fontColor: 'rgba(0,0,0,0.85)',
          borderColor: '#E3E6E8',
          bgColor: '#5b8ff9',
        };
        break;
      }
      default:
        break;
    }
    return config;
  };

  const nodeBasicMethod = {
    createNodeBox: (group, config, w, h, isRoot) => {
      /* 最外面的大矩形 */
      const container = group.addShape('rect', {
        attrs: {
          x: 0,
          y: 0,
          width: w,
          height: h,
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'big-rect-shape',
      });
      if (!isRoot) {
        /* 左边的小圆点 */
        group.addShape('circle', {
          attrs: {
            x: 3,
            y: h / 2,
            r: 6,
            fill: config.basicColor,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'left-dot-shape',
        });
      }
      /* 矩形 */
      group.addShape('rect', {
        attrs: {
          x: 3,
          y: 0,
          width: w - 19,
          height: h,
          fill: config.bgColor,
          stroke: config.borderColor,
          radius: 2,
          cursor: 'pointer',
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'rect-shape',
      });

      /* 左边的粗线 */
      group.addShape('rect', {
        attrs: {
          x: 3,
          y: 0,
          width: 3,
          height: h,
          fill: config.basicColor,
          radius: 1.5,
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'left-border-shape',
      });
      return container;
    },
    /* 生成树上的 marker */
    createNodeMarker: (group, collapsed, x, y) => {
      group.addShape('circle', {
        attrs: {
          x,
          y,
          r: 13,
          fill: 'rgba(47, 84, 235, 0.05)',
          opacity: 0,
          zIndex: -2,
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'collapse-icon-bg',
      });
      group.addShape('marker', {
        attrs: {
          x,
          y,
          r: 7,
          symbol: collapsed ? EXPAND_ICON : COLLAPSE_ICON,
          stroke: 'rgba(0,0,0,0.25)',
          fill: 'rgba(0,0,0,0)',
          lineWidth: 1,
          cursor: 'pointer',
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'collapse-icon',
      });
    },
    afterDraw: (cfg, group) => {
      /* 操作 marker 的背景色显示隐藏 */
      const icon = group.find((element) => element.get('name') === 'collapse-icon');
      if (icon) {
        const bg = group.find((element) => element.get('name') === 'collapse-icon-bg');
        icon.on('mouseenter', () => {
          bg.attr('opacity', 1);
          graph.get('canvas').draw();
        });
        icon.on('mouseleave', () => {
          bg.attr('opacity', 0);
          graph.get('canvas').draw();
        });
      }
      /* ip 显示 */
      const ipBox = group.find((element) => element.get('name') === 'ip-box');
      if (ipBox) {
        /* ip 复制的几个元素 */
        const ipLine = group.find((element) => element.get('name') === 'ip-cp-line');
        const ipBG = group.find((element) => element.get('name') === 'ip-cp-bg');
        const ipIcon = group.find((element) => element.get('name') === 'ip-cp-icon');
        const ipCPBox = group.find((element) => element.get('name') === 'ip-cp-box');

        const onMouseEnter = () => {
          ipLine.attr('opacity', 1);
          ipBG.attr('opacity', 1);
          ipIcon.attr('opacity', 1);
          graph.get('canvas').draw();
        };
        const onMouseLeave = () => {
          ipLine.attr('opacity', 0);
          ipBG.attr('opacity', 0);
          ipIcon.attr('opacity', 0);
          graph.get('canvas').draw();
        };
        ipBox.on('mouseenter', () => {
          onMouseEnter();
        });
        ipBox.on('mouseleave', () => {
          onMouseLeave();
        });
        ipCPBox.on('mouseenter', () => {
          onMouseEnter();
        });
        ipCPBox.on('mouseleave', () => {
          onMouseLeave();
        });
        ipCPBox.on('click', () => {});
      }
    },
    setState: (name, value, item) => {
      const hasOpacityClass = [
        'ip-cp-line',
        'ip-cp-bg',
        'ip-cp-icon',
        'ip-cp-box',
        'ip-box',
        'collapse-icon-bg',
      ];
      const group = item.getContainer();
      const childrens = group.get('children');
      graph.setAutoPaint(false);
      if (name === 'emptiness') {
        if (value) {
          childrens.forEach((shape) => {
            if (hasOpacityClass.indexOf(shape.get('name')) > -1) {
              return;
            }
            shape.attr('opacity', 0.4);
          });
        } else {
          childrens.forEach((shape) => {
            if (hasOpacityClass.indexOf(shape.get('name')) > -1) {
              return;
            }
            shape.attr('opacity', 1);
          });
        }
      }
      graph.setAutoPaint(true);
    },
  };

  G6.registerNode('card-node', {
    draw: (cfg, group) => {
      const config = getNodeConfig(cfg);
      const isRoot = cfg.dataType === 'root';
      const nodeError = cfg.nodeError;
      /* the biggest rect */
      const container = nodeBasicMethod.createNodeBox(group, config, 243, 64, isRoot);

      if (cfg.dataType !== 'root') {
        /* the type text */
        group.addShape('text', {
          attrs: {
            text: cfg.dataType,
            x: 3,
            y: -10,
            fontSize: 12,
            textAlign: 'left',
            textBaseline: 'middle',
            fill: 'rgba(0,0,0,0.65)',
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'type-text-shape',
        });
      }

      if (cfg.ip) {
        /* ip start */
        /* ipBox */
        const ipRect = group.addShape('rect', {
          attrs: {
            fill: nodeError ? null : '#FFF',
            stroke: nodeError ? 'rgba(255,255,255,0.65)' : null,
            radius: 2,
            cursor: 'pointer',
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-container-shape',
        });

        /* ip */
        const ipText = group.addShape('text', {
          attrs: {
            text: cfg.ip,
            x: 0,
            y: 19,
            fontSize: 12,
            textAlign: 'left',
            textBaseline: 'middle',
            fill: nodeError ? 'rgba(255,255,255,0.85)' : 'rgba(0,0,0,0.65)',
            cursor: 'pointer',
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-text-shape',
        });

        const ipBBox = ipText.getBBox();
        /* the distance from the IP to the right is 12px */
        ipText.attr({
          x: 224 - 12 - ipBBox.width,
        });
        /* ipBox */
        ipRect.attr({
          x: 224 - 12 - ipBBox.width - 4,
          y: ipBBox.minY - 5,
          width: ipBBox.width + 8,
          height: ipBBox.height + 10,
        });

        /* a transparent shape on the IP for click listener */
        group.addShape('rect', {
          attrs: {
            stroke: '',
            cursor: 'pointer',
            x: 224 - 12 - ipBBox.width - 4,
            y: ipBBox.minY - 5,
            width: ipBBox.width + 8,
            height: ipBBox.height + 10,
            fill: '#fff',
            opacity: 0,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-box',
        });

        /* copyIpLine */
        group.addShape('rect', {
          attrs: {
            x: 194,
            y: 7,
            width: 1,
            height: 24,
            fill: '#E3E6E8',
            opacity: 0,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-cp-line',
        });
        /* copyIpBG */
        group.addShape('rect', {
          attrs: {
            x: 195,
            y: 8,
            width: 22,
            height: 22,
            fill: '#FFF',
            cursor: 'pointer',
            opacity: 0,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-cp-bg',
        });
        /* copyIpIcon */
        group.addShape('image', {
          attrs: {
            x: 200,
            y: 13,
            height: 12,
            width: 10,
            img: 'https://os.alipayobjects.com/rmsportal/DFhnQEhHyPjSGYW.png',
            cursor: 'pointer',
            opacity: 0,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-cp-icon',
        });
        /* a transparent rect on the icon area for click listener */
        group.addShape('rect', {
          attrs: {
            x: 195,
            y: 8,
            width: 22,
            height: 22,
            fill: '#FFF',
            cursor: 'pointer',
            opacity: 0,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'ip-cp-box',
          tooltip: 'Copy the IP',
        });

        /* ip end */
      }

      /* name */
      group.addShape('text', {
        attrs: {
          text: cfg.name,
          x: 19,
          y: 19,
          fontSize: 14,
          fontWeight: 700,
          textAlign: 'left',
          textBaseline: 'middle',
          fill: config.fontColor,
          cursor: 'pointer',
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'name-text-shape',
      });

      group.addShape('image', {
        attrs: {
          x: 19,
          y: 19,
          height: 30,
          width: 30,
          img: '/icons/vector.svg',
          cursor: 'pointer',
          fill: '#fff',
          // opacity: 0,
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'type-cp-icon',
      });

      group.addShape('rect', {
        attrs: {
          x: 0,
          y: 0,
          width: 90,
          height: 90,
          fill: '#FFF',
          cursor: 'pointer',
          opacity: 0,
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'ip-cp-boxxx',
        tooltip: 'Copy the IP',
      });

      /* the description text */
      group.addShape('text', {
        attrs: {
          text: cfg.keyInfo,
          x: 19,
          y: 45,
          fontSize: 14,
          textAlign: 'left',
          textBaseline: 'middle',
          fill: config.fontColor,
          cursor: 'pointer',
        },
        // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
        name: 'bottom-text-shape',
      });

      if (nodeError) {
        group.addShape('text', {
          attrs: {
            x: 191,
            y: 62,
            text: '⚠️',
            fill: '#000',
            fontSize: 18,
          },
          // must be assigned in G6 3.3 and later versions. it can be any string you want, but should be unique in a custom item type
          name: 'error-text-shape',
        });
      }

      const hasChildren = cfg.children && cfg.children.length > 0;
      if (hasChildren) {
        nodeBasicMethod.createNodeMarker(group, cfg.collapsed, 236, 32);
      }
      return container;
    },
    afterDraw: nodeBasicMethod.afterDraw,
    setState: nodeBasicMethod.setState,
  });
};
