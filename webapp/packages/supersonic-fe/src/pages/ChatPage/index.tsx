import { useLocation } from '@umijs/max';
import { getToken } from '@/utils/utils';
import queryString from 'query-string';
import { Chat } from 'supersonic-chat-sdk';

const ChatPage = () => {
  const location = useLocation();
  const query = queryString.parse(location.search) || {};
  const { agentId } = query;

  return (
    <Chat initialAgentId={agentId ? +agentId : undefined} token={getToken() || ''} isDeveloper />
  );
};

export default ChatPage;
