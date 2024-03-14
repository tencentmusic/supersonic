import { EntityInfoType, ChatContextType } from '../../../common/type';
import { PREFIX_CLS } from '../../../common/constants';

type Props = {
  position: 'left' | 'right';
  width?: number | string;
  maxWidth?: number | string;
  height?: number | string;
  title?: string;
  followQuestions?: string[];
  bubbleClassName?: string;
  chatContext?: ChatContextType;
  entityInfo?: EntityInfoType;
  children?: React.ReactNode;
  isMobileMode?: boolean;
  queryMode?: string;
};

const Message: React.FC<Props> = ({
  width,
  maxWidth,
  height,
  children,
  bubbleClassName,
  entityInfo,
  queryMode,
  chatContext,
}) => {
  const prefixCls = `${PREFIX_CLS}-message`;

  const { modelName, dateInfo, dimensionFilters } = chatContext || {};
  const { startDate, endDate } = dateInfo || {};

  const entityInfoList =
    entityInfo?.dimensions?.filter(dimension => !dimension.bizName.includes('photo')) || [];

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-content`}>
        <div className={`${prefixCls}-body`}>
          <div
            className={`${prefixCls}-bubble${bubbleClassName ? ` ${bubbleClassName}` : ''}`}
            style={{ width, height, maxWidth }}
            onClick={e => {
              e.stopPropagation();
            }}
          >
            {(queryMode === 'METRIC_ID' || queryMode === 'TAG_DETAIL') &&
              entityInfoList.length > 0 && (
                <div className={`${prefixCls}-info-bar`}>
                  <div className={`${prefixCls}-main-entity-info`}>
                    {entityInfoList.slice(0, 4).map(dimension => {
                      return (
                        <div className={`${prefixCls}-info-item`} key={dimension.bizName}>
                          <div className={`${prefixCls}-info-name`}>{dimension.name}：</div>
                          {dimension.bizName.includes('photo') ? (
                            <img width={40} height={40} src={dimension.value} alt="" />
                          ) : (
                            <div className={`${prefixCls}-info-value`}>{dimension.value}</div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            {queryMode === 'TAG_LIST_FILTER' && (
              <div className={`${prefixCls}-info-bar`}>
                <div className={`${prefixCls}-main-entity-info`}>
                  <div className={`${prefixCls}-info-item`}>
                    <div className={`${prefixCls}-info-name`}>数据模型：</div>
                    <div className={`${prefixCls}-info-value`}>{modelName}</div>
                  </div>
                  <div className={`${prefixCls}-info-item`}>
                    <div className={`${prefixCls}-info-name`}>时间：</div>
                    <div className={`${prefixCls}-info-value`}>
                      {startDate === endDate ? startDate : `${startDate} ~ ${endDate}`}
                    </div>
                  </div>
                  {dimensionFilters && dimensionFilters?.length > 0 && (
                    <div className={`${prefixCls}-info-item`}>
                      <div className={`${prefixCls}-info-name`}>筛选条件：</div>
                      {dimensionFilters.map((filter, index) => (
                        <div className={`${prefixCls}-info-value`}>
                          <span>{filter.name}：</span>
                          <span>
                            {Array.isArray(filter.value) ? filter.value.join('、') : filter.value}
                          </span>
                          {index !== dimensionFilters.length - 1 && <span>、</span>}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
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
