import React from 'react'
import {} from 'antd'
import { PlusCircleOutlined, DeleteOutlined, LinkOutlined } from '@ant-design/icons'
import { MODELS, useXFlowApp } from '@antv/xflow'
import './index.less'

interface Props {
  onAddNodeClick: () => void
  onDeleteNodeClick: () => void
  onConnectEdgeClick: () => void
}

const GraphToolbar = (props: Props) => {
  const { onAddNodeClick, onDeleteNodeClick, onConnectEdgeClick } = props
  const [selectedNodes, setSelectedNodes] = React.useState([])

  /** 监听画布中选中的节点 */
  const watchModelService = async () => {
    const appRef = useXFlowApp()
    const modelService = appRef && appRef?.modelService
    if (modelService) {
      const model = await MODELS.SELECTED_NODES.getModel(modelService)
      model.watch(async () => {
        const nodes = await MODELS.SELECTED_NODES.useValue(modelService)
        setSelectedNodes(nodes)
      })
    }
  }

  watchModelService()

  return (
    <div className="xflow-er-solution-toolbar">
      <div className="icon" onClick={() => onAddNodeClick()}>
        <span>添加节点</span>
        <PlusCircleOutlined />
      </div>
      <div className="icon" onClick={() => onConnectEdgeClick()}>
        <span>添加关系</span>
        <LinkOutlined />
      </div>
      <div
        className={`icon ${selectedNodes?.length > 0 ? '' : 'disabled'}`}
        onClick={() => onDeleteNodeClick()}
      >
        <span>删除节点</span>
        <DeleteOutlined />
      </div>
    </div>
  )
}

export default GraphToolbar
