import LeftAvatar from '../CopilotAvatar';
import Message from '../Message';
import styles from './style.module.less';
import { AgentType } from '../../type';
import { isMobile } from '../../../utils/utils';
import { useDataSetsInfo } from '../../../hooks/useDataSetsInfo';
import { Modal, Table } from 'antd';
import { useMemo, useState } from 'react';

type Props = {
  currentAgent?: AgentType;
  onSendMsg: (value: string) => void;
};

const AgentTip: React.FC<Props> = ({ currentAgent, onSendMsg }) => {
  const dataSets = useDataSetsInfo();
  const [currentId, setCurrentId] = useState<number | null>(null);
  const currentDataSet = useMemo(() => {
    return currentId ? dataSets.get(currentId) : null;
  }, [currentId, dataSets]);
  const [open, setOpen] = useState(false);

  const dataSetArray = [...dataSets.values()];

  const columns = useMemo(
    () => [
      {
        dataIndex: 'name',
        title: '字段名称',
        width: '40%',
        ellipsis: true,
      },
      {
        dataIndex: 'description',
        title: '字段含义',
        ellipsis: true,
        width: '60%',
      },
    ],
    []
  );

  const dataSource = useMemo(() => {
    if (!currentDataSet) {
      return [];
    }
    return [...currentDataSet.dimensions, ...currentDataSet.metrics].map(field => ({
      key: field.bizName,
      name: field.name,
      description: field.description,
    }));
  }, [currentDataSet]);

  const handleClick = (id: number) => {
    setCurrentId(id);
    setOpen(true);
  };

  if (!currentAgent) {
    return null;
  }

  return (
    <div className={styles.agentTip}>
      {!isMobile && <LeftAvatar />}
      <Message position="left" bubbleClassName={styles.agentTipMsg}>
        <div className={styles.title}>
          您好，我是您的数据分析AI助理，我可以帮您分析各类数据，生成图标，数据导出等事情，提高您数据分析等效率，以下是我能够查询的数据范围：
        </div>
        <div className={styles.dataSetContainer}>
          {dataSetArray.map((dataSet, idx) => (
            <div className={styles.dataSet} key={idx}>
              <div className={styles.dataSetName}>
                <div className={styles.imgContainer}>
                  <img
                    src="data:image/png;base64,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"
                    alt=""
                  ></img>
                </div>
                <div className={styles.spanContanier}>
                  <div onClick={() => handleClick(dataSet.id)}>{dataSet.name} &gt;</div>
                </div>
              </div>
              <div title={dataSet.description ?? '暂无描述'} className={styles.dataSetDescription}>
                {dataSet.description ?? '暂无描述'}
              </div>
            </div>
          ))}
        </div>
        <div className={styles.title}>可以通过以下的内容尝试发起提问：</div>
        <div className={styles.content}>
          <div className={styles.examples}>
            {currentAgent.examples?.length > 0 ? (
              currentAgent.examples.map(example => (
                <div
                  key={example}
                  className={styles.example}
                  onClick={() => {
                    onSendMsg(example);
                  }}
                >
                  &ldquo;{example}&rdquo;
                </div>
              ))
            ) : (
              <div className={styles.example}>{currentAgent.description}</div>
            )}
          </div>
        </div>
      </Message>

      <Modal
        title={currentDataSet?.name ?? '数据集'}
        open={open}
        width={800}
        onCancel={() => setOpen(false)}
        footer={null}
      >
        <Table columns={columns} dataSource={dataSource} />
      </Modal>
    </div>
  );
};

export default AgentTip;
