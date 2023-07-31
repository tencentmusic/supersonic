import { EntityInfoType, ChatContextType } from '../../../common/type';
import { PREFIX_CLS } from '../../../common/constants';

type Props = {
  position: 'left' | 'right';
  width?: number | string;
  height?: number | string;
  title?: string;
  followQuestions?: string[];
  bubbleClassName?: string;
  chatContext?: ChatContextType;
  entityInfo?: EntityInfoType;
  children?: React.ReactNode;
  isMobileMode?: boolean;
};

const Message: React.FC<Props> = ({
  position,
  width,
  height,
  title,
  followQuestions,
  children,
  bubbleClassName,
  chatContext,
  entityInfo,
  isMobileMode,
}) => {
  const { dimensionFilters, domainName } = chatContext || {};

  const prefixCls = `${PREFIX_CLS}-message`;

  const entityInfoList =
    entityInfo?.dimensions?.filter(dimension => !dimension.bizName.includes('photo')) || [];

  const hasFilterSection =
    dimensionFilters && dimensionFilters.length > 0 && entityInfoList.length === 0;

  const filterSection = hasFilterSection && (
    <div className={`${prefixCls}-filter-section`}>
      <div className={`${prefixCls}-field-name`}>筛选条件：</div>
      <div className={`${prefixCls}-filter-values`}>
        {dimensionFilters.map(filterItem => {
          const filterValue =
            typeof filterItem.value === 'string' ? [filterItem.value] : filterItem.value || [];
          return (
            <div
              className={`${prefixCls}-filter-item`}
              key={filterItem.name}
              title={filterValue.join('、')}
            >
              {filterItem.name}：{filterValue.join('、')}
            </div>
          );
        })}
      </div>
    </div>
  );

  const leftTitle = title
    ? followQuestions && followQuestions.length > 0
      ? `多轮对话：${[title, ...followQuestions].join(' ← ')}`
      : `单轮对话：${title}`
    : '';

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-title-bar`}>
        {domainName && <div className={`${prefixCls}-domain-name`}>{domainName}</div>}
        {position === 'left' && leftTitle && (
          <div className={`${prefixCls}-top-bar`} title={leftTitle}>
            ({leftTitle})
          </div>
        )}
      </div>
      <div className={`${prefixCls}-content`}>
        <div className={`${prefixCls}-body`}>
          <div
            className={`${prefixCls}-bubble${bubbleClassName ? ` ${bubbleClassName}` : ''}`}
            style={{ width, height }}
            onClick={e => {
              e.stopPropagation();
            }}
          >
            {entityInfoList.length > 0 && (
              <div className={`${prefixCls}-info-bar`}>
                {/* {filterSection} */}
                {entityInfoList.length > 0 && (
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
                )}
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
