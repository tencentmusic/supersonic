import { CHAT_BLUE } from '@/common/constants';
import { Spin } from 'antd';
import BeatLoader from 'react-spinners/BeatLoader';
import Message from './Message';
import styles from './style.less';

const Typing = () => {
  return (
    <Message position="left" bubbleClassName={styles.typingBubble}>
      <Spin
        spinning={true}
        indicator={<BeatLoader color={CHAT_BLUE} size={10} />}
        className={styles.typing}
      />
    </Message>
  );
};

export default Typing;
