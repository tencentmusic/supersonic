import { isMobile } from '../../utils/utils';
import { ReloadOutlined } from '@ant-design/icons';
import classNames from 'classnames';
import { useEffect, useState } from 'react';
import { EntityInfoType } from '../../common/type';
import Message from '../ChatMsg/Message';
import { CLS_PREFIX } from '../../common/constants';

type Props = {
  currentMsgAggregator?: string;
  columns: any[];
  mainEntity: EntityInfoType;
  suggestions: any;
  onSelect?: (value: string) => void;
};

const PAGE_SIZE = isMobile ? 3 : 5;

const Suggestion: React.FC<Props> = ({
  currentMsgAggregator,
  columns,
  mainEntity,
  suggestions,
  onSelect,
}) => {
  const [dimensions, setDimensions] = useState<string[]>([]);
  const [metrics, setMetrics] = useState<string[]>([]);
  const [dimensionIndex, setDimensionIndex] = useState(0);
  const [metricIndex, setMetricIndex] = useState(0);

  const fields = columns
    .filter(column => currentMsgAggregator !== 'tag' || column.showType !== 'NUMBER')
    .concat(isMobile ? [] : mainEntity?.dimensions || [])
    .map(item => item.name);

  useEffect(() => {
    setDimensions(
      suggestions.dimensions
        .filter((dimension: any) => !fields.some(field => field === dimension.name))
        .map((item: any) => item.name)
    );
    setMetrics(
      suggestions.metrics
        .filter((metric: any) => !fields.some(field => field === metric.name))
        .map((item: any) => item.name)
    );
  }, []);

  const reloadDimensionCmds = () => {
    const dimensionPageCount = Math.ceil(dimensions.length / PAGE_SIZE);
    setDimensionIndex((dimensionIndex + 1) % dimensionPageCount);
  };

  const reloadMetricCmds = () => {
    const metricPageCount = Math.ceil(metrics.length / PAGE_SIZE);
    setMetricIndex((metricIndex + 1) % metricPageCount);
  };

  const dimensionList = dimensions.slice(
    dimensionIndex * PAGE_SIZE,
    (dimensionIndex + 1) * PAGE_SIZE
  );

  const metricList = metrics.slice(metricIndex * PAGE_SIZE, (metricIndex + 1) * PAGE_SIZE);

  if (!dimensionList.length && !metricList.length) {
    return null;
  }

  const prefixCls = `${CLS_PREFIX}-suggestion`;

  const suggestionClass = classNames(prefixCls, {
    [`${prefixCls}-mobile`]: isMobile,
  });

  const sectionItemClass = classNames({
    [`${prefixCls}-section-item-selectable`]: onSelect !== undefined,
  });

  return (
    <div className={suggestionClass}>
      <Message position="left" width="fit-content" noWaterMark>
        <div className={`${prefixCls}-tip`}>问答支持多轮对话，您可以继续输入：</div>
        {metricList.length > 0 && (
          <div className={`${prefixCls}-content-section`}>
            <div className={`${prefixCls}-title`}>指标：</div>
            <div className={`${prefixCls}-section-items`}>
              {metricList.map((metric, index) => {
                let metricNode = (
                  <div
                    className={sectionItemClass}
                    onClick={() => {
                      if (onSelect) {
                        onSelect(metric);
                      }
                    }}
                  >
                    {metric}
                  </div>
                );
                return (
                  <>
                    {metricNode}
                    {index < metricList.length - 1 && '、'}
                  </>
                );
              })}
            </div>
            {metrics.length > PAGE_SIZE && (
              <div
                className={`${prefixCls}-reload`}
                onClick={() => {
                  reloadMetricCmds();
                }}
              >
                <ReloadOutlined className={`${prefixCls}-reload-icon`} />
                {!isMobile && <div className={`${prefixCls}-reload-label`}>换一批</div>}
              </div>
            )}
          </div>
        )}
        {dimensionList.length > 0 && (
          <div className={`${prefixCls}-content-section`}>
            <div className={`${prefixCls}-title`}>维度：</div>
            <div className={`${prefixCls}-section-items`}>
              {dimensionList.map((dimension, index) => {
                return (
                  <>
                    <div
                      className={sectionItemClass}
                      onClick={() => {
                        if (onSelect) {
                          onSelect(dimension);
                        }
                      }}
                    >
                      {dimension}
                    </div>
                    {index < dimensionList.length - 1 && '、'}
                  </>
                );
              })}
            </div>
            {dimensions.length > PAGE_SIZE && (
              <div
                className={`${prefixCls}-reload`}
                onClick={() => {
                  reloadDimensionCmds();
                }}
              >
                <ReloadOutlined className={`${prefixCls}-reload-icon`} />
                {!isMobile && <div className={`${prefixCls}-reload-label`}>换一批</div>}
              </div>
            )}
          </div>
        )}
      </Message>
    </div>
  );
};

export default Suggestion;
