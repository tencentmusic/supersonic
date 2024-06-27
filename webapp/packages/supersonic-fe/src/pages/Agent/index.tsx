import { message } from 'antd';
import { useEffect, useState } from 'react';
import AgentsSection from './AgentsSection';
import { uuid, jsonParse } from '@/utils/utils';
import { deleteAgent, getAgentList, saveAgent } from './service';
import styles from './style.less';
import ToolModal from './ToolModal';
import AgentDetail from './AgentDetail';
import { AgentToolType, AgentType } from './type';

const Agent = () => {
  const [agents, setAgents] = useState<AgentType[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentAgent, setCurrentAgent] = useState<AgentType>();
  const [modalVisible, setModalVisible] = useState(false);
  const [showDetail, setShowDetail] = useState<boolean>(false);
  const [agentConfig, setAgentConfig] = useState<any>({});

  useEffect(() => {
    const config = jsonParse(currentAgent?.agentConfig, {});
    setAgentConfig(config);
  }, [currentAgent]);

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

  const onSaveTool = async (tool: AgentToolType) => {
    const newAgentConfig = agentConfig || ({} as any);
    if (!newAgentConfig.tools) {
      newAgentConfig.tools = [];
    }
    if (tool.id) {
      const index = newAgentConfig.tools.findIndex((item: AgentToolType) => item.id === tool.id);
      newAgentConfig.tools[index] = tool;
    } else {
      newAgentConfig.tools.push({ ...tool, id: uuid() });
    }
    await onSaveAgent({
      ...currentAgent,
      agentConfig: JSON.stringify(newAgentConfig) as any,
    });
    setModalVisible(false);
  };

  const onSaveAgent = async (agent: AgentType, noTip?: boolean) => {
    await saveAgent(agent);
    if (!noTip) {
      message.success('保存成功');
    }
    updateData();
  };

  const onDeleteAgent = async (id: number) => {
    await deleteAgent(id);
    message.success('删除成功');
    updateData();
  };

  return (
    <div className={styles.agent}>
      {!showDetail ? (
        <AgentsSection
          agents={agents}
          loading={loading}
          onSelectAgent={(agent) => {
            setCurrentAgent(agent);
            setShowDetail(true);
          }}
          onDeleteAgent={onDeleteAgent}
          onSaveAgent={onSaveAgent}
          onCreatBtnClick={() => {
            setCurrentAgent(undefined);
            setShowDetail(true);
          }}
        />
      ) : (
        <AgentDetail
          currentAgent={currentAgent}
          onSaveAgent={onSaveAgent}
          onCreateToolBtnClick={() => {
            setModalVisible(true);
          }}
          goBack={() => {
            setShowDetail(false);
            setCurrentAgent(undefined);
          }}
        />
      )}
      {modalVisible && (
        <ToolModal
          onSaveTool={onSaveTool}
          onCancel={() => {
            setModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default Agent;
