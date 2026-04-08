import { useState } from 'react';
import { useLocation } from '@umijs/max';
import { getToken } from '@/utils/utils';
import queryString from 'query-string';
import { Chat } from 'supersonic-chat-sdk';
import AgentCreateModal from '@/pages/Agent/AgentCreateModal';
import './index.less';

const ChatPage = () => {
  const location = useLocation();
  const query = queryString.parse(location.search) || {};
  const { agentId } = query;
  const [createModalVisible, setCreateModalVisible] = useState(false);

  const handleAddAgent = () => {
    setCreateModalVisible(true);
  };

  const handleCreateSuccess = () => {
    // 刷新页面以加载新创建的助理
    window.location.reload();
  };

  return (
    <div className="chat-page-shell">
      <div className="chat-page-shell-main">
        <Chat
          initialAgentId={agentId ? +agentId : undefined}
          token={getToken() || ''}
          isDeveloper
          onAddAgent={handleAddAgent}
        />
      </div>
      <AgentCreateModal
        visible={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        onSuccess={handleCreateSuccess}
      />
    </div>
  );
};

export default ChatPage;
