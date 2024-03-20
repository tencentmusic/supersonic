import React from 'react';
import type { NsGraph } from '@antv/xflow';
import type { RelationCanvasModel } from '../interface';
import { Popover, Popconfirm, Tooltip } from 'antd';
import { ScissorOutlined } from '@ant-design/icons';
import _ from 'lodash';
import './Relation.less';

interface OwnProps {
  deleteRelation: Function;
}

type Props = OwnProps & NsGraph.IReactEdgeProps;

const Relation = (props: Props) => {
  const relation: RelationCanvasModel = props?.data;

  const renderRelationOperationItem = (relation: RelationCanvasModel) => {
    const sourcePropertyName = relation?.source;
    const targetPropertyName = relation?.target;
    return (
      <div className="relation-operation-item" key={relation.id}>
        <div className="relation-operation-item-content">
          <Tooltip placement="top" title={sourcePropertyName}>
            <span className="relation-property-source">{sourcePropertyName}</span>
          </Tooltip>
          (N:1)
          <Tooltip placement="top" title={targetPropertyName}>
            <span className="relation-property-target">{targetPropertyName}</span>
          </Tooltip>
        </div>
        <Popconfirm
          placement="leftTop"
          title="你确定要删除该关系吗"
          okText="确定"
          cancelText="取消"
          onConfirm={() => {
            props?.deleteRelation(relation.id);
          }}
        >
          <ScissorOutlined />
        </Popconfirm>
      </div>
    );
  };

  const renderPopoverContent = () => {
    return (
      <div className="relation-operation-container">{renderRelationOperationItem(relation)}</div>
    );
  };

  return (
    <Popover
      trigger={'hover'}
      content={renderPopoverContent()}
      overlayClassName="relation-operation-popover"
    >
      <div className="relation-count-container">{1}</div>
    </Popover>
  );
};

export default Relation;
