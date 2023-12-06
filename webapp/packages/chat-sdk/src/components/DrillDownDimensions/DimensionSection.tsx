import classNames from 'classnames';
import { CLS_PREFIX } from '../../common/constants';
import { DrillDownDimensionType } from '../../common/type';
import { Dropdown, Menu } from 'antd';
import { DownOutlined } from '@ant-design/icons';

type Props = {
  drillDownDimension?: DrillDownDimensionType;
  dimensions: DrillDownDimensionType[];
  isSecondDrillDown?: boolean;
  onSelectDimension: (dimension?: DrillDownDimensionType) => void;
  onCancelDrillDown: () => void;
};

const DEFAULT_DIMENSION_COUNT = 5;

const DimensionSection: React.FC<Props> = ({
  drillDownDimension,
  dimensions,
  isSecondDrillDown,
  onSelectDimension,
  onCancelDrillDown,
}) => {
  const prefixCls = `${CLS_PREFIX}-drill-down-dimensions`;

  const defaultDimensions = dimensions.slice(0, DEFAULT_DIMENSION_COUNT);

  if (defaultDimensions.length === 0) {
    return null;
  }

  return (
    <div className={`${prefixCls}-section`}>
      <div className={`${prefixCls}-title`}>{isSecondDrillDown ? '二级' : '推荐'}下钻维度：</div>
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
          <div className={`${prefixCls}-cancel-drill-down`} onClick={onCancelDrillDown}>
            取消{isSecondDrillDown ? '二级' : ''}下钻
          </div>
        )}
      </div>
    </div>
  );
};

export default DimensionSection;
