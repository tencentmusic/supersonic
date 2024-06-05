import Chat from '../Chat';
import styles from './style.module.less';

type Props = {};

const ChatDemo: React.FC<Props> = ({}) => {
  return (
    <div className={styles.chatDemo}>
      <Chat isDeveloper token='eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl91c2VyX2lkIjo4ODcsInRva2VuX3VzZXJfZGlzcGxheV9uYW1lIjoi5pa55rSLIiwidG9rZW5fY3JlYXRlX3RpbWUiOjE3MTc1Njc0MzkwMjEsInN1YiI6ImZhbmd5YW5nIiwidG9rZW5faXNfYWRtaW4iOjEsInRva2VuX3VzZXJfbmFtZSI6ImZhbmd5YW5nIiwiZXhwIjoxNzE3NTc0NjM5LCJ0b2tlbl91c2VyX3Bhc3N3b3JkIjoiZmFuZ3lhbmcifQ.TJI_YaTv_vmHg_Xl09nlj2eDP-1j6hjruJu-qqKelYFdhBaO3F-1mKAn4zyjhZk7OsR261F9LDrxk96bz_qBAw' />
    </div>
  );
};

export default ChatDemo;
