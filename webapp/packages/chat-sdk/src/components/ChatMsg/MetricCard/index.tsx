import { PREFIX_CLS } from '../../../common/constants';
import { getFormattedValue } from '../../../utils/utils';
import ApplyAuth from '../ApplyAuth';
import { MsgDataType } from '../../../common/type';

type Props = {
  data: MsgDataType;
  onApplyAuth?: (domain: string) => void;
};

const MetricCard: React.FC<Props> = ({ data, onApplyAuth }) => {
  const { queryColumns, queryResults, entityInfo } = data;

  const indicatorColumn = queryColumns?.find(column => column.showType === 'NUMBER');
  const indicatorColumnName = indicatorColumn?.nameEn || '';

  const prefixCls = `${PREFIX_CLS}-metric-card`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-indicator`}>
        {/* <div className={`${prefixCls}-date-range`}>
          {startTime === endTime ? startTime : `${startTime} ~ ${endTime}`}
        </div> */}
        {!indicatorColumn?.authorized ? (
          <ApplyAuth domain={entityInfo?.domainInfo.name || ''} onApplyAuth={onApplyAuth} />
        ) : (
          <div className={`${prefixCls}-indicator-value`}>
            {getFormattedValue(queryResults?.[0]?.[indicatorColumnName])}
          </div>
        )}
        {/* <div className={`${prefixCls}-indicator-name`}>{query}</div> */}
      </div>
    </div>
  );
};

export default MetricCard;
