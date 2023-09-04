import { PREFIX_CLS } from '../../../common/constants';
import { formatByDecimalPlaces, formatMetric, formatNumberWithCN } from '../../../utils/utils';
import ApplyAuth from '../ApplyAuth';
import { DrillDownDimensionType, MsgDataType } from '../../../common/type';
import PeriodCompareItem from './PeriodCompareItem';
import DrillDownDimensions from '../../DrillDownDimensions';
import { Spin } from 'antd';
import classNames from 'classnames';
import { SwapOutlined } from '@ant-design/icons';
import { useState } from 'react';

type Props = {
  data: MsgDataType;
  drillDownDimension?: DrillDownDimensionType;
  loading: boolean;
  onSelectDimension: (dimension?: DrillDownDimensionType) => void;
  onApplyAuth?: (model: string) => void;
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
        {drillDownDimension && (
          <div className={`${prefixCls}-filter-section-wrapper`}>
            (
            <div className={`${prefixCls}-filter-section`}>
              {drillDownDimension && (
                <div className={`${prefixCls}-filter-item`}>
                  <div className={`${prefixCls}-filter-item-label`}>下钻维度：</div>
                  <div className={`${prefixCls}-filter-item-value`}>{drillDownDimension.name}</div>
                </div>
              )}
            </div>
            )
          </div>
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
      {queryMode.includes('METRIC') && (
        <div className={`${prefixCls}-drill-down-dimensions`}>
          <DrillDownDimensions
            modelId={chatContext?.modelId}
            dimensionFilters={chatContext?.dimensionFilters}
            drillDownDimension={drillDownDimension}
            onSelectDimension={onSelectDimension}
          />
        </div>
      )}
    </div>
  );
};

export default MetricCard;
