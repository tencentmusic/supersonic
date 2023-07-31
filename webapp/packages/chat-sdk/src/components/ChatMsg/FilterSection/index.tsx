import { PREFIX_CLS } from '../../../common/constants';
import { ChatContextType } from '../../../common/type';

type Props = {
  chatContext?: ChatContextType;
};

const FilterSection: React.FC<Props> = ({ chatContext }) => {
  const prefixCls = `${PREFIX_CLS}-filter-section`;

  const { dimensionFilters } = chatContext || {};

  const hasFilterSection = dimensionFilters && dimensionFilters.length > 0;

  return hasFilterSection ? (
    <div className={prefixCls}>
      <div className={`${prefixCls}-field-label`}>筛选条件：</div>
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
              <span className={`${prefixCls}-field-name`}>{filterItem.name}：</span>
              <span className={`${prefixCls}-filter-value`}>{filterValue.join('、')}</span>
            </div>
          );
        })}
      </div>
    </div>
  ) : null;
};

export default FilterSection;
