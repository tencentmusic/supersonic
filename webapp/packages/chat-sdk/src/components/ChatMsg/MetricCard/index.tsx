import { PREFIX_CLS } from '../../../common/constants';
import { formatByThousandSeperator } from '../../../utils/utils';
import ApplyAuth from '../ApplyAuth';
import { DrillDownDimensionType, MsgDataType } from '../../../common/type';
import PeriodCompareItem from './PeriodCompareItem';
import DrillDownDimensions from '../../DrillDownDimensions';
import { Spin } from 'antd';
import classNames from 'classnames';

type Props = {
  data: MsgDataType;
  drillDownDimension?: DrillDownDimensionType;
  loading: boolean;
  onSelectDimension: (dimension?: DrillDownDimensionType) => void;
  onApplyAuth?: (domain: string) => void;
};

const MetricCard: React.FC<Props> = ({
  data,
  drillDownDimension,
  loading,
  onSelectDimension,
  onApplyAuth,
}) => {
  const { queryMode, queryColumns, queryResults, entityInfo, aggregateInfo, chatContext } = data;

  const { metricInfos } = aggregateInfo || {};
  const { dateInfo } = chatContext || {};
  const { startDate, endDate } = dateInfo || {};

  const indicatorColumn = queryColumns?.find(column => column.showType === 'NUMBER');
  const indicatorColumnName = indicatorColumn?.nameEn || '';

  const prefixCls = `${PREFIX_CLS}-metric-card`;

  const indicatorClass = classNames(`${prefixCls}-indicator`, {
    [`${prefixCls}-indicator-period-compare`]: metricInfos?.length > 0,
  });

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-indicator-name`}>{indicatorColumn?.name}</div>
      <Spin spinning={loading}>
        <div className={indicatorClass}>
          <div className={`${prefixCls}-date-range`}>
            {startDate === endDate ? startDate : `${startDate} ~ ${endDate}`}
          </div>
          {indicatorColumn && !indicatorColumn?.authorized ? (
            <ApplyAuth domain={entityInfo?.domainInfo.name || ''} onApplyAuth={onApplyAuth} />
          ) : (
            <div className={`${prefixCls}-indicator-value`}>
              {formatByThousandSeperator(queryResults?.[0]?.[indicatorColumnName])}
            </div>
          )}
          {metricInfos?.length > 0 && (
            <div className={`${prefixCls}-period-compare`}>
              {Object.keys(metricInfos[0].statistics).map((key: any) => (
                <PeriodCompareItem title={key} value={metricInfos[0].statistics[key]} />
              ))}
            </div>
          )}
        </div>
      </Spin>
      {(queryMode === 'METRIC_DOMAIN' || queryMode === 'METRIC_FILTER') && (
        <div className={`${prefixCls}-drill-down-dimensions`}>
          <DrillDownDimensions
            domainId={chatContext.domainId}
            dimensionFilters={chatContext.dimensionFilters}
            drillDownDimension={drillDownDimension}
            onSelectDimension={onSelectDimension}
            isMetricCard
          />
        </div>
      )}
    </div>
  );
};

export default MetricCard;
