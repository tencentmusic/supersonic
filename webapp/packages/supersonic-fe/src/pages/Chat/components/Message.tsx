import classNames from 'classnames';
import styles from './style.less';

type Props = {
  position: 'left' | 'right';
  bubbleClassName?: string;
  aggregator?: string;
  noTime?: boolean;
};

const Message: React.FC<Props> = ({ position, children, bubbleClassName }) => {
  const messageClass = classNames(styles.message, {
    [styles.left]: position === 'left',
    [styles.right]: position === 'right',
  });

  return (
    <div className={messageClass}>
      <div className={styles.messageContent}>
        <div className={styles.messageBody}>
          <div
            className={`${styles.bubble}${bubbleClassName ? ` ${bubbleClassName}` : ''}`}
            onClick={(e) => {
              e.stopPropagation();
            }}
          >
            {children}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Message;
