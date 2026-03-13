import { ArrowLeftOutlined } from '@ant-design/icons';
import { Switch } from 'antd';
import { StatusEnum } from '@/common/constants';
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
        <div className={styles.agentTitle}>{currentAgent?.name}</div>
        <div className={styles.toggleStatus}>
          {currentAgent?.status === StatusEnum.DISABLED ? '已禁用' : <span className={styles.online}>已启用</span>}
          <span
            onClick={(e) => {
              e.stopPropagation();
            }}
          >
            <Switch
              size="small"
              defaultChecked={currentAgent?.status === StatusEnum.ENABLED}
              onChange={(value) => {
                onSaveAgent({ ...currentAgent, status: value ? StatusEnum.ENABLED : StatusEnum.DISABLED }, true);
              }}
            />
          </span>
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
