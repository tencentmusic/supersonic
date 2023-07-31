import { PREFIX_CLS } from '../../../common/constants';
import { formatByThousandSeperator } from '../../../utils/utils';
import { AggregateInfoType } from '../../../common/type';
import PeriodCompareItem from '../MetricCard/PeriodCompareItem';

type Props = {
  aggregateInfo: AggregateInfoType;
};

const MetricInfo: React.FC<Props> = ({ aggregateInfo }) => {
  const { metricInfos } = aggregateInfo || {};
  const metricInfo = metricInfos?.[0] || {};
  const { date, value, statistics } = metricInfo || {};

  const prefixCls = `${PREFIX_CLS}-metric-info`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-indicator`}>
        <div className={`${prefixCls}-date`}>{date}</div>
        <div className={`${prefixCls}-indicator-value`}>{formatByThousandSeperator(value)}</div>
        {metricInfos?.length > 0 && (
          <div className={`${prefixCls}-period-compare`}>
            {Object.keys(statistics).map((key: any) => (
              <PeriodCompareItem title={key} value={metricInfos[0].statistics[key]} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default MetricInfo;
