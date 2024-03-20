import { createCmdConfig, DisposableCollection } from '@antv/xflow'
import { MockApi } from './service'

export const useCmdConfig = createCmdConfig(config => {
  /** 设置hook */
  config.setRegisterHookFn(hooks => {
    const list = [
      hooks.addNode.registerHook({
        name: 'addNodeHook',
        handler: async args => {
          args.createNodeService = MockApi.addNode
        },
      }),
      hooks.addEdge.registerHook({
        name: 'addEdgeHook',
        handler: async args => {
          args.createEdgeService = MockApi.addEdge
        },
      }),
    ]
    const toDispose = new DisposableCollection()
    toDispose.pushAll(list)
    return toDispose
  })
})
