import { Spin, Row, Col, Switch, Button, Tooltip, Dropdown, MenuProps } from 'antd';
import { CheckCircleFilled, EllipsisOutlined } from '@ant-design/icons';
import { PREFIX_CLS, MsgContentTypeEnum } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import ChatMsg from '../ChatMsg';
import WebPage from '../ChatMsg/WebPage';
import Loading from './Loading';
import React, { ReactNode, useState } from 'react';

type Props = {
  queryId?: number;
  executeLoading: boolean;
  entitySwitchLoading: boolean;
  chartIndex: number;
  executeTip?: string;
  executeItemNode?: ReactNode;
  renderCustomExecuteNode?: boolean;
  data?: MsgDataType;
  triggerResize?: boolean;
  isDeveloper?: boolean;
  isSimpleMode?: boolean;
  onClickItem?: (key: string) => void;
};

const ExecuteItem: React.FC<Props> = ({
  queryId,
  executeLoading,
  entitySwitchLoading,
  chartIndex,
  executeTip,
  executeItemNode,
  renderCustomExecuteNode,
  data,
  triggerResize,
  isDeveloper,
  isSimpleMode,
  onClickItem,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;
  const [showMsgContentTable, setShowMsgContentTable] = useState<boolean>(false);
  const [msgContentType, setMsgContentType] = useState<MsgContentTypeEnum>();

  const getNodeTip = (title: ReactNode, tip?: string) => {
    return (
      <>
        <div className={`${prefixCls}-title-bar`}>
          <CheckCircleFilled className={`${prefixCls}-step-icon`} />
          <div className={`${prefixCls}-step-title`}>
            {title}
            {!tip && <Loading />}
          </div>
        </div>
        {tip && <div className={`${prefixCls}-content-container`}>{tip}</div>}
      </>
    );
  };

  if (executeLoading) {
    return getNodeTip('数据查询中');
  }

  if (executeTip) {
    return getNodeTip(
      <>
        数据查询失败
        {data?.queryTimeCost && isDeveloper && (
          <span className={`${prefixCls}-title-tip`}>(耗时: {data.queryTimeCost}ms)</span>
        )}
      </>,
      executeTip
    );
  }

  if (!data) {
    return null;
  }

  const onClick: MenuProps['onClick'] = ({ key }) => {
    onClickItem?.(key);
  };

  return (
    <>
      <div className={`${prefixCls}-title-bar`}>
        <CheckCircleFilled className={`${prefixCls}-step-icon`} />
        <div className={`${prefixCls}-step-title`} style={{ width: '100%' }}>
          <Row style={{ width: '100%' }} align="middle" justify="center">
            <Col flex="1 1 auto">
              数据查询
              {data?.queryTimeCost && isDeveloper && (
                <span className={`${prefixCls}-title-tip`}>(耗时: {data.queryTimeCost}ms)</span>
              )}
              {[MsgContentTypeEnum.METRIC_TREND, MsgContentTypeEnum.METRIC_BAR].includes(
                msgContentType as MsgContentTypeEnum
              ) && (
                <Switch
                  style={{ marginLeft: '10px' }}
                  checkedChildren="表格"
                  unCheckedChildren="表格"
                  onChange={checked => {
                    setShowMsgContentTable(checked);
                  }}
                />
              )}
            </Col>
            <Col flex="0 1 30px">
              <Dropdown
                trigger={['click']}
                menu={{
                  onClick,
                  items: [
                    {
                      label: '导出查询结果',
                      disabled: true,
                      key: 'exportData',
                    },
                    {
                      label: '查看SQL',
                      key: 'viewSQL',
                    },
                  ],
                }}
              >
                <Tooltip title="更多操作">
                  <Button size="large" type="text" icon={<EllipsisOutlined />} />
                </Tooltip>
              </Dropdown>
            </Col>
          </Row>
        </div>
      </div>
      <div className={`${prefixCls}-content-container without-border`}>
        <Spin spinning={entitySwitchLoading}>
          {data.queryAuthorization?.message && (
            <div className={`${prefixCls}-auth-tip`}>提示：{data.queryAuthorization.message}</div>
          )}
          {renderCustomExecuteNode && executeItemNode ? (
            executeItemNode
          ) : data?.queryMode === 'WEB_PAGE' ? (
            <WebPage id={queryId!} data={data} />
          ) : (
            <ChatMsg
              isSimpleMode={isSimpleMode}
              forceShowTable={showMsgContentTable}
              queryId={queryId}
              data={data}
              chartIndex={chartIndex}
              triggerResize={triggerResize}
              onMsgContentTypeChange={type => {
                setMsgContentType(type);
              }}
            />
          )}
        </Spin>
      </div>
    </>
  );
};

export default ExecuteItem;
