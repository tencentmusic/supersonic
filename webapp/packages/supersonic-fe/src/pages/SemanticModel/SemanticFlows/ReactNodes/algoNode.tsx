import React from 'react';
import {
  DatabaseOutlined,
  RedoOutlined,
  CloseCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import type { NsGraph } from '@antv/xflow';
import { NsGraphStatusCommand } from '@antv/xflow';
import { Tooltip } from 'antd';
import moment from 'moment';
import './algoNode.less';

const fontStyle = { fontSize: '16px', color: '#3057e3' };
interface IProps {
  status: NsGraphStatusCommand.StatusEnum;
  hide: boolean;
}
export const AlgoIcon: React.FC<IProps> = (props) => {
  if (props.hide) {
    return null;
  }
  switch (props.status) {
    case NsGraphStatusCommand.StatusEnum.PROCESSING:
      return <RedoOutlined spin style={{ color: '#c1cdf7', fontSize: '16px' }} />;
    case NsGraphStatusCommand.StatusEnum.ERROR:
      return <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: '16px' }} />;
    case NsGraphStatusCommand.StatusEnum.SUCCESS:
      return <CheckCircleOutlined style={{ color: '#39ca74cc', fontSize: '16px' }} />;
    case NsGraphStatusCommand.StatusEnum.WARNING:
      return <ExclamationCircleOutlined style={{ color: '#faad14', fontSize: '16px' }} />;
    case NsGraphStatusCommand.StatusEnum.DEFAULT:
      return <InfoCircleOutlined style={{ color: '#d9d9d9', fontSize: '16px' }} />;
    default:
      return null;
  }
};

export const AlgoNode: NsGraph.INodeRender = (props) => {
  const { data } = props;
  const dataSourceData = data.payload;

  const openState = dataSourceData ? undefined : false;
  let tooltipNode = <></>;
  if (dataSourceData) {
    const { name, id, bizName, description, createdBy, updatedAt } = dataSourceData;
    const labelList = [
      {
        label: '数据源ID',
        value: id,
      },
      {
        label: '名称',
        value: name,
      },
      {
        label: '英文名',
        value: bizName,
      },
      {
        label: '创建人',
        value: createdBy,
      },
      {
        label: '更新时间',
        value: updatedAt ? moment(updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-',
      },
      {
        label: '描述',
        value: description,
      },
    ];
    tooltipNode = (
      <div className="dataSourceTooltip">
        {labelList.map(({ label, value }) => {
          return (
            <p key={value}>
              <span className="dataSourceTooltipLabel">{label}:</span>
              <span className="dataSourceTooltipValue">{value || '-'}</span>
            </p>
          );
        })}
      </div>
    );
  }

  return (
    <div className={`xflow-algo-node ${props.isNodeTreePanel ? 'panel-node' : ''}`}>
      <span className="icon">
        <DatabaseOutlined style={fontStyle} />
      </span>

      <span className="label">
        <Tooltip
          open={openState}
          title={tooltipNode}
          placement="right"
          color="#fff"
          overlayClassName="dataSourceTooltipWrapper"
        >
          {props.data.label}
        </Tooltip>
      </span>

      <span className="status">
        <AlgoIcon status={props.data && props.data.status} hide={props.isNodeTreePanel} />
      </span>
    </div>
  );
};
