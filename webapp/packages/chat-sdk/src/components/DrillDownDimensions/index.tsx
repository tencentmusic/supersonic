import { useEffect, useState } from 'react';
import { CLS_PREFIX } from '../../common/constants';
import { DrillDownDimensionType, FilterItemType } from '../../common/type';
import { queryDrillDownDimensions } from '../../service';
import { Dropdown } from 'antd';
import { DownOutlined } from '@ant-design/icons';
import classNames from 'classnames';

type Props = {
  domainId: number;
  drillDownDimension?: DrillDownDimensionType;
  isMetricCard?: boolean;
  dimensionFilters?: FilterItemType[];
  onSelectDimension: (dimension?: DrillDownDimensionType) => void;
};

const MAX_DIMENSION_COUNT = 20;

const DrillDownDimensions: React.FC<Props> = ({
  domainId,
  drillDownDimension,
  isMetricCard,
  dimensionFilters,
  onSelectDimension,
}) => {
  const [dimensions, setDimensions] = useState<DrillDownDimensionType[]>([]);

  const DEFAULT_DIMENSION_COUNT = isMetricCard ? 3 : 5;

  const prefixCls = `${CLS_PREFIX}-drill-down-dimensions`;

  const initData = async () => {
    const res = await queryDrillDownDimensions(domainId);
    setDimensions(
      res.data.data.dimensions
        .filter(dimension => !dimensionFilters?.some(filter => filter.name === dimension.name))
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
        <div className={`${prefixCls}-title`}>快速维度下钻：</div>
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
                menu={{
                  items: dimensions.slice(DEFAULT_DIMENSION_COUNT).map(dimension => {
                    const itemNameClass = classNames({
                      [`${prefixCls}-menu-item-active`]: drillDownDimension?.id === dimension.id,
                    });
                    return {
                      label: (
                        <span
                          className={itemNameClass}
                          onClick={() => {
                            onSelectDimension(dimension);
                          }}
                        >
                          {dimension.name}
                        </span>
                      ),
                      key: dimension.id,
                    };
                  }),
                }}
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
