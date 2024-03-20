import { createGraphConfig } from '@antv/xflow'

export const useGraphConfig = createGraphConfig(config => {
  /** 预设XFlow画布配置项 */
  config.setX6Config({
    grid: true,
    scroller: {
      enabled: true,
    },
    scaling: {
      min: 0.2,
      max: 3,
    },
    connecting: {
      /** 连线过程中距离目标节点50px时自动吸附 */
      snap: {
        radius: 50,
      },
      connector: {
        name: 'rounded',
        args: {
          radius: 50,
        },
      },
      router: {
        name: 'er',
      },
      /** 不允许连接到画布空白位置, 即没有目标节点时连线会自动消失 */
      allowBlank: false,
    },
  })
})
