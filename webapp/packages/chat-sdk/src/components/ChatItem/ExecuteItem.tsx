import { Spin, Switch, Button } from 'antd';
import { CheckCircleFilled, DownloadOutlined } from '@ant-design/icons';
import { PREFIX_CLS, MsgContentTypeEnum } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import ChatMsg from '../ChatMsg';
import WebPage from '../ChatMsg/WebPage';
import Loading from './Loading';
import React, { ReactNode, useState } from 'react';
import { exportCsvFile } from '../../utils/utils';

type Props = {
  queryId?: number;
  question: string;
  queryMode?: string;
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
};

const ExecuteItem: React.FC<Props> = ({
  queryId,
  question,
  queryMode,
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
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;
  const [showMsgContentTable, setShowMsgContentTable] = useState<boolean>(false);
  const [msgContentType, setMsgContentType] = useState<MsgContentTypeEnum>();

  const titlePrefix = queryMode === 'PLAIN_TEXT' || queryMode === 'WEB_SERVICE' ? '问答' : '数据';

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

  const onExportData = () => {
    const { queryColumns, queryResults } = data || {};
    if (!!queryResults) {
      const exportData = queryResults.map(item => {
        return Object.keys(item).reduce((result, key) => {
          const columnName = queryColumns?.find(column => column.nameEn === key)?.name || key;
          result[columnName] = item[key];
          return result;
        }, {});
      });
      exportCsvFile(exportData);
    }
  };

  if (executeLoading) {
    return getNodeTip(`${titlePrefix}查询中`);
  }

  if (executeTip) {
    return getNodeTip(
      <>
        {titlePrefix}查询失败
        {!!data?.queryTimeCost && isDeveloper && (
          <span className={`${prefixCls}-title-tip`}>(耗时: {data.queryTimeCost}ms)</span>
        )}
      </>,
      executeTip
    );
  }

  if (!data) {
    return null;
  }

  return (
    <>
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
            {!!data?.queryColumns?.length && (
              <Button className={`${prefixCls}-export-data`} size="small" onClick={onExportData}>
                <DownloadOutlined />
                导出数据
              </Button>
            )}
          </div>
        </div>
      </div>
      <div
        className={`${prefixCls}-content-container`}
        style={{ borderLeft: queryMode === 'PLAIN_TEXT' ? 'none' : undefined }}
      >
        <Spin spinning={entitySwitchLoading}>
          {data.queryAuthorization?.message && (
            <div className={`${prefixCls}-auth-tip`}>提示：{data.queryAuthorization.message}</div>
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
