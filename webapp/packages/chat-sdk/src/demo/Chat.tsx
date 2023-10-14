import { Input } from 'antd';
import styles from './style.module.less';
import { useEffect, useState } from 'react';
import ChatItem from '../components/ChatItem';
import { MsgDataType } from '../common/type';

const { Search } = Input;

const Chat = () => {
  const [inputMsg, setInputMsg] = useState('');
  const [msg, setMsg] = useState('');
  const [triggerResize, setTriggerResize] = useState(false);
  const [chatItemVisible, setChatItemVisible] = useState(false);

  const onWindowResize = () => {
    setTriggerResize(true);
    setTimeout(() => {
      setTriggerResize(false);
    }, 0);
  };

  useEffect(() => {
    window.addEventListener('resize', onWindowResize);
    return () => {
      window.removeEventListener('resize', onWindowResize);
    };
  }, []);

  const onInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    setInputMsg(value);
  };

  const onSearch = () => {
    setMsg(inputMsg);
    setChatItemVisible(false);
    setTimeout(() => {
      setChatItemVisible(true);
    }, 200);
  };

  const onMsgDataLoaded = (msgData: MsgDataType) => {};

  //预发环境： 5: 查信息，6: 智能圈选，12：问指标，15：歌曲库，16：艺人库

  return (
    <div className={styles.page}>
      <div className={styles.inputMsg}>
        <Search
          placeholder="请输入问题"
          value={inputMsg}
          onChange={onInputChange}
          onSearch={onSearch}
        />
      </div>
      {msg && chatItemVisible && (
        <div className={styles.chatItem}>
          <ChatItem
            msg={msg}
            agentId={5}
            conversationId={112211121}
            onMsgDataLoaded={onMsgDataLoaded}
            isLastMessage
            triggerResize={triggerResize}
            integrateSystem="wiki"
            isDeveloper
          />
        </div>
      )}
    </div>
  );
};

export default Chat;
