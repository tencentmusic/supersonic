import { message } from 'antd';
import { useEffect, useState } from 'react';
import AgentsSection from './AgentsSection';
import AgentModal from './AgentModal';
import { deleteAgent, getAgentList, saveAgent } from './service';
import styles from './style.less';
import ToolsSection from './ToolsSection';
import { AgentType } from './type';

const Agent = () => {
  const [agents, setAgents] = useState<AgentType[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentAgent, setCurrentAgent] = useState<AgentType>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editAgent, setEditAgent] = useState<AgentType>();

  const updateData = async () => {
    setLoading(true);
    const res = await getAgentList();
    setLoading(false);
    setAgents(res.data || []);
    if (!res.data?.length) {
      return;
    }
    if (currentAgent) {
      const agent = res.data.find((item) => item.id === currentAgent.id);
      if (agent) {
        setCurrentAgent(agent);
      } else {
        setCurrentAgent(res.data[0]);
      }
    }
  };

  useEffect(() => {
    updateData();
  }, []);

  const onSaveAgent = async (agent: AgentType, noTip?: boolean) => {
    await saveAgent(agent);
    if (!noTip) {
      message.success('保存成功');
    }
    setModalVisible(false);
    updateData();
  };

  const onDeleteAgent = async (id: number) => {
    await deleteAgent(id);
    message.success('删除成功');
    updateData();
  };

  const onEditAgent = (agent?: AgentType) => {
    setEditAgent(agent);
    setModalVisible(true);
  };

  return (
    <div className={styles.agent}>
      {!currentAgent ? (
        <AgentsSection
          agents={agents}
          currentAgent={currentAgent}
          loading={loading}
          onSelectAgent={setCurrentAgent}
          onEditAgent={onEditAgent}
          onDeleteAgent={onDeleteAgent}
          onSaveAgent={onSaveAgent}
        />
      ) : (
        <ToolsSection
          currentAgent={currentAgent}
          onSaveAgent={onSaveAgent}
          onEditAgent={onEditAgent}
          goBack={() => {
            setCurrentAgent(undefined);
          }}
        />
      )}
      {modalVisible && (
        <AgentModal
          editAgent={editAgent}
          onSaveAgent={onSaveAgent}
          onCancel={() => {
            setModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default Agent;
