import { useEffect, useState } from 'react';
import { CLS_PREFIX } from '../../common/constants';
import { DrillDownDimensionType, FilterItemType } from '../../common/type';
import DimensionSection from './DimensionSection';

type Props = {
  drillDownDimensions: DrillDownDimensionType[];
  drillDownDimension?: DrillDownDimensionType;
  secondDrillDownDimension?: DrillDownDimensionType;
  originDimensions?: DrillDownDimensionType[];
  dimensionFilters?: FilterItemType[];
  onSelectDimension: (dimension?: DrillDownDimensionType) => void;
  onSelectSecondDimension: (dimension?: DrillDownDimensionType) => void;
};

const MAX_DIMENSION_COUNT = 20;

const DrillDownDimensions: React.FC<Props> = ({
  drillDownDimensions,
  drillDownDimension,
  secondDrillDownDimension,
  originDimensions,
  dimensionFilters,
  onSelectDimension,
  onSelectSecondDimension,
}) => {
  const [dimensions, setDimensions] = useState<DrillDownDimensionType[]>([]);

  const prefixCls = `${CLS_PREFIX}-drill-down-dimensions`;

  const initData = async () => {
    setDimensions(
      drillDownDimensions
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

  const cancelSecondDrillDown = () => {
    onSelectSecondDimension(undefined);
  };

  return (
    <div className={prefixCls}>
      <DimensionSection
        drillDownDimension={drillDownDimension}
        dimensions={dimensions}
        onSelectDimension={onSelectDimension}
        onCancelDrillDown={cancelDrillDown}
      />
      {drillDownDimension && dimensions.length > 1 && (
        <DimensionSection
          drillDownDimension={secondDrillDownDimension}
          dimensions={dimensions.filter(dimension => dimension.id !== drillDownDimension?.id)}
          isSecondDrillDown
          onSelectDimension={onSelectSecondDimension}
          onCancelDrillDown={cancelSecondDrillDown}
        />
      )}
    </div>
  );
};

export default DrillDownDimensions;
