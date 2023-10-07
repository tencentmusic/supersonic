import React, { ReactNode } from 'react';
import { AGG_TYPE_MAP, PREFIX_CLS } from '../../common/constants';
import { ChatContextType, FilterItemType } from '../../common/type';
import { CheckCircleFilled } from '@ant-design/icons';
import classNames from 'classnames';
import SwicthEntity from './SwitchEntity';
import Loading from './Loading';
import FilterItem from './FilterItem';

type Props = {
  parseLoading: boolean;
  parseInfoOptions: ChatContextType[];
  parseTip: string;
  currentParseInfo?: ChatContextType;
  onSelectParseInfo: (parseInfo: ChatContextType) => void;
  onSwitchEntity: (entityId: string) => void;
  onFiltersChange: (filters: FilterItemType[]) => void;
};

const MAX_OPTION_VALUES_COUNT = 2;

const ParseTip: React.FC<Props> = ({
  parseLoading,
  parseInfoOptions,
  parseTip,
  currentParseInfo,
  onSelectParseInfo,
  onSwitchEntity,
  onFiltersChange,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;

  const getNode = (tipTitle: ReactNode, tipNode?: ReactNode, parseSucceed?: boolean) => {
    const contentContainerClass = classNames(`${prefixCls}-content-container`, {
      [`${prefixCls}-content-container-succeed`]: parseSucceed,
    });
    return (
      <div className={`${prefixCls}-parse-tip`}>
        <div className={`${prefixCls}-title-bar`}>
          <CheckCircleFilled className={`${prefixCls}-step-icon`} />
          <div className={`${prefixCls}-step-title`}>
            {tipTitle}
            {!tipNode && <Loading />}
          </div>
        </div>
        {tipNode && <div className={contentContainerClass}>{tipNode}</div>}
      </div>
    );
  };

  if (parseLoading) {
    return getNode('意图解析中');
  }

  if (parseTip) {
    return getNode('意图解析失败', parseTip);
  }

  if (parseInfoOptions.length === 0) {
    return null;
  }

  const getTipNode = (parseInfo: ChatContextType, isOptions?: boolean, index?: number) => {
    const {
      modelName,
      dateInfo,
      dimensionFilters,
      dimensions,
      metrics,
      aggType,
      queryMode,
      properties,
      entity,
      elementMatches,
      nativeQuery,
    } = parseInfo || {};

    const { startDate, endDate } = dateInfo || {};
    const dimensionItems = dimensions?.filter(item => item.type === 'DIMENSION');
    const metric = metrics?.[0];

    const itemValueClass = `${prefixCls}-tip-item-value`;
    const entityId = dimensionFilters?.length > 0 ? dimensionFilters[0].value : undefined;
    const entityAlias = entity?.alias?.[0]?.split('.')?.[0];
    const entityName = elementMatches?.find(item => item.element?.type === 'ID')?.element.name;

    const { type: agentType, name: agentName } = properties || {};

    const fields =
      queryMode === 'ENTITY_DETAIL' ? dimensionItems?.concat(metrics || []) : dimensionItems;

    return (
      <div
        className={`${prefixCls}-tip-content`}
        onClick={() => {
          if (isOptions && currentParseInfo === undefined) {
            onSelectParseInfo(parseInfo);
          }
        }}
      >
        {index !== undefined && <div>{index + 1}.</div>}
        {!!agentType && queryMode !== 'DSL' ? (
          <div className={`${prefixCls}-tip-item`}>
            将由{agentType === 'plugin' ? '插件' : '内置'}工具
            <span className={itemValueClass}>{agentName}</span>来解答
          </div>
        ) : (
          <>
            {(queryMode?.includes('ENTITY') || queryMode === 'DSL') &&
            typeof entityId === 'string' &&
            !!entityAlias &&
            !!entityName ? (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>{entityAlias}：</div>
                {!isOptions && (entityAlias === '歌曲' || entityAlias === '艺人') ? (
                  <SwicthEntity
                    entityName={entityName}
                    chatContext={parseInfo}
                    onSwitchEntity={onSwitchEntity}
                  />
                ) : (
                  <div className={itemValueClass}>{entityName}</div>
                )}
              </div>
            ) : (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>数据来源：</div>
                <div className={itemValueClass}>{modelName}</div>
              </div>
            )}
            {!queryMode?.includes('ENTITY') && metric && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>指标：</div>
                <div className={itemValueClass}>{metric.name}</div>
              </div>
            )}
            {!isOptions && (
              <div className={`${prefixCls}-tip-item`}>
                <div className={`${prefixCls}-tip-item-name`}>数据时间：</div>
                <div className={itemValueClass}>
                  {startDate === endDate ? startDate : `${startDate} ~ ${endDate}`}
                </div>
              </div>
            )}
            {['METRIC_GROUPBY', 'METRIC_ORDERBY', 'ENTITY_DETAIL', 'DSL'].includes(queryMode) &&
              fields &&
              fields.length > 0 && (
                <div className={`${prefixCls}-tip-item`}>
                  <div className={`${prefixCls}-tip-item-name`}>
                    {queryMode === 'DSL'
                      ? nativeQuery
                        ? '查询字段'
                        : '下钻维度'
                      : queryMode === 'ENTITY_DETAIL'
                      ? '查询字段'
                      : '下钻维度'}
                    ：
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
            {queryMode !== 'ENTITY_ID' &&
              entityDimensions?.length > 0 &&
              entityDimensions.map(dimension => (
                <div className={`${prefixCls}-tip-item`} key={dimension.itemId}>
                  <div className={`${prefixCls}-tip-item-name`}>{dimension.name}：</div>
                  <div className={itemValueClass}>{dimension.value}</div>
                </div>
              ))}
            {queryMode === 'METRIC_ORDERBY' && aggType && aggType !== 'NONE' && (
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

  const parseInfo = parseInfoOptions[0] || {};
  const { modelId, properties, entity, entityInfo, elementMatches, queryMode, dimensionFilters } =
    parseInfo || {};

  const { type } = properties || {};
  const entityAlias = entity?.alias?.[0]?.split('.')?.[0];
  const entityName = elementMatches?.find(item => item.element?.type === 'ID')?.element.name;

  const entityDimensions = entityInfo?.dimensions?.filter(
    item =>
      !['zyqk_song_id', 'song_name', 'singer_id', 'zyqk_cmpny_id'].includes(item.bizName) &&
      !(
        entityInfo?.dimensions?.some(dimension => dimension.bizName === 'singer_id') &&
        item.bizName === 'singer_name'
      ) &&
      !(
        entityInfo?.dimensions?.some(dimension => dimension.bizName === 'zyqk_cmpny_id') &&
        item.bizName === 'cmpny_name'
      )
  );

  const getFilterContent = (filters: any) => {
    const itemValueClass = `${prefixCls}-tip-item-value`;
    return (
      <div className={`${prefixCls}-tip-item-filter-content`}>
        {filters.map((filter: any) => (
          <div className={`${prefixCls}-tip-item-option`} key={filter.name}>
            <span>
              <span className={`${prefixCls}-tip-item-filter-name`}>{filter.name}</span>
              {filter.operator !== '=' && filter.operator !== 'IN' ? ` ${filter.operator} ` : '：'}
            </span>
            {/* {queryMode !== 'DSL' && !filter.bizName?.includes('_id') ? ( */}
            {!filter.bizName?.includes('_id') ? (
              <FilterItem
                modelId={modelId}
                filters={dimensionFilters}
                filter={filter}
                onFiltersChange={onFiltersChange}
              />
            ) : (
              <span className={itemValueClass}>{filter.value}</span>
            )}
          </div>
        ))}
      </div>
    );
  };

  const getFiltersNode = () => {
    return (
      <div className={`${prefixCls}-tip-item`}>
        <div className={`${prefixCls}-tip-item-name`}>筛选条件：</div>
        <div className={`${prefixCls}-tip-item-content`}>{getFilterContent(dimensionFilters)}</div>
      </div>
    );
  };

  const tipNode = (
    <div className={`${prefixCls}-tip`}>
      {getTipNode(parseInfo)}
      {[
        'METRIC_FILTER',
        'METRIC_ENTITY',
        'ENTITY_DETAIL',
        'ENTITY_LIST_FILTER',
        'ENTITY_ID',
        'DSL',
      ].includes(queryMode) &&
        dimensionFilters &&
        dimensionFilters?.length > 0 &&
        getFiltersNode()}
    </div>
  );

  return getNode(
    <div className={`${prefixCls}-title-bar`}>
      意图解析
      {(!type || queryMode === 'DSL') && entityAlias && entityAlias !== '厂牌' && entityName && (
        <div className={`${prefixCls}-switch-entity-tip`}>
          (如果未匹配到您查询的{entityAlias}，可点击{entityAlias}名切换)
        </div>
      )}
    </div>,
    tipNode,
    true
  );
};

export default ParseTip;
