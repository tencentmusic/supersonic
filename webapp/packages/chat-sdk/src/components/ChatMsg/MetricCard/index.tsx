import { PREFIX_CLS } from '../../../common/constants';
import { formatByDecimalPlaces, formatMetric, formatNumberWithCN } from '../../../utils/utils';
import ApplyAuth from '../ApplyAuth';
import { MsgDataType } from '../../../common/type';
import PeriodCompareItem from './PeriodCompareItem';
import { Spin } from 'antd';
import classNames from 'classnames';
import { SwapOutlined } from '@ant-design/icons';
import { useState } from 'react';

type Props = {
  data: MsgDataType;
  loading: boolean;
  onApplyAuth?: (model: string) => void;
};

const MetricCard: React.FC<Props> = ({ data, loading, onApplyAuth }) => {
  const { queryMode, queryColumns, queryResults, entityInfo, aggregateInfo } = data;

  const { metricInfos } = aggregateInfo || {};

  const indicatorColumn = queryColumns?.find(column => column.showType === 'NUMBER');
  const indicatorColumnName = indicatorColumn?.nameEn || '';

  const { dataFormatType, dataFormat } = indicatorColumn || {};
  const value = queryResults?.[0]?.[indicatorColumnName] || 0;

  const prefixCls = `${PREFIX_CLS}-metric-card`;

  const matricCardClass = classNames(prefixCls, {
    [`${PREFIX_CLS}-metric-card-dsl`]: queryMode === 'DSL',
  });

  const indicatorClass = classNames(`${prefixCls}-indicator`, {
    [`${prefixCls}-indicator-period-compare`]: metricInfos?.length > 0,
  });

  const [isNumber, setIsNumber] = useState(false);
  const handleNumberClick = () => {
    setIsNumber(!isNumber);
  };

  return (
    <div className={matricCardClass}>
      <div className={`${prefixCls}-top-bar`}>
        {indicatorColumn?.name ? (
          <div className={`${prefixCls}-indicator-name`}>{indicatorColumn?.name}</div>
        ) : (
          <div style={{ height: 10 }} />
        )}
      </div>
      <Spin spinning={loading}>
        <div className={indicatorClass}>
          {indicatorColumn && !indicatorColumn?.authorized ? (
            <ApplyAuth model={entityInfo?.modelInfo.name || ''} onApplyAuth={onApplyAuth} />
          ) : (
            <div style={{ display: 'flex', alignItems: 'flex-end' }}>
              <div className={`${prefixCls}-indicator-value`}>
                {dataFormatType === 'percent' || dataFormatType === 'decimal'
                  ? `${formatByDecimalPlaces(
                      dataFormat?.needMultiply100 ? +value * 100 : value,
                      dataFormat?.decimalPlaces || 2
                    )}${dataFormatType === 'percent' ? '%' : ''}`
                  : isNumber
                  ? formatMetric(value) || '-'
                  : formatNumberWithCN(+value)}
              </div>
              {!isNaN(+value) && +value >= 10000 && (
                <div className={`${prefixCls}-indicator-switch`}>
                  <SwapOutlined onClick={handleNumberClick} />
                </div>
              )}
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
    </div>
  );
};

export default MetricCard;
