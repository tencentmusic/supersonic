import Chat from '../Chat';
import styles from './style.module.less';

type Props = {};

const ChatDemo: React.FC<Props> = ({}) => {
  return (
    <div className={styles.chatDemo}>
      <Chat isDeveloper token='eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl91c2VyX2lkIjo4ODcsInRva2VuX3VzZXJfZGlzcGxheV9uYW1lIjoi5pa55rSLIiwidG9rZW5fY3JlYXRlX3RpbWUiOjE3MTc2MzgzMDU3MjEsInN1YiI6ImZhbmd5YW5nIiwidG9rZW5faXNfYWRtaW4iOjEsInRva2VuX3VzZXJfbmFtZSI6ImZhbmd5YW5nIiwiZXhwIjoxNzE3NjU5OTA1LCJ0b2tlbl91c2VyX3Bhc3N3b3JkIjoiZmFuZ3lhbmcifQ.Kc_BdyE3YuDgwc_-WcraLHSdCjBIZIG3jBnBaKa_atWcInyMHR5u0WfpRx_AZInsVEUMMwVQvR0-CjqNMgU8FQ' />
    </div>
  );
};

export default ChatDemo;
