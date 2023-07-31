import classNames from 'classnames';
import { PREFIX_CLS } from '../../../common/constants';
import IconFont from '../../IconFont';

type Props = {
  title: string;
  value: string;
};

const PeriodCompareItem: React.FC<Props> = ({ title, value }) => {
  const prefixCls = `${PREFIX_CLS}-metric-card`;

  const itemValueClass = classNames(`${prefixCls}-period-compare-item-value`, {
    [`${prefixCls}-period-compare-item-value-up`]: !value.includes('-'),
    [`${prefixCls}-period-compare-item-value-down`]: value.includes('-'),
  });

  return (
    <div className={`${prefixCls}-period-compare-item`}>
      <div className={`${prefixCls}-period-compare-item-title`}>{title}</div>
      <div className={itemValueClass}>
        <IconFont type={!value.includes('-') ? 'icon-shangsheng' : 'icon-xiajiang'} />
        <div>{value}</div>
      </div>
    </div>
  );
};

export default PeriodCompareItem;
