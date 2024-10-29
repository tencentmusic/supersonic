import { ChatContextTypeQueryTypeEnum } from '../../common/constants';
import { AGG_TYPE_MAP, PREFIX_CLS } from '../../common/constants';
export const MAX_OPTION_VALUES_COUNT = 2;

export const prefixCls = `${PREFIX_CLS}-item`;

export const getTipNode = ({ parseInfo, dimensionFilters, entityInfo }) => {
  const {
    dataSet,
    dimensions,
    metrics,
    aggType,
    queryMode,
    queryType,
    properties,
    entity,
    elementMatches,
  } = parseInfo || {};
  const dimensionItems = dimensions?.filter(item => item.type === 'DIMENSION');
  const itemValueClass = `${prefixCls}-tip-item-value`;
  const entityId = dimensionFilters?.length > 0 ? dimensionFilters[0].value : undefined;
  const entityAlias = entity?.alias?.[0]?.split('.')?.[0];
  const entityName = elementMatches?.find(item => item.element?.type === 'ID')?.element.name;

  const { type: agentType, name: agentName } = properties || {};

  const fields =
    queryMode === 'TAG_DETAIL' ? dimensionItems?.concat(metrics || []) : dimensionItems;
  return (
    <div className={`${prefixCls}-tip-content`}>
      {!!agentType && queryMode !== 'LLM_S2SQL' ? (
        <div className={`${prefixCls}-tip-item`}>
          将由{agentType === 'plugin' ? '插件' : '内置'}工具
          <span className={itemValueClass}>{agentName}</span>来解答
        </div>
      ) : (
        <>
          {(queryMode?.includes('ENTITY') || queryMode === 'LLM_S2SQL') &&
          typeof entityId === 'string' &&
          !!entityAlias &&
          !!entityName ? (
            <div className={`${prefixCls}-tip-item`}>
              <div className={`${prefixCls}-tip-item-name`}>{entityAlias}：</div>
              <div className={itemValueClass}>{entityName}</div>
            </div>
          ) : (
            <div className={`${prefixCls}-tip-item`}>
              <div className={`${prefixCls}-tip-item-name`}>数据集：</div>
              <div className={itemValueClass}>{dataSet?.name}</div>
            </div>
          )}
          {(queryType === ChatContextTypeQueryTypeEnum.AGGREGATE ||
            queryType === 'METRIC_TAG' ||
            queryType === 'DETAIL') && (
            <div className={`${prefixCls}-tip-item`}>
              <div className={`${prefixCls}-tip-item-name`}>查询模式：</div>
              <div className={itemValueClass}>
                {queryType === ChatContextTypeQueryTypeEnum.AGGREGATE || queryType === 'METRIC_TAG'
                  ? '聚合模式'
                  : '明细模式'}
              </div>
            </div>
          )}
          {queryType !== 'DETAIL' &&
            metrics &&
            metrics.length > 0 &&
            !dimensions?.some(item => item.bizName?.includes('_id')) && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>指标：</div>
                <div className={itemValueClass}>
                  {metrics.map(metric => metric.name).join('、')}
                </div>
              </div>
            )}
          {[
            'METRIC_GROUPBY',
            'METRIC_ORDERBY',
            'TAG_DETAIL',
            'LLM_S2SQL',
            'METRIC_FILTER',
          ].includes(queryMode!) &&
            fields &&
            fields.length > 0 && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>
                  {queryType === 'DETAIL' ? '查询字段' : '下钻维度'}：
                </div>
                <div className={itemValueClass}>
                  {fields
                    .slice(0, MAX_OPTION_VALUES_COUNT)
                    .map(field => field.name)
                    .join('、')}
                  {fields.length > MAX_OPTION_VALUES_COUNT && '...'}
                </div>
              </div>
            )}
          {queryMode !== 'TAG_ID' &&
            !dimensions?.some(item => item.bizName?.includes('_id')) &&
            entityInfo?.dimensions
              ?.filter(dimension => dimension.value != null)
              .map(dimension => (
                <div className={`${prefixCls}-tip-item`} key={dimension.itemId}>
                  <div className={`${prefixCls}-tip-item-name`}>{dimension.name}：</div>
                  <div className={itemValueClass}>{dimension.value}</div>
                </div>
              ))}
          {(queryMode === 'METRIC_ORDERBY' || queryMode === 'METRIC_MODEL') &&
            aggType &&
            aggType !== 'NONE' && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>聚合方式：</div>
                <div className={itemValueClass}>{AGG_TYPE_MAP[aggType]}</div>
              </div>
            )}
        </>
      )}
    </div>
  );
};
