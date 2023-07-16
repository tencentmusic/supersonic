import { isMobile } from '../../utils/utils';
import Bar from './Bar';
import Message from './Message';
import MetricCard from './MetricCard';
import MetricTrend from './MetricTrend';
import Table from './Table';
import { MsgDataType } from '../../common/type';

type Props = {
  question: string;
  followQuestions?: string[];
  data: MsgDataType;
  isMobileMode?: boolean;
  triggerResize?: boolean;
  onCheckMetricInfo?: (data: any) => void;
};

const ChatMsg: React.FC<Props> = ({
  question,
  followQuestions,
  data,
  isMobileMode,
  triggerResize,
  onCheckMetricInfo,
}) => {
  const { queryColumns, queryResults, chatContext, entityInfo, queryMode } = data;

  if (!queryColumns || !queryResults) {
    return null;
  }

  const singleData = queryResults.length === 1;
  const dateField = queryColumns.find(item => item.showType === 'DATE' || item.type === 'DATE');
  const categoryField = queryColumns.filter(item => item.showType === 'CATEGORY');
  const metricFields = queryColumns.filter(item => item.showType === 'NUMBER');

  const getMsgContent = () => {
    if (
      categoryField.length > 1 ||
      queryMode === 'ENTITY_DETAIL' ||
      queryMode === 'ENTITY_DIMENSION' ||
      (categoryField.length === 1 && metricFields.length === 0)
    ) {
      return <Table data={data} />;
    }
    if (dateField && metricFields.length > 0) {
      return (
        <MetricTrend
          data={data}
          triggerResize={triggerResize}
          onCheckMetricInfo={onCheckMetricInfo}
        />
      );
    }
    if (singleData) {
      return <MetricCard data={data} />;
    }
    return <Bar data={data} triggerResize={triggerResize} />;
  };

  let width = '100%';
  if (categoryField.length > 1 && !isMobile && !isMobileMode) {
    if (queryColumns.length === 1) {
      width = '600px';
    } else if (queryColumns.length === 2) {
      width = '1000px';
    }
  }

  return (
    <Message
      position="left"
      chatContext={chatContext}
      entityInfo={entityInfo}
      title={question}
      followQuestions={followQuestions}
      isMobileMode={isMobileMode}
      width={width}
    >
      {getMsgContent()}
    </Message>
  );
};

export default ChatMsg;
