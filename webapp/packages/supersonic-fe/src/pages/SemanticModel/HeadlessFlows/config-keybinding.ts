import type { NsNodeCmd, NsEdgeCmd, IGraphCommandService } from '@antv/xflow'
import { createKeybindingConfig, XFlowNodeCommands, XFlowEdgeCommands, MODELS } from '@antv/xflow'
import type { Node as X6Node, Edge as X6Edge } from '@antv/x6'
import { Platform } from '@antv/x6'
import { message } from 'antd'

/** 快捷键 */
enum ShortCut {
  DELETE = 'Backspace', // 删除
  CmdDelete = 'Cmd+Delete', // Mac按住Command多选删除
  CtrlDelete = 'Ctrl+Delete', // Windows按住Ctrl多选删除
}

export const useKeybindingConfig = createKeybindingConfig((config: any) => {
  config.setKeybindingFunc((registry: any) => {
    return registry.registerKeybinding([
      {
        id: 'delete',
        keybinding: ShortCut.DELETE,
        callback: async (item: any, modelService: any, commandService: any, e: any) => {
          /** 如果是input的delete事件, 则不走删除的回调 */
          const target = e && (e?.target as HTMLElement)
          if (target && target.tagName && target.tagName.toLowerCase() === 'input') {
            return
          }
          const cells = await MODELS.SELECTED_CELLS.useValue(modelService)
          const nodes = cells?.filter((cell: any) => cell.isNode())
          const edges = cells?.filter((cell: any) => cell.isEdge())
          if (edges?.length > 0) {
            deleteEdges(commandService, edges as X6Edge[])
          }
          if (nodes?.length > 0) {
            deleteNodes(commandService, nodes as X6Node[])
          }
        },
      },
      {
        id: 'deleteAll',
        keybinding: Platform.IS_MAC ? ShortCut.CmdDelete : ShortCut.CtrlDelete,
        callback: async (item: any, modelService: any, commandService: any, e: any) => {
          const cells = await MODELS.SELECTED_CELLS.useValue(modelService)
          const nodes = cells?.filter((cell: any) => cell.isNode())
          const edges = cells?.filter((cell: any) => cell.isEdge())
          deleteEdges(commandService, edges as X6Edge[])
          deleteNodes(commandService, nodes as X6Node[])
        },
      },
    ])
  })
})

export const deleteNodes = async (commandService: IGraphCommandService, nodes: X6Node[]) => {
  const promiseList = nodes?.map(node => {
    return commandService.executeCommand(XFlowNodeCommands.DEL_NODE.id, {
      nodeConfig: {
        ...node.getData(),
      },
    } as NsNodeCmd.DelNode.IArgs)
  })
  const res = await Promise.all(promiseList)
  if (res.length > 0) {
    message.success('删除节点成功！')
  }
}

export const deleteEdges = async (commandServce: IGraphCommandService, edges: X6Edge[]) => {
  const promiseList = edges
    ?.filter(edge => edge.isEdge())
    ?.map(edge => {
      return commandServce.executeCommand(XFlowEdgeCommands.DEL_EDGE.id, {
        edgeConfig: {
          ...edge.getData(),
        },
      } as NsEdgeCmd.DelEdge.IArgs)
    })
  const res = await Promise.all(promiseList)
  if (res.length > 0) {
    message.success('删除连线成功！')
  }
}
