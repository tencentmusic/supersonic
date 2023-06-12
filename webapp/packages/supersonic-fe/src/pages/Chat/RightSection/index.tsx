import classNames from 'classnames';
import Context from './Context';
import Introduction from './Introduction';
import styles from './style.less';
import type { MsgDataType } from 'supersonic-chat-sdk';

type Props = {
  currentEntity?: MsgDataType;
};

const RightSection: React.FC<Props> = ({ currentEntity }) => {
  const rightSectionClass = classNames(styles.rightSection, {
    [styles.external]: true,
  });

  return (
    <div className={rightSectionClass}>
      {currentEntity && (
        <div className={styles.entityInfo}>
          {currentEntity?.chatContext && <Context chatContext={currentEntity.chatContext} />}
          <Introduction currentEntity={currentEntity} />
        </div>
      )}
    </div>
  );
};

export default RightSection;
