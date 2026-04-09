import { Card, Col, Row, Statistic, message } from 'antd';
import {
  RobotOutlined,
  ToolOutlined,
  CheckCircleOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons';
import { useEffect, useState } from 'react';
import AgentsSection from './AgentsSection';
import { uuid, jsonParse } from '@/utils/utils';
import { StatusEnum } from '@/common/constants';
import { MSG } from '@/common/messages';
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
  const [toolConfig, setAgentConfig] = useState<any>({});

  useEffect(() => {
    const config = jsonParse(currentAgent?.toolConfig, {});
    setAgentConfig(config);
  }, [currentAgent]);

  const updateData = async () => {
    setLoading(true);
    const res = await getAgentList();
    setLoading(false);
    setAgents(res.data || []);
  };

  useEffect(() => {
    updateData();
  }, []);

  const onSaveTool = async (tool: AgentToolType) => {
    const newAgentConfig = toolConfig || ({} as any);
    if (!newAgentConfig.tools) {
      newAgentConfig.tools = [];
    }
    if (tool.id) {
      const index = newAgentConfig.tools.findIndex((item: AgentToolType) => item.id === tool.id);
      newAgentConfig.tools[index] = tool;
    } else {
      newAgentConfig.tools.push({ ...tool, id: uuid() });
    }
    setAgentConfig(newAgentConfig);
    if (!currentAgent?.id) {
      setCurrentAgent({
        ...currentAgent,
        toolConfig: JSON.stringify(newAgentConfig),
      } as AgentType);
      setModalVisible(false);
      return;
    }
    await onSaveAgent({
      ...currentAgent,
      toolConfig: JSON.stringify(newAgentConfig) as any,
    });
    setModalVisible(false);
  };

  const onSaveAgent = async (agent: AgentType, noTip?: boolean) => {
    const { data, code } = await saveAgent(agent);
    if (code === 200) {
      setCurrentAgent({
        ...data,
      });
      updateData();
    }
    if (!noTip) {
      message.success(MSG.SAVE_SUCCESS);
    }
  };

  const onDeleteAgent = async (id: number) => {
    await deleteAgent(id);
    message.success(MSG.DELETE_SUCCESS);
    updateData();
  };

  const enabledCount = agents.filter((agent) => agent.status === StatusEnum.ENABLED).length;
  const disabledCount = agents.filter((agent) => agent.status === StatusEnum.DISABLED).length;
  const toolsCount = agents.reduce((count, agent) => {
    const config = jsonParse(agent.toolConfig, {});
    return count + (config?.tools?.length || 0);
  }, 0);

  return (
    <div className={styles.agent}>
      {!showDetail ? (
        <div className={styles.agentOverview}>
          <Card className={styles.agentHeroCard}>
            <Row gutter={16}>
              <Col span={6}>
                <Card className={styles.agentSummaryCard}>
                  <Statistic title="助理总数" value={agents.length} prefix={<RobotOutlined />} />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.agentSummaryCard}>
                  <Statistic title="已启用" value={enabledCount} prefix={<CheckCircleOutlined />} />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.agentSummaryCard}>
                  <Statistic title="已停用" value={disabledCount} prefix={<PauseCircleOutlined />} />
                </Card>
              </Col>
              <Col span={6}>
                <Card className={styles.agentSummaryCard}>
                  <Statistic title="已配置工具" value={toolsCount} prefix={<ToolOutlined />} />
                </Card>
              </Col>
            </Row>
          </Card>
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
        </div>
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
