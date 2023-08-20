import { Spin } from 'antd';
import { PREFIX_CLS } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import ChatMsg from '../ChatMsg';
import Tools from '../Tools';
import Text from './Text';
import Typing from './Typing';

type Props = {
  question: string;
  executeLoading: boolean;
  entitySwitchLoading: boolean;
  chartIndex: number;
  executeTip?: string;
  data?: MsgDataType;
  isMobileMode?: boolean;
  triggerResize?: boolean;
  isLastMessage?: boolean;
  onSwitchEntity: (entityId: string) => void;
  onChangeChart: () => void;
};

const ExecuteItem: React.FC<Props> = ({
  question,
  executeLoading,
  entitySwitchLoading,
  chartIndex,
  executeTip,
  data,
  isMobileMode,
  triggerResize,
  isLastMessage,
  onSwitchEntity,
  onChangeChart,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;

  if (executeLoading) {
    return <Typing />;
  }

  if (executeTip) {
    return <Text data={executeTip} />;
  }

  if (!data || data.queryMode === 'WEB_PAGE') {
    return null;
  }

  const isMetricCard =
    (data.queryMode === 'METRIC_DOMAIN' || data.queryMode === 'METRIC_FILTER') &&
    data.queryResults?.length === 1;

  return (
    <div className={`${prefixCls}-msg-content`}>
      <Spin spinning={entitySwitchLoading}>
        <ChatMsg
          question={question}
          data={data}
          chartIndex={chartIndex}
          isMobileMode={isMobileMode}
          triggerResize={triggerResize}
        />
      </Spin>
      {!isMetricCard && (
        <Tools
          data={data}
          isLastMessage={isLastMessage}
          isMobileMode={isMobileMode}
          onSwitchEntity={onSwitchEntity}
          onChangeChart={onChangeChart}
        />
      )}
    </div>
  );
};

export default ExecuteItem;
