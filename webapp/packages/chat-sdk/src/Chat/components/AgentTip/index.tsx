import LeftAvatar from '../CopilotAvatar';
import Message from '../Message';
import styles from './style.module.less';
import { AgentType } from '../../type';
import { isMobile } from '../../../utils/utils';
import { useDataSetsInfo } from '../../../hooks/useDataSetsInfo';
import imgUrl from './data.png';
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
                  <img src={imgUrl} alt="" />
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
