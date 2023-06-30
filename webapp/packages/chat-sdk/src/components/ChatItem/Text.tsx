import Message from '../ChatMsg/Message';
import { PREFIX_CLS } from '../../common/constants';

type Props = {
  data: any;
};

const Text: React.FC<Props> = ({ data }) => {
  const prefixCls = `${PREFIX_CLS}-item`;
  return (
    <Message position="left" bubbleClassName={`${prefixCls}-text-bubble`}>
      <div className={`${prefixCls}-text`}>{data}</div>
    </Message>
  );
};

export default Text;
