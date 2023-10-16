import { Button, Space } from 'antd';
import styles from './style.module.less';
import Copilot from '../Copilot';
import { useRef } from 'react';

const buttonParams = [
  {
    msg: '周杰伦 艺人趋势解读',
    agentId: 8,
    modelId: 23,
    filters: [{ bizName: 'singer_id', elementID: 283, value: 4558 }],
  },
  {
    msg: '林俊杰 艺人趋势解读',
    agentId: 8,
    modelId: 23,
    filters: [{ bizName: 'singer_id', elementID: 283, value: 4286 }],
  },
];

const CopilotDemo = () => {
  const copilotRef = useRef<any>();

  return (
    <div className={styles.copilotDemo}>
      <Space>
        {buttonParams.map(params => (
          <Button
            key={params.msg}
            onClick={() => {
              copilotRef?.current?.sendCopilotMsg(params);
            }}
          >
            {params.msg}
          </Button>
        ))}
      </Space>
      <Copilot isDeveloper ref={copilotRef} />
    </div>
  );
};

export default CopilotDemo;
