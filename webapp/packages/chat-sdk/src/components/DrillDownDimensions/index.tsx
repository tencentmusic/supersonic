import { useEffect, useState } from 'react';
import { CLS_PREFIX } from '../../common/constants';
import { DrillDownDimensionType, FilterItemType } from '../../common/type';
import { queryDrillDownDimensions } from '../../service';
import { Dropdown, Menu } from 'antd';
import { DownOutlined } from '@ant-design/icons';
import classNames from 'classnames';

type Props = {
  modelId: number;
  metricId?: number;
  drillDownDimension?: DrillDownDimensionType;
  isMetricCard?: boolean;
  originDimensions?: DrillDownDimensionType[];
  dimensionFilters?: FilterItemType[];
  onSelectDimension: (dimension?: DrillDownDimensionType) => void;
};

const MAX_DIMENSION_COUNT = 20;

const DEFAULT_DIMENSION_COUNT = 5;

const DrillDownDimensions: React.FC<Props> = ({
  modelId,
  metricId,
  drillDownDimension,
  isMetricCard,
  originDimensions,
  dimensionFilters,
  onSelectDimension,
}) => {
  const [dimensions, setDimensions] = useState<DrillDownDimensionType[]>([]);

  const prefixCls = `${CLS_PREFIX}-drill-down-dimensions`;

  const initData = async () => {
    const res = await queryDrillDownDimensions(modelId, metricId);
    setDimensions(
      res.data.dimensions
        .filter(
          dimension =>
            !dimensionFilters?.some(filter => filter.name === dimension.name) &&
            (!originDimensions || !originDimensions.some(item => item.id === dimension.id))
        )
        .slice(0, MAX_DIMENSION_COUNT)
    );
  };

  useEffect(() => {
    initData();
  }, []);

  const cancelDrillDown = () => {
    onSelectDimension(undefined);
  };

  const defaultDimensions = dimensions.slice(0, DEFAULT_DIMENSION_COUNT);

  const drillDownDimensionsSectionClass = classNames(`${prefixCls}-section`, {
    [`${prefixCls}-metric-card`]: isMetricCard,
  });

  return (
    <div className={prefixCls}>
      <div className={drillDownDimensionsSectionClass}>
        <div className={`${prefixCls}-title`}>推荐下钻维度：</div>
        <div className={`${prefixCls}-content`}>
          {defaultDimensions.map((dimension, index) => {
            const itemNameClass = classNames(`${prefixCls}-content-item-name`, {
              [`${prefixCls}-content-item-active`]: drillDownDimension?.id === dimension.id,
            });
            return (
              <div>
                <span
                  className={itemNameClass}
                  onClick={() => {
                    onSelectDimension(
                      drillDownDimension?.id === dimension.id ? undefined : dimension
                    );
                  }}
                >
                  {dimension.name}
                </span>
                {index !== defaultDimensions.length - 1 && <span>、</span>}
              </div>
            );
          })}
          {dimensions.length > DEFAULT_DIMENSION_COUNT && (
            <div>
              <span>、</span>
              <Dropdown
                overlay={
                  <Menu>
                    {dimensions.slice(DEFAULT_DIMENSION_COUNT).map(dimension => {
                      const itemNameClass = classNames({
                        [`${prefixCls}-menu-item-active`]: drillDownDimension?.id === dimension.id,
                      });
                      return (
                        <Menu.Item key={dimension.id}>
                          <span
                            className={itemNameClass}
                            onClick={() => {
                              onSelectDimension(dimension);
                            }}
                          >
                            {dimension.name}
                          </span>
                        </Menu.Item>
                      );
                    })}
                  </Menu>
                }
              >
                <span>
                  <span className={`${prefixCls}-content-item-name`}>更多</span>
                  <DownOutlined className={`${prefixCls}-down-arrow`} />
                </span>
              </Dropdown>
            </div>
          )}
          {drillDownDimension && (
            <div className={`${prefixCls}-cancel-drill-down`} onClick={cancelDrillDown}>
              取消下钻
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DrillDownDimensions;
