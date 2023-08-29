import React, { ReactNode } from 'react';
import { AGG_TYPE_MAP, PREFIX_CLS } from '../../common/constants';
import { ChatContextType } from '../../common/type';
import { CheckCircleFilled, InfoCircleOutlined } from '@ant-design/icons';
import classNames from 'classnames';
import SwicthEntity from './SwitchEntity';
import { Tooltip } from 'antd';
import Loading from './Loading';

type Props = {
  parseLoading: boolean;
  parseInfoOptions: ChatContextType[];
  parseTip: string;
  currentParseInfo?: ChatContextType;
  optionMode?: boolean;
  onSelectParseInfo: (parseInfo: ChatContextType) => void;
  onSwitchEntity: (entityId: string) => void;
};

const MAX_OPTION_VALUES_COUNT = 2;

const ParseTip: React.FC<Props> = ({
  parseLoading,
  parseInfoOptions,
  parseTip,
  currentParseInfo,
  optionMode,
  onSelectParseInfo,
  onSwitchEntity,
}) => {
  const prefixCls = `${PREFIX_CLS}-item`;

  const getNode = (tipTitle: string, tipNode?: ReactNode, parseSucceed?: boolean) => {
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
    } = parseInfo || {};
    const { startDate, endDate } = dateInfo || {};
    const dimensionItems = dimensions?.filter(item => item.type === 'DIMENSION');
    const metric = metrics?.[0];

    const tipContentClass = classNames(`${prefixCls}-tip-content`, {
      [`${prefixCls}-tip-content-option`]: isOptions,
      [`${prefixCls}-tip-content-option-active`]:
        isOptions &&
        currentParseInfo &&
        JSON.stringify(currentParseInfo) === JSON.stringify(parseInfo),
      [`${prefixCls}-tip-content-option-disabled`]:
        isOptions &&
        currentParseInfo !== undefined &&
        JSON.stringify(currentParseInfo) !== JSON.stringify(parseInfo),
    });

    const itemValueClass = classNames({
      [`${prefixCls}-tip-item-value`]: !isOptions,
      [`${prefixCls}-tip-item-option`]: isOptions,
    });

    const entityId = dimensionFilters?.length > 0 ? dimensionFilters[0].value : undefined;
    const entityAlias = entity?.alias?.[0]?.split('.')?.[0];
    const entityName = elementMatches?.find(item => item.element?.type === 'ID')?.element.name;

    const { type: agentType, name: agentName } = properties || {};

    const fields =
      queryMode === 'ENTITY_DETAIL' ? dimensionItems?.concat(metrics || []) : dimensionItems;

    const getFilterContent = (filters: any) => {
      return (
        <div className={`${prefixCls}-tip-item-filter-content`}>
          {filters.map((filter: any, index: number) => (
            <div className={itemValueClass}>
              <span>
                {filter.name}
                {filter.operator !== '=' ? ` ${filter.operator} ` : '：'}
              </span>
              <span>{Array.isArray(filter.value) ? filter.value.join('、') : filter.value}</span>
              {index !== filters.length - 1 && <span>、</span>}
            </div>
          ))}
        </div>
      );
    };

    const getFiltersNode = () => {
      return (
        <div className={`${prefixCls}-tip-item`}>
          <div className={`${prefixCls}-tip-item-name`}>筛选条件：</div>
          <Tooltip
            title={
              dimensionFilters.length > MAX_OPTION_VALUES_COUNT
                ? getFilterContent(dimensionFilters)
                : ''
            }
            color="#fff"
            overlayStyle={{ maxWidth: 'none' }}
          >
            <div className={`${prefixCls}-tip-item-content`}>
              {getFilterContent(dimensionFilters.slice(0, MAX_OPTION_VALUES_COUNT))}
              {dimensionFilters.length > MAX_OPTION_VALUES_COUNT && ' ...'}
            </div>
          </Tooltip>
        </div>
      );
    };

    return (
      <div
        className={tipContentClass}
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
            {(queryMode.includes('ENTITY') || queryMode === 'DSL') &&
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
                <div className={`${prefixCls}-tip-item-name`}>主题域：</div>
                <div className={itemValueClass}>{modelName}</div>
              </div>
            )}
            {queryMode !== 'ENTITY_ID' && metric && (
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
            {['METRIC_GROUPBY', 'METRIC_ORDERBY', 'ENTITY_DETAIL'].includes(queryMode) &&
              fields &&
              fields.length > 0 && (
                <div className={`${prefixCls}-tip-item`}>
                  <div className={`${prefixCls}-tip-item-name`}>
                    {queryMode === 'ENTITY_DETAIL' ? '查询字段' : '下钻维度'}：
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

  let tipNode: ReactNode;

  if (parseInfoOptions.length > 1 || optionMode) {
    tipNode = (
      <div className={`${prefixCls}-multi-options`}>
        <div>
          还有以下的相关问题，<strong>请您点击提交</strong>
        </div>
        <div className={`${prefixCls}-options`}>
          {parseInfoOptions.map((item, index) => getTipNode(item, true, index))}
        </div>
      </div>
    );
  } else {
    const { type } = parseInfoOptions[0]?.properties || {};
    const entityAlias = parseInfoOptions[0]?.entity?.alias?.[0]?.split('.')?.[0];
    const entityName = parseInfoOptions[0]?.elementMatches?.find(
      item => item.element?.type === 'ID'
    )?.element.name;
    const queryMode = parseInfoOptions[0]?.queryMode;

    tipNode = (
      <div className={`${prefixCls}-tip`}>
        {getTipNode(parseInfoOptions[0])}
        {(!type || queryMode === 'DSL') && entityAlias && entityName && (
          <div className={`${prefixCls}-switch-entity-tip`}>
            <InfoCircleOutlined />
            <div>
              如果未匹配到您查询的{entityAlias}，可点击上面的{entityAlias}名切换
            </div>
          </div>
        )}
      </div>
    );
  }

  return getNode('意图解析结果', tipNode, true);
};

export default ParseTip;
