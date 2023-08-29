import { Spin } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';
import { PREFIX_CLS } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import ChatMsg from '../ChatMsg';
import WebPage from '../ChatMsg/WebPage';
import Loading from './Loading';

type Props = {
  question: string;
  queryId?: number;
  executeLoading: boolean;
  entitySwitchLoading: boolean;
  chartIndex: number;
  executeTip?: string;
  data?: MsgDataType;
  isMobileMode?: boolean;
  triggerResize?: boolean;
  onChangeChart: () => void;
};

const ExecuteItem: React.FC<Props> = ({
  question,
  queryId,
  executeLoading,
  entitySwitchLoading,
  chartIndex,
  executeTip,
  data,
  isMobileMode,
  triggerResize,
  onChangeChart,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;

  const getNodeTip = (title: string, tip?: string) => {
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
    return getNodeTip('数据查询失败', executeTip);
  }

  if (!data) {
    return null;
  }

  return (
    <>
      <div className={`${prefixCls}-title-bar`}>
        <CheckCircleFilled className={`${prefixCls}-step-icon`} />
        <div className={`${prefixCls}-step-title`}>数据查询结果</div>
      </div>
      <div className={`${prefixCls}-content-container ${prefixCls}-last-node`}>
        <Spin spinning={entitySwitchLoading}>
          {data?.queryMode === 'WEB_PAGE' ? (
            <WebPage id={queryId!} data={data} />
          ) : (
            <ChatMsg
              question={question}
              data={data}
              chartIndex={chartIndex}
              isMobileMode={isMobileMode}
              triggerResize={triggerResize}
            />
          )}
        </Spin>
      </div>
    </>
  );
};

export default ExecuteItem;
