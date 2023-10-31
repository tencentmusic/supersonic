import { isMobile } from '../../utils/utils';
import { Avatar } from 'antd';
import classNames from 'classnames';
import LeftAvatar from './CopilotAvatar';
import Message from './Message';
import styles from './style.module.less';
import { userAvatarUrl } from '../../common/env';
import IconFont from '../../components/IconFont';

type Props = {
  position: 'left' | 'right';
  data: any;
  quote?: string;
  anonymousUser?: boolean;
};

const Text: React.FC<Props> = ({ position, data, quote, anonymousUser }) => {
  const textWrapperClass = classNames(styles.textWrapper, {
    [styles.rightTextWrapper]: position === 'right',
  });
  const rightAvatarUrl = userAvatarUrl;
  return (
    <div className={textWrapperClass}>
      {!isMobile && position === 'left' && <LeftAvatar />}
      <Message position={position} bubbleClassName={styles.textBubble}>
        {position === 'right' && quote && <div className={styles.quote}>{quote}</div>}
        <div className={styles.text}>{data}</div>
      </Message>
      {!isMobile && position === 'right' && rightAvatarUrl && (
        <Avatar
          shape="circle"
          size={40}
          src={anonymousUser ? undefined : rightAvatarUrl}
          icon={<IconFont type="icon-geren" />}
          className={styles.rightAvatar}
        />
      )}
    </div>
  );
};

export default Text;
