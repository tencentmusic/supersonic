import type { NsGraph, NsNodeCmd, NsEdgeCmd } from '@antv/xflow'
import { mockEntityData, mockRelationData } from './mock'

/** mock后端接口调用 */
export namespace MockApi {
  /** 加载画布数据 */
  export const loadGraphData = async () => {
    const promise: Promise<NsGraph.IGraphData> = new Promise(resolve => {
      setTimeout(() => {
        /** 链接桩样式配置, 将具有相同行为和外观的链接桩归为同一组 */
        const portAttrs = {
          circle: {
            r: 7,
            stroke: '#31d0c6',
            strokeWidth: 2,
            fill: '#fff',
            /** 链接桩在连线交互时可以被连接上 */
            magnet: true,
          },
        }
        const nodes: NsGraph.INodeConfig[] = mockEntityData?.map(entity => {
          const nodeConfig: NsGraph.INodeConfig = {
            ...entity,
            renderKey: 'NODE1',
            ports: {
              groups: {
                top: {
                  position: 'top',
                  attrs: portAttrs,
                },
                right: {
                  position: 'right',
                  attrs: portAttrs,
                },
                bottom: {
                  position: 'bottom',
                  attrs: portAttrs,
                },
                left: {
                  position: 'left',
                  attrs: portAttrs,
                },
              },
              items: [
                { id: 'top_port', group: 'top' },
                { id: 'right_port', group: 'right' },
                { id: 'bottom_port', group: 'bottom' },
                { id: 'left_port', group: 'left' },
              ],
            },
          }
          return nodeConfig
        })
        const edges: NsGraph.IEdgeConfig[] = mockRelationData?.map(relation => {
          const edgeConfig: NsGraph.IEdgeConfig = {
            ...relation,
            renderKey: 'EDGE1',
            edgeContentWidth: 20,
            edgeContentHeight: 20,
            /** 设置连线样式 */
            attrs: {
              line: {
                stroke: '#d8d8d8',
                strokeWidth: 1,
                targetMarker: {
                  name: 'classic',
                },
              },
            },
          }
          return edgeConfig
        })
        const graphData = { nodes, edges }
        resolve(graphData)
      }, 100)
    })
    const res = await promise
    return res
  }

  /** 添加节点 */
  export const addNode: NsNodeCmd.AddNode.IArgs['createNodeService'] = async args => {
    const { nodeConfig } = args
    const promise: Promise<NsGraph.INodeConfig> = new Promise(resolve => {
      setTimeout(() => {
        resolve({
          ...nodeConfig,
        })
      }, 1000)
    })
    const res = await promise
    return res
  }
  /** 删除节点 */
  export const delNode: NsNodeCmd.DelNode.IArgs['deleteNodeService'] = async args => {
    const { nodeConfig } = args
    const promise: Promise<boolean> = new Promise(resolve => {
      setTimeout(() => {
        resolve(true)
      }, 1000)
    })
    const res = await promise
    return res
  }
  /** 添加边 */
  export const addEdge: NsEdgeCmd.AddEdge.IArgs['createEdgeService'] = async args => {
    const { edgeConfig } = args
    const promise: Promise<NsGraph.IEdgeConfig> = new Promise(resolve => {
      setTimeout(() => {
        resolve({
          ...edgeConfig,
        })
      }, 1000)
    })
    const res = await promise
    return res
  }
  /** 删除边 */
  export const delEdge: NsEdgeCmd.DelEdge.IArgs['deleteEdgeService'] = async args => {
    const { edgeConfig } = args
    const promise: Promise<boolean> = new Promise(resolve => {
      setTimeout(() => {
        resolve(true)
      }, 1000)
    })
    const res = await promise
    return res
  }
}
