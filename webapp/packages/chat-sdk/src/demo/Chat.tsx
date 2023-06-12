import { Input } from 'antd';
import styles from './style.module.less';
import { useState } from 'react';
import ChatItem from '../components/ChatItem';
import { queryContext, searchRecommend } from '../service';

const { Search } = Input;

const Chat = () => {
  const [inputMsg, setInputMsg] = useState('');
  const [msg, setMsg] = useState('');

  const onInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    setInputMsg(value);
    searchRecommend(value);
  };

  const onSearch = () => {
    setMsg(inputMsg);
    queryContext(inputMsg);
  };

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
      <div className={styles.chatItem}>
        <ChatItem msg={msg} suggestionEnable isLastMessage />
      </div>
    </div>
  );
};

export default Chat;
