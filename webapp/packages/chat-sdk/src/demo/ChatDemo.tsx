import Chat from '../Chat';
import styles from './style.module.less';

type Props = {};

const ChatDemo: React.FC<Props> = () => {
  return (
    <div className={styles.chatDemo}>
      <Chat token="eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlbl9pc19hZG1pbiI6MSwidG9rZW5fdXNlcl9pZCI6ODg3LCJ0b2tlbl91c2VyX25hbWUiOiJmYW5neWFuZyIsInRva2VuX3VzZXJfZGlzcGxheV9uYW1lIjoi5pa55rSLIiwidG9rZW5fY3JlYXRlX3RpbWUiOjE3MjAxNzM2OTc1NDEsInRva2VuX3VzZXJfcGFzc3dvcmQiOiJjM1Z3WlhKemIyNXBZMEJpYVdOdmJiZ3RMcVRoVVAyMXhHTVN1QS9nbXpDUDQyei8vTEk3TGtmTmF1cWlHRWpZIiwic3ViIjoiZmFuZ3lhbmciLCJleHAiOjE3OTIxNzM2OTd9.C3BdA5IR0OYv1tK3BxVEbxGhEEaJk0qU--54vUHMOgmIhVkWqvKKkQI9GXIOpRqhFkH4gSnyQkM0sigh2Ibmqg" />
    </div>
  );
};

export default ChatDemo;
