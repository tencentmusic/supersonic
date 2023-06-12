import { CHAT_BLUE, PREFIX_CLS } from '../../common/constants';
import { Spin } from 'antd';
import BeatLoader from 'react-spinners/BeatLoader';
import Message from '../ChatMsg/Message';

const Typing = () => {
  const prefixCls = `${PREFIX_CLS}-item`;
  return (
    <Message position="left" bubbleClassName={`${prefixCls}-typing-bubble`}>
      <Spin
        spinning={true}
        indicator={<BeatLoader color={CHAT_BLUE} size={10} />}
        className={`${prefixCls}-typing`}
      />
    </Message>
  );
};

export default Typing;
