import classNames from 'classnames';
import styles from './style.less';

type Props = {
  position: 'left' | 'right';
  width?: number | string;
  height?: number | string;
  bubbleClassName?: string;
};

const Message: React.FC<Props> = ({ position, width, height, children, bubbleClassName }) => {
  const messageClass = classNames(styles.message, {
    [styles.left]: position === 'left',
    [styles.right]: position === 'right',
  });

  return (
    <div className={messageClass} style={{ width }}>
      <div className={styles.messageContent}>
        <div className={styles.messageBody}>
          <div
            className={`${styles.bubble}${bubbleClassName ? ` ${bubbleClassName}` : ''}`}
            style={{ height }}
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
