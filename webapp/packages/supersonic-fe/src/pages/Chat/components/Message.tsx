import classNames from 'classnames';
import styles from './style.less';

type Props = {
  position: 'left' | 'right';
  width?: number | string;
  height?: number | string;
  bubbleClassName?: string;
  domainName?: string;
  question?: string;
  followQuestions?: string[];
};

const Message: React.FC<Props> = ({
  position,
  width,
  height,
  children,
  bubbleClassName,
  domainName,
  question,
  followQuestions,
}) => {
  const messageClass = classNames(styles.message, {
    [styles.left]: position === 'left',
    [styles.right]: position === 'right',
  });

  const leftTitle = question
    ? followQuestions && followQuestions.length > 0
      ? `多轮对话：${[question, ...followQuestions].join(' ← ')}`
      : `单轮对话：${question}`
    : '';

  return (
    <div className={messageClass} style={{ width }}>
      {/* <div className={styles.messageTitleBar}>
        {!!domainName && <div className={styles.domainName}>{domainName}</div>}
        {position === 'left' && leftTitle && (
          <div className={styles.messageTopBar} title={leftTitle}>
            ({leftTitle})
          </div>
        )}
      </div> */}
      <div className={styles.messageContent}>
        <div className={styles.messageBody}>
          <div
            className={`${styles.bubble}${bubbleClassName ? ` ${bubbleClassName}` : ''}`}
            style={{ height }}
            onClick={(e) => {
              e.stopPropagation();
            }}
          >
            {/* {position === 'left' && question && (
              <div className={styles.messageTopBar} title={leftTitle}>
                {leftTitle}
              </div>
            )} */}
            {children}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Message;
