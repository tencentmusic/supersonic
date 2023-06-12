import classNames from 'classnames';
import { CLS_PREFIX } from '../../../common/constants';
import ApplyAuth from '../ApplyAuth';

type Props = {
  domain: string;
  chartType?: string;
  onApplyAuth?: (domain: string) => void;
};

const NoPermissionChart: React.FC<Props> = ({ domain, chartType, onApplyAuth }) => {
  const prefixCls = `${CLS_PREFIX}-no-permission-chart`;

  const chartHolderClass = classNames(`${prefixCls}-holder`, {
    [`${prefixCls}-bar-chart-holder`]: chartType === 'barChart',
  });

  return (
    <div className={prefixCls}>
      <div className={chartHolderClass} />
      <div className={`${prefixCls}-no-permission`}>
        <ApplyAuth domain={domain} onApplyAuth={onApplyAuth} />
      </div>
    </div>
  );
};

export default NoPermissionChart;
