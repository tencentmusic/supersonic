import { createCmdConfig, DisposableCollection } from '@antv/xflow'
import { MockApi } from './service'

export const useCmdConfig = createCmdConfig((config: any) => {
  /** 设置hook */
  config.setRegisterHookFn((hooks: any) => {
    const list = [
      hooks.addNode.registerHook({
        name: 'addNodeHook',
        handler: async (args: any) => {
          args.createNodeService = MockApi.addNode
        },
      }),
      hooks.addEdge.registerHook({
        name: 'addEdgeHook',
        handler: async (args: any) => {
          args.createEdgeService = MockApi.addEdge
        },
      }),
    ]
    const toDispose = new DisposableCollection()
    toDispose.pushAll(list)
    return toDispose
  })
})
