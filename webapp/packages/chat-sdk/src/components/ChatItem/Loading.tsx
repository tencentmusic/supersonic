import { PREFIX_CLS } from '../../common/constants';

const Loading = () => {
  const prefixCls = `${PREFIX_CLS}-item`;
  return (
    <span className={`${prefixCls}-loading`}>
      <span className={`${prefixCls}-loading-dot`} />
      <span className={`${prefixCls}-loading-dot`} />
      <span className={`${prefixCls}-loading-dot`} />
    </span>
  );
};

export default Loading;
