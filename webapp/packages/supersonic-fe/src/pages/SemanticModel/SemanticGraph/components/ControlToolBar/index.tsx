import React from 'react';
import {
  ControlOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  SearchOutlined,
  OneToOneOutlined,
} from '@ant-design/icons';
import { FloatButton, Tooltip, Input } from 'antd';
import GraphLegendVisibleModeItem from '../GraphLegendVisibleModeItem';
import { SemanticNodeType } from '../../../enum';
import styles from './style.less';

const { Search } = Input;

type Props = {
  graph: any;
  onSearch?: (text: string) => void;
  onShowTypeChange?: (nodeType: SemanticNodeType) => void;
  onZoomIn?: () => void;
  onZoomOut?: () => void;
  onAutoZoom?: () => void;
};

function zoomGraph(graph, ratio: number) {
  const width = graph.get('width');
  const height = graph.get('height');
  const centerX = width / 2;
  const centerY = height / 2;
  graph.zoom(ratio, { x: centerX, y: centerY });
}

const ControlToolBar: React.FC<Props> = ({
  graph,
  onSearch,
  onShowTypeChange,
  onZoomIn,
  onZoomOut,
  onAutoZoom,
}) => {
  const sensitivity = 0.1; // 设置缩放灵敏度，值越小，缩放越不敏感，默认值为 1
  const zoomOutRatio = 1 - sensitivity;
  const zoomInRatio = 1 + sensitivity;

  return (
    <div className={styles.graphControlContent}>
      <FloatButton.Group
        shape="square"
        style={{ left: 25, top: 25, height: 310, position: 'absolute' }}
      >
        <Tooltip
          overlayClassName={styles.overlayClassName}
          title={
            <Search
              placeholder="请输入指标/维度名称"
              allowClear
              onSearch={(text: string) => {
                onSearch?.(text);
              }}
              style={{ width: 250 }}
            />
          }
          placement="right"
        >
          <FloatButton icon={<SearchOutlined />} description="搜索" />
        </Tooltip>

        <Tooltip
          overlayClassName={styles.overlayClassName}
          title={
            <GraphLegendVisibleModeItem
              onChange={(nodeType: SemanticNodeType) => {
                onShowTypeChange?.(nodeType);
              }}
            />
          }
          placement="right"
        >
          <FloatButton icon={<ControlOutlined />} description="模式" />
        </Tooltip>

        <FloatButton
          icon={<ZoomInOutlined />}
          description="放大"
          onClick={() => {
            zoomGraph(graph, zoomInRatio);
            onZoomIn?.();
          }}
        />
        <FloatButton
          icon={<ZoomOutOutlined />}
          description="缩小"
          onClick={() => {
            zoomGraph(graph, zoomOutRatio);
            onZoomOut?.();
          }}
        />
        <FloatButton
          icon={<OneToOneOutlined />}
          description="重置"
          onClick={() => {
            onAutoZoom?.();
          }}
        />
      </FloatButton.Group>
    </div>
  );
};

export default ControlToolBar;
