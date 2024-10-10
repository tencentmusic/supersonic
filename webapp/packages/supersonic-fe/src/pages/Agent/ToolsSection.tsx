import { uuid } from '@/utils/utils';
import { DeleteOutlined, EditOutlined, ToolOutlined } from '@ant-design/icons';
import { Empty, Popconfirm, message } from 'antd';
import { useState, useEffect } from 'react';
import styles from './style.less';
import ToolModal from './ToolModal';
import { getToolTypes } from './service';
import { AgentToolType, AgentType } from './type';

type Props = {
  currentAgent?: AgentType;
  onSaveAgent: (agent: AgentType, noTip?: boolean) => Promise<void>;
};

const ToolsSection: React.FC<Props> = ({ currentAgent, onSaveAgent }) => {
  const [modalVisible, setModalVisible] = useState(false);
  const [editTool, setEditTool] = useState<AgentToolType>();

  const [toolTypesOptions, setToolTypesOptions] = useState<OptionsItem[]>([]);

  useEffect(() => {
    queryToolTypes();
  }, []);

  const toolConfig = currentAgent?.toolConfig ? JSON.parse(currentAgent.toolConfig as any) : {};

  const saveAgent = async (agent: AgentType) => {
    await onSaveAgent(agent);
    setModalVisible(false);
  };

  const queryToolTypes = async () => {
    const { code, data } = await getToolTypes();
    if (code === 200 && data) {
      const options = Object.keys(data).map((key: string) => {
        return {
          label: data[key],
          value: key,
        };
      });
      setToolTypesOptions(options);
    } else {
      message.error('获取工具类型失败');
    }
  };

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
    await saveAgent({
      ...currentAgent,
      toolConfig: JSON.stringify(newAgentConfig) as any,
    });
    setModalVisible(false);
  };

  const onDeleteTool = async (tool: AgentToolType) => {
    const newAgentConfig = toolConfig || ({} as any);
    if (!newAgentConfig.tools) {
      newAgentConfig.tools = [];
    }
    newAgentConfig.tools = newAgentConfig.tools.filter(
      (item: AgentToolType) => item.id !== tool.id,
    );
    await saveAgent({
      ...currentAgent,
      toolConfig: JSON.stringify(newAgentConfig) as any,
    });
  };

  return (
    <>
      <div className={styles.toolSection}>
        {toolConfig?.tools && toolConfig?.tools?.length > 0 ? (
          <div className={styles.toolsContent}>
            {toolConfig.tools.map((tool: AgentToolType) => {
              const toolType = toolTypesOptions.find((item) => item.value === tool.type)?.label;
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
            <Empty
              description={`${
                currentAgent?.name ? `【${currentAgent?.name}】` : ''
              }暂无工具，请新增工具`}
            />
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
    </>
  );
};

export default ToolsSection;
