import { isMobile } from '../../utils/utils';
import { Avatar } from 'antd';
import classNames from 'classnames';
import LeftAvatar from './CopilotAvatar';
import Message from './Message';
import styles from './style.module.less';

type Props = {
  position: 'left' | 'right';
  data: any;
  quote?: string;
};

const Text: React.FC<Props> = ({ position, data, quote }) => {
  const textWrapperClass = classNames(styles.textWrapper, {
    [styles.rightTextWrapper]: position === 'right',
  });
  return (
    <div className={textWrapperClass}>
      {!isMobile && position === 'left' && <LeftAvatar />}
      <Message position={position} bubbleClassName={styles.textBubble}>
        {position === 'right' && quote && <div className={styles.quote}>{quote}</div>}
        <div className={styles.text}>{data}</div>
      </Message>
    </div>
  );
};

export default Text;
