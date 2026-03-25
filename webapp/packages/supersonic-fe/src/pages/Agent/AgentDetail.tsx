import { ArrowLeftOutlined } from '@ant-design/icons';
import styles from './style.less';
import { AgentType } from './type';
import AgentForm from './AgentForm';

type Props = {
  currentAgent?: AgentType;
  onSaveAgent: (agent: AgentType, noTip?: boolean) => Promise<void>;
  onCreateToolBtnClick?: () => void;
  goBack: () => void;
};

const ToolsSection: React.FC<Props> = ({
  currentAgent,
  onSaveAgent,
  onCreateToolBtnClick,
  goBack,
}) => {
  return (
    <div className={styles.toolsSection}>
      <div className={styles.toolsSectionTitleBar}>
        <ArrowLeftOutlined className={styles.backIcon} onClick={goBack} />
        <div className={styles.agentTitleBlock}>
          <div className={styles.agentTitle}>{currentAgent?.name || '新建助理'}</div>
        </div>
      </div>
      <div className={styles.basicInfo}>
        <AgentForm
          onSaveAgent={onSaveAgent}
          editAgent={currentAgent}
          onCreateToolBtnClick={onCreateToolBtnClick}
        />
      </div>
    </div>
  );
};

export default ToolsSection;
