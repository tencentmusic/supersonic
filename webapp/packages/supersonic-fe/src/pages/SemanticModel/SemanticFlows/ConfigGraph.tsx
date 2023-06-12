import type { IProps } from './index';
import { NsGraph } from '@antv/xflow';
import type { Graph } from '@antv/x6';
import { createHookConfig, DisposableCollection } from '@antv/xflow';
import { DATASOURCE_NODE_RENDER_ID, GROUP_NODE_RENDER_ID } from './constant';
import { AlgoNode } from './ReactNodes/algoNode';
import { GroupNode } from './ReactNodes/group';

export const useGraphHookConfig = createHookConfig<IProps>((config) => {
  // 获取 Props
  // const props = proxy.getValue();
  config.setRegisterHook((hooks) => {
    const disposableList = [
      // 注册增加 react Node Render
      hooks.reactNodeRender.registerHook({
        name: 'add react node',
        handler: async (renderMap) => {
          renderMap.set(DATASOURCE_NODE_RENDER_ID, AlgoNode);
          renderMap.set(GROUP_NODE_RENDER_ID, GroupNode);
        },
      }),
      // 注册修改graphOptions配置的钩子
      hooks.graphOptions.registerHook({
        name: 'custom-x6-options',
        after: 'dag-extension-x6-options',
        handler: async (options) => {
          const graphOptions: Graph.Options = {
            connecting: {
              allowLoop: false,
              // 是否触发交互事件
              validateMagnet() {
                // return magnet.getAttribute('port-group') !== NsGraph.AnchorGroup.TOP
                return true;
              },
              // 显示可用的链接桩
              validateConnection(args: any) {
                const { sourceView, targetView, sourceMagnet, targetMagnet } = args;

                // 不允许连接到自己
                if (sourceView === targetView) {
                  return false;
                }
                // 没有起点的返回false
                if (!sourceMagnet) {
                  return false;
                }
                if (!targetMagnet) {
                  return false;
                }
                // 只能从上游节点的输出链接桩创建连接
                if (sourceMagnet?.getAttribute('port-group') === NsGraph.AnchorGroup.LEFT) {
                  return false;
                }
                // 只能连接到下游节点的输入桩
                if (targetMagnet?.getAttribute('port-group') === NsGraph.AnchorGroup.RIGHT) {
                  return false;
                }
                const node = targetView!.cell as any;

                // 判断目标链接桩是否可连接
                const portId = targetMagnet.getAttribute('port')!;
                const port = node.getPort(portId);
                return !!port;
              },
            },
          };
          options.connecting = { ...options.connecting, ...graphOptions.connecting };
        },
      }),
      // hooks.afterGraphInit.registerHook({
      //   name: '注册toolTips工具',
      //   handler: async (args) => {},
      // }),
    ];
    const toDispose = new DisposableCollection();
    toDispose.pushAll(disposableList);
    return toDispose;
  });
});
