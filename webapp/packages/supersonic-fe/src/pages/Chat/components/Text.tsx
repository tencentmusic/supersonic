import Message from './Message';
import styles from './style.less';

type Props = {
  position: 'left' | 'right';
  data: any;
  quote?: string;
};

const Text: React.FC<Props> = ({ position, data, quote }) => {
  return (
    <Message position={position} bubbleClassName={styles.textBubble}>
      {position === 'right' && quote && <div className={styles.quote}>{quote}</div>}
      <div className={styles.text}>{data}</div>
    </Message>
  );
};

export default Text;
