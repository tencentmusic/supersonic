import { PlusSquareOutlined, MinusSquareOutlined } from '@ant-design/icons';
import type { NsGraph } from '@antv/xflow';
import { useXFlowApp, XFlowGroupCommands } from '@antv/xflow';
import './group.less';

export const GroupNode: NsGraph.INodeRender = (props) => {
  const { cell } = props;
  const app = useXFlowApp();
  const isCollapsed = props.data.isCollapsed || false;
  const onExpand = () => {
    app.executeCommand(XFlowGroupCommands.COLLAPSE_GROUP.id, {
      nodeId: cell.id,
      isCollapsed: false,
      collapsedSize: { width: 200, height: 40 },
    });
  };
  const onCollapse = () => {
    app.executeCommand(XFlowGroupCommands.COLLAPSE_GROUP.id, {
      nodeId: cell.id,
      isCollapsed: true,
      collapsedSize: { width: 200, height: 40 },
      gap: 3,
    });
  };

  return (
    <div className="xflow-group-node">
      <div className="xflow-group-header">
        <div className="header-left">{props.data.label}</div>
        <div className="header-right">
          {isCollapsed && <PlusSquareOutlined onClick={onExpand} />}
          {!isCollapsed && <MinusSquareOutlined onClick={onCollapse} />}
        </div>
      </div>
    </div>
  );
};
