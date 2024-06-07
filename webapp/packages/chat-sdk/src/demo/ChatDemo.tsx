import Chat from '../Chat';
import styles from './style.module.less';

type Props = {};

const ChatDemo: React.FC<Props> = ({}) => {
  return (
    <div className={styles.chatDemo}>
      <Chat isDeveloper token='eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl91c2VyX2lkIjo4ODcsInRva2VuX3VzZXJfZGlzcGxheV9uYW1lIjoi5pa55rSLIiwidG9rZW5fY3JlYXRlX3RpbWUiOjE3MTc3MjA3NTA2NTIsInN1YiI6ImZhbmd5YW5nIiwidG9rZW5faXNfYWRtaW4iOjEsInRva2VuX3VzZXJfbmFtZSI6ImZhbmd5YW5nIiwiZXhwIjoxNzE3NzQyMzUwLCJ0b2tlbl91c2VyX3Bhc3N3b3JkIjoiZmFuZ3lhbmcifQ.m8tWb7NDEynuV6IUmF86_yTU0fW_UOF_9_Gdh9flajWmc_KnKKpYQWGBypuaKVWs1afl2zYXwpToRULo-qBc8w' />
    </div>
  );
};

export default ChatDemo;
