import classNames from 'classnames';
import { CLS_PREFIX, DATE_TYPES } from '../../../common/constants';
import { isMobile } from '../../../utils/utils';
import { ChatContextType } from '../../../common/type';

type Props = {
  chatContext: ChatContextType;
  currentDateOption?: number;
  onSelectDateOption: (value: number) => void;
};

const DateOptions: React.FC<Props> = ({ chatContext, currentDateOption, onSelectDateOption }) => {
  const prefixCls = `${CLS_PREFIX}-date-options`;

  const dateOptions = DATE_TYPES[chatContext?.dateInfo?.period] || DATE_TYPES.DAY;

  return (
    <div className={prefixCls}>
      {dateOptions.map((dateOption: { label: string; value: number }, index: number) => {
        const dateOptionClass = classNames(`${prefixCls}-item`, {
          [`${prefixCls}-date-active`]: dateOption.value === currentDateOption,
          [`${prefixCls}-date-mobile`]: isMobile,
        });
        return (
          <>
            <div
              key={dateOption.value}
              className={dateOptionClass}
              onClick={() => {
                onSelectDateOption(dateOption.value);
              }}
            >
              {dateOption.label}
              {dateOption.value === currentDateOption && (
                <div className={`${prefixCls}-active-identifier`} />
              )}
            </div>
            {index !== dateOptions.length - 1 && <div className={`${prefixCls}-item-divider`} />}
          </>
        );
      })}
    </div>
  );
};

export default DateOptions;
