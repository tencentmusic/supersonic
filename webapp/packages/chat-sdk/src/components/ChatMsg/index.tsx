import { isMobile } from '../../utils/utils';
import Bar from './Bar';
import Message from './Message';
import MetricCard from './MetricCard';
import MetricTrend from './MetricTrend';
import Table from './Table';
import { MsgDataType } from '../../common/type';

type Props = {
  data: MsgDataType;
  onCheckMetricInfo?: (data: any) => void;
};

const ChatMsg: React.FC<Props> = ({ data, onCheckMetricInfo }) => {
  const { aggregateType, queryColumns, queryResults, chatContext, entityInfo } = data;

  if (!queryColumns || !queryResults) {
    return null;
  }

  const singleData = queryResults.length === 1;
  const dateField = queryColumns.find(item => item.showType === 'DATE' || item.type === 'DATE');
  const categoryField = queryColumns.filter(item => item.showType === 'CATEGORY');
  const metricFields = queryColumns.filter(item => item.showType === 'NUMBER');

  const getMsgContent = () => {
    if (categoryField.length > 1 || aggregateType === 'tag') {
      return <Table data={data} />;
    }
    if (dateField && metricFields.length > 0) {
      return <MetricTrend data={data} onCheckMetricInfo={onCheckMetricInfo} />;
    }
    if (singleData) {
      return <MetricCard data={data} />;
    }
    return <Bar data={data} />;
  };

  let width = '100%';
  if ((categoryField.length > 1 || aggregateType === 'tag') && !isMobile) {
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
      aggregator={aggregateType}
      tip={''}
      width={width}
    >
      {getMsgContent()}
    </Message>
  );
};

export default ChatMsg;
