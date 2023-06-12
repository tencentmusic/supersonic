/* eslint-disable @typescript-eslint/no-unused-vars */
import type { NsNodeCmd, NsEdgeCmd, IMenuOptions, NsGraph, NsGraphCmd } from '@antv/xflow';
import type { NsRenameNodeCmd } from './CmdExtensions/CmdRenameNodeModal';
import { createCtxMenuConfig, MenuItemType } from '@antv/xflow';
import { IconStore, XFlowNodeCommands, XFlowEdgeCommands, XFlowGraphCommands } from '@antv/xflow';
import { initDimensionGraphCmds } from './ConfigCmd';
import type { NsConfirmModalCmd } from './CmdExtensions/CmdConfirmModal';
import { NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE } from './ConfigModelService';
import { DeleteOutlined, EditOutlined, StopOutlined } from '@ant-design/icons';
import { CustomCommands } from './CmdExtensions/constants';
import { GraphApi } from './service';

/** menuitem 配置 */
export namespace NsMenuItemConfig {
  /** 注册菜单依赖的icon */
  IconStore.set('DeleteOutlined', DeleteOutlined);
  IconStore.set('EditOutlined', EditOutlined);
  IconStore.set('StopOutlined', StopOutlined);

  export const DELETE_EDGE: IMenuOptions = {
    id: XFlowEdgeCommands.DEL_EDGE.id,
    label: '删除边',
    iconName: 'DeleteOutlined',
    onClick: async (args) => {
      const { target, commandService, modelService } = args;
      await commandService.executeCommand<NsEdgeCmd.DelEdge.IArgs>(XFlowEdgeCommands.DEL_EDGE.id, {
        edgeConfig: target.data as NsGraph.IEdgeConfig,
      });
      // 保存数据源关联关系
      await commandService.executeCommand(CustomCommands.DATASOURCE_RELATION.id, {});
      // 保存图数据
      commandService.executeCommand<NsGraphCmd.SaveGraphData.IArgs>(
        XFlowGraphCommands.SAVE_GRAPH_DATA.id,
        { saveGraphDataService: (meta, graphData) => GraphApi.saveGraphData!(meta, graphData) },
      );
      // 关闭设置关联关系弹窗
      const modalModel = await modelService!.awaitModel(
        NS_DATA_SOURCE_RELATION_MODAL_OPEN_STATE.ID,
      );
      modalModel.setValue({ open: false });
    },
  };

  export const DELETE_NODE: IMenuOptions = {
    id: XFlowNodeCommands.DEL_NODE.id,
    label: '删除节点',
    iconName: 'DeleteOutlined',
    onClick: async ({ target, commandService }) => {
      commandService.executeCommand<NsNodeCmd.DelNode.IArgs>(XFlowNodeCommands.DEL_NODE.id, {
        nodeConfig: { id: target?.data?.id || '', targetData: target.data },
      });
    },
  };

  export const EMPTY_MENU: IMenuOptions = {
    id: 'EMPTY_MENU_ITEM',
    label: '暂无可用',
    isEnabled: false,
    iconName: 'DeleteOutlined',
  };

  export const RENAME_NODE: IMenuOptions = {
    id: CustomCommands.SHOW_RENAME_MODAL.id,
    label: '重命名',
    isVisible: true,
    iconName: 'EditOutlined',
    onClick: async ({ target, commandService }) => {
      const nodeConfig = target.data as NsGraph.INodeConfig;
      commandService.executeCommand<NsRenameNodeCmd.IArgs>(CustomCommands.SHOW_RENAME_MODAL.id, {
        nodeConfig,
        updateNodeNameService: GraphApi.renameNode,
      });
    },
  };

  export const DELETE_DATASOURCE_NODE: IMenuOptions = {
    id: CustomCommands.SHOW_RENAME_MODAL.id,
    label: '删除数据源',
    isVisible: true,
    iconName: 'EditOutlined',
    onClick: async ({ target, commandService }) => {
      const nodeConfig = {
        ...target.data,
        modalProps: {
          title: '确认删除？',
        },
      } as NsGraph.INodeConfig;
      await commandService.executeCommand<NsConfirmModalCmd.IArgs>(
        CustomCommands.SHOW_CONFIRM_MODAL.id,
        {
          nodeConfig,
          confirmModalCallBack: async () => {
            await commandService.executeCommand<NsNodeCmd.DelNode.IArgs>(
              XFlowNodeCommands.DEL_NODE.id,
              {
                nodeConfig: {
                  id: target?.data?.id || '',
                  type: 'dataSource',
                  targetData: target.data,
                },
              },
            );
            commandService.executeCommand<NsGraphCmd.SaveGraphData.IArgs>(
              XFlowGraphCommands.SAVE_GRAPH_DATA.id,
              {
                saveGraphDataService: (meta, graphData) => GraphApi.saveGraphData!(meta, graphData),
              },
            );
          },
        },
      );
    },
  };

  export const VIEW_DIMENSION: IMenuOptions = {
    id: CustomCommands.VIEW_DIMENSION.id,
    label: '查看维度',
    isVisible: true,
    iconName: 'EditOutlined',
    onClick: async (args) => {
      const { target, commandService, modelService } = args as any;
      initDimensionGraphCmds({ commandService, target });
    },
  };

  export const SEPARATOR: IMenuOptions = {
    id: 'separator',
    type: MenuItemType.Separator,
  };
}

export const useMenuConfig = createCtxMenuConfig((config) => {
  config.setMenuModelService(async (target, model, modelService, toDispose) => {
    const { type, cell } = target as any;
    switch (type) {
      /** 节点菜单 */
      case 'node':
        model.setValue({
          id: 'root',
          type: MenuItemType.Root,
          submenu: [
            // NsMenuItemConfig.VIEW_DIMENSION,
            // NsMenuItemConfig.SEPARATOR,
            // NsMenuItemConfig.DELETE_NODE,
            NsMenuItemConfig.DELETE_DATASOURCE_NODE,
            // NsMenuItemConfig.RENAME_NODE,
          ],
        });
        break;
      /** 边菜单 */
      case 'edge':
        model.setValue({
          id: 'root',
          type: MenuItemType.Root,
          submenu: [NsMenuItemConfig.DELETE_EDGE],
        });
        break;
      /** 画布菜单 */
      case 'blank':
        model.setValue({
          id: 'root',
          type: MenuItemType.Root,
          submenu: [NsMenuItemConfig.EMPTY_MENU],
        });
        break;
      /** 默认菜单 */
      default:
        model.setValue({
          id: 'root',
          type: MenuItemType.Root,
          submenu: [NsMenuItemConfig.EMPTY_MENU],
        });
        break;
    }
  });
});
