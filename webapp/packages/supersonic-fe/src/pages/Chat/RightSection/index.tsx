import classNames from 'classnames';
import Context from './Context';
import Introduction from './Introduction';
import styles from './style.less';
import type { MsgDataType } from 'supersonic-chat-sdk';
import Domains from './Domains';
import { ConversationDetailType, DomainType } from '../type';
import DomainInfo from './Context/DomainInfo';
import Conversation from './Conversation';

type Props = {
  domains: DomainType[];
  currentEntity?: MsgDataType;
  currentConversation?: ConversationDetailType;
  currentDomain?: DomainType;
  conversationRef: any;
  onSelectConversation: (conversation: ConversationDetailType, name?: string) => void;
  onSelectDomain: (domain: DomainType) => void;
};

const RightSection: React.FC<Props> = ({
  domains,
  currentEntity,
  currentDomain,
  currentConversation,
  conversationRef,
  onSelectConversation,
  onSelectDomain,
}) => {
  const rightSectionClass = classNames(styles.rightSection, {
    [styles.external]: false,
  });

  return (
    <div className={rightSectionClass}>
      <Conversation
        currentConversation={currentConversation}
        onSelectConversation={onSelectConversation}
        ref={conversationRef}
      />
      {currentDomain && !currentEntity && (
        <div className={styles.entityInfo}>
          <DomainInfo domain={currentDomain} />
        </div>
      )}
      {!!currentEntity?.chatContext?.domainId && (
        <div className={styles.entityInfo}>
          <Context chatContext={currentEntity.chatContext} entityInfo={currentEntity.entityInfo} />
          <Introduction currentEntity={currentEntity} />
        </div>
      )}
      {domains && domains.length > 0 && (
        <Domains domains={domains} currentDomain={currentDomain} onSelectDomain={onSelectDomain} />
      )}
    </div>
  );
};

export default RightSection;
