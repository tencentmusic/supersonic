import { EntityInfoType, ChatContextType } from '../../../common/type';
import moment from 'moment';
import { PREFIX_CLS } from '../../../common/constants';

type Props = {
  position: 'left' | 'right';
  width?: number | string;
  height?: number | string;
  bubbleClassName?: string;
  noWaterMark?: boolean;
  chatContext?: ChatContextType;
  entityInfo?: EntityInfoType;
  tip?: string;
  aggregator?: string;
  noTime?: boolean;
  children?: React.ReactNode;
};

const Message: React.FC<Props> = ({
  position,
  width,
  height,
  children,
  bubbleClassName,
  chatContext,
  entityInfo,
  aggregator,
  noTime,
}) => {
  const { aggType, dateInfo, filters, metrics, domainName } = chatContext || {};

  const prefixCls = `${PREFIX_CLS}-message`;

  const timeSection =
    !noTime && dateInfo?.text ? (
      dateInfo.text
    ) : (
      <div>{`近${moment(dateInfo?.endDate).diff(dateInfo?.startDate, 'days') + 1}天`}</div>
    );

  const metricSection =
    metrics &&
    metrics.map((metric, index) => {
      let metricNode = <span className={`${PREFIX_CLS}-metric`}>{metric.name}</span>;
      return (
        <>
          {metricNode}
          {index < metrics.length - 1 && <span>、</span>}
        </>
      );
    });

  const aggregatorSection = aggregator !== 'tag' && aggType !== 'NONE' && aggType;

  const hasFilterSection = filters && filters.length > 0;

  const filterSection = hasFilterSection && (
    <div className={`${prefixCls}-filter-section`}>
      <div className={`${prefixCls}-field-name`}>筛选条件：</div>
      <div className={`${prefixCls}-filter-values`}>
        {filters.map(filterItem => {
          return (
            <div className={`${prefixCls}-filter-item`} key={filterItem.name}>
              {filterItem.name}：{filterItem.value}
            </div>
          );
        })}
      </div>
    </div>
  );

  const entityInfoList =
    entityInfo?.dimensions?.filter(dimension => !dimension.bizName.includes('photo')) || [];

  const hasEntityInfoSection =
    entityInfoList.length > 0 && chatContext && chatContext.dimensions?.length > 0;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-content`}>
        <div className={`${prefixCls}-body`}>
          <div
            className={`${prefixCls}-bubble${bubbleClassName ? ` ${bubbleClassName}` : ''}`}
            style={{ width, height }}
            onClick={e => {
              e.stopPropagation();
            }}
          >
            {position === 'left' && chatContext && (
              <div className={`${prefixCls}-top-bar`}>
                {domainName}
                {/* {dimensionSection} */}
                {timeSection}
                {metricSection}
                {aggregatorSection}
                {/* {tipSection} */}
              </div>
            )}
            {(hasEntityInfoSection || hasFilterSection) && (
              <div className={`${prefixCls}-info-bar`}>
                {hasEntityInfoSection && (
                  <div className={`${prefixCls}-main-entity-info`}>
                    {entityInfoList.slice(0, 3).map(dimension => {
                      return (
                        <div className={`${prefixCls}-info-item`} key={dimension.bizName}>
                          <div className={`${prefixCls}-info-name`}>{dimension.name}：</div>
                          <div className={`${prefixCls}-info-value`}>{dimension.value}</div>
                        </div>
                      );
                    })}
                  </div>
                )}
                {filterSection}
              </div>
            )}
            <div className={`${prefixCls}-children`}>{children}</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Message;
