import { uuid } from '@/utils/utils';
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import { Button, Empty, Popconfirm, Space, Switch, Tag } from 'antd';
import { useState } from 'react';
import styles from './style.less';
import ToolModal from './ToolModal';
import { AgentToolType, AgentType, AGENT_TOOL_TYPE_LIST } from './type';

type Props = {
  currentAgent?: AgentType;
  onSaveAgent: (agent: AgentType, noTip?: boolean) => Promise<void>;
  onEditAgent: (agent?: AgentType) => void;
  goBack: () => void;
};

const ToolsSection: React.FC<Props> = ({ currentAgent, onSaveAgent, onEditAgent, goBack }) => {
  const [modalVisible, setModalVisible] = useState(false);
  const [editTool, setEditTool] = useState<AgentToolType>();

  const agentConfig = currentAgent?.agentConfig ? JSON.parse(currentAgent.agentConfig as any) : {};

  const saveAgent = async (agent: AgentType) => {
    await onSaveAgent(agent);
    setModalVisible(false);
  };

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
    await saveAgent({
      ...currentAgent,
      agentConfig: JSON.stringify(newAgentConfig) as any,
    });
    setModalVisible(false);
  };

  const onDeleteTool = async (tool: AgentToolType) => {
    const newAgentConfig = agentConfig || ({} as any);
    if (!newAgentConfig.tools) {
      newAgentConfig.tools = [];
    }
    newAgentConfig.tools = newAgentConfig.tools.filter(
      (item: AgentToolType) => item.id !== tool.id,
    );
    await saveAgent({
      ...currentAgent,
      agentConfig: JSON.stringify(newAgentConfig) as any,
    });
  };

  return (
    <div className={styles.toolsSection}>
      <div className={styles.toolsSectionTitleBar}>
        <ArrowLeftOutlined className={styles.backIcon} onClick={goBack} />
        <div className={styles.agentTitle}>{currentAgent?.name}</div>
        <div className={styles.toggleStatus}>
          {currentAgent?.status === 0 ? '已禁用' : <span className={styles.online}>已启用</span>}
          <span
            onClick={(e) => {
              e.stopPropagation();
            }}
          >
            <Switch
              size="small"
              defaultChecked={currentAgent?.status === 1}
              onChange={(value) => {
                onSaveAgent({ ...currentAgent, status: value ? 1 : 0 }, true);
              }}
            />
          </span>
        </div>
      </div>
      <div className={styles.basicInfo}>
        <div className={styles.basicInfoTitle}>
          基本信息
          <Button
            type="primary"
            onClick={() => {
              onEditAgent(currentAgent);
            }}
          >
            修改基本信息
          </Button>
        </div>
        <div className={styles.infoContent}>
          <div className={styles.infoItem}>
            示例问题：
            <Space>
              {currentAgent?.examples?.map((item) => (
                <Tag key={item}>{item}</Tag>
              ))}
            </Space>
          </div>
          <div className={styles.infoItem}>描述：{currentAgent?.description}</div>
        </div>
      </div>
      <div className={styles.toolSection}>
        <div className={styles.toolSectionTitleBar}>
          <div className={styles.toolSectionTitle}>工具</div>
          <Button
            type="primary"
            onClick={() => {
              setEditTool(undefined);
              setModalVisible(true);
            }}
          >
            <PlusOutlined /> 新增工具
          </Button>
        </div>
        {agentConfig?.tools && agentConfig?.tools?.length > 0 ? (
          <div className={styles.toolsContent}>
            {agentConfig.tools.map((tool: AgentToolType) => {
              const toolType = AGENT_TOOL_TYPE_LIST.find((item) => item.value === tool.type)?.label;
              return (
                <div
                  className={styles.toolItem}
                  key={tool.id}
                  onClick={() => {
                    setEditTool(tool);
                    setModalVisible(true);
                  }}
                >
                  <ToolOutlined className={styles.toolIcon} />
                  <div className={styles.toolContent}>
                    <div className={styles.toolTopSection}>
                      <div className={styles.toolType}>{tool.name || toolType}</div>
                      <div className={styles.toolOperateIcons}>
                        <EditOutlined
                          className={styles.toolOperateIcon}
                          onClick={(e) => {
                            e.stopPropagation();
                            setEditTool(tool);
                            setModalVisible(true);
                          }}
                        />
                        <Popconfirm
                          title="确定删除吗？"
                          onCancel={(e) => {
                            e?.stopPropagation();
                          }}
                          onConfirm={(e) => {
                            e?.stopPropagation();
                            onDeleteTool(tool);
                          }}
                        >
                          <DeleteOutlined
                            className={styles.toolOperateIcon}
                            onClick={(e) => {
                              e.stopPropagation();
                            }}
                          />
                        </Popconfirm>
                      </div>
                    </div>
                    <div className={styles.toolDesc} title={toolType}>
                      {toolType}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className={styles.emptyHolder}>
            <Empty description={`【${currentAgent?.name}】暂无工具，请新增工具`} />
          </div>
        )}
      </div>
      {modalVisible && (
        <ToolModal
          editTool={editTool}
          onSaveTool={onSaveTool}
          onCancel={() => {
            setModalVisible(false);
          }}
        />
      )}
    </div>
  );
};

export default ToolsSection;
