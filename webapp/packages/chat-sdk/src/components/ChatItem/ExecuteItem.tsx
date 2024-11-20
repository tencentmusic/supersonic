import { Space, Spin, Switch, Tooltip, message } from 'antd';
import { CheckCircleFilled, InfoCircleOutlined } from '@ant-design/icons';
import { PREFIX_CLS, MsgContentTypeEnum } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import ChatMsg from '../ChatMsg';
import WebPage from '../ChatMsg/WebPage';
import Loading from './Loading';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { solarizedlight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import React, { ReactNode, useState } from 'react';

type Props = {
  queryId?: number;
  question: string;
  queryMode?: string;
  executeLoading: boolean;
  entitySwitchLoading?: boolean;
  chartIndex: number;
  executeTip?: string;
  executeErrorMsg?: string;
  executeItemNode?: ReactNode;
  renderCustomExecuteNode?: boolean;
  data?: MsgDataType;
  triggerResize?: boolean;
  isDeveloper?: boolean;
  isSimpleMode?: boolean;
};

const ExecuteItem: React.FC<Props> = ({
  queryId,
  question,
  queryMode,
  executeLoading,
  entitySwitchLoading = false,
  chartIndex,
  executeTip,
  executeErrorMsg,
  executeItemNode,
  renderCustomExecuteNode,
  data,
  triggerResize,
  isDeveloper,
  isSimpleMode,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;
  const [showMsgContentTable, setShowMsgContentTable] = useState<boolean>(false);
  const [msgContentType, setMsgContentType] = useState<MsgContentTypeEnum>();
  const [showErrMsg, setShowErrMsg] = useState<boolean>(false);
  const titlePrefix = queryMode === 'PLAIN_TEXT' || queryMode === 'WEB_SERVICE' ? '问答' : '数据';

  const getNodeTip = (title: ReactNode, tip?: string | ReactNode) => {
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
    return getNodeTip(`${titlePrefix}查询中`);
  }

  const handleCopy = (_: string, result: any) => {
    result ? message.success('复制SQL成功', 1) : message.error('复制SQL失败', 1);
  };

  if (executeTip) {
    return getNodeTip(
      <>
        <span>{titlePrefix}查询失败</span>
        {executeErrorMsg && (
          <Space>
            <InfoCircleOutlined style={{ marginLeft: 5, color: 'red' }} />
            <a
              onClick={() => {
                setShowErrMsg(!showErrMsg);
              }}
            >
              {!showErrMsg ? '查看' : '收起'}
            </a>
          </Space>
        )}
        {!!data?.queryTimeCost && isDeveloper && (
          <span className={`${prefixCls}-title-tip`}>(耗时: {data.queryTimeCost}ms)</span>
        )}
      </>,

      <>
        {showErrMsg && (
          <SyntaxHighlighter className={`${prefixCls}-code`} language="sql" style={solarizedlight}>
            {executeErrorMsg}
          </SyntaxHighlighter>
        )}
      </>
    );
  }

  if (!data) {
    return null;
  }

  return (
    <>
      {!isSimpleMode && (
        <div className={`${prefixCls}-title-bar`}>
          <CheckCircleFilled className={`${prefixCls}-step-icon`} />
          <div
            className={`${prefixCls}-step-title ${prefixCls}-execute-title-bar`}
            style={{ width: '100%' }}
          >
            <div>
              {titlePrefix}查询
              {!!data?.queryTimeCost && isDeveloper && (
                <span className={`${prefixCls}-title-tip`}>(耗时: {data.queryTimeCost}ms)</span>
              )}
            </div>
            <div>
              {[MsgContentTypeEnum.METRIC_TREND, MsgContentTypeEnum.METRIC_BAR].includes(
                msgContentType as MsgContentTypeEnum
              ) && (
                <Switch
                  checkedChildren="表格"
                  unCheckedChildren="表格"
                  onChange={checked => {
                    setShowMsgContentTable(checked);
                  }}
                />
              )}
            </div>
          </div>
        </div>
      )}

      <div
        className={`${prefixCls}-content-container ${
          isSimpleMode ? `${prefixCls}-content-container-simple` : ''
        }`}
        style={{ borderLeft: queryMode === 'PLAIN_TEXT' ? 'none' : undefined }}
      >
        <Spin spinning={entitySwitchLoading}>
          {data.queryAuthorization?.message && (
            <div className={`${prefixCls}-auth-tip`}>提示：{data.queryAuthorization.message}</div>
          )}
          {data.textSummary && (
            <p className={`${prefixCls}-step-title`}>
              <span style={{ marginRight: 5 }}>总结:</span>
              {data.textSummary}
            </p>
          )}

          {renderCustomExecuteNode && executeItemNode ? (
            executeItemNode
          ) : data?.queryMode === 'PLAIN_TEXT' || data?.queryMode === 'WEB_SERVICE' ? (
            data?.textResult
          ) : data?.queryMode === 'WEB_PAGE' ? (
            <WebPage id={queryId!} data={data} />
          ) : (
            <ChatMsg
              isSimpleMode={isSimpleMode}
              forceShowTable={showMsgContentTable}
              queryId={queryId}
              question={question}
              data={data}
              chartIndex={chartIndex}
              triggerResize={triggerResize}
              onMsgContentTypeChange={setMsgContentType}
            />
          )}
        </Spin>
      </div>
    </>
  );
};

export default ExecuteItem;
