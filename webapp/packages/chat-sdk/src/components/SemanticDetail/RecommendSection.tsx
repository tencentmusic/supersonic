import { useEffect, useState } from 'react';
import { getMetricQueryInfo } from '../../service';
import { message, Row, Col } from 'antd';
import { CLS_PREFIX } from '../../common/constants';

type Props = {
  classId: number;
  metricName: string;
  onSelect?: (value: string) => void;
};

const RecommendQuestions: React.FC<Props> = ({ classId, metricName, onSelect }) => {
  const [moreMode, setMoreMode] = useState<boolean>(false);
  const [questionData, setQuestionData] = useState<any[]>([]);

  const prefixCls = `${CLS_PREFIX}-semantic-detail`;

  const queryMetricQueryInfo = async () => {
    const { data: resData } = await getMetricQueryInfo({
      classId,
      metricName,
    });
    const { code, data, msg } = resData;
    if (code === '0') {
      setQuestionData(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryMetricQueryInfo();
  }, []);

  return (
    <div className={`${prefixCls}-recommend-questions`}>
      <div className={`${prefixCls}-header`}>
        <Row>
          <Col flex="0 0 85px">
            <div className={`${prefixCls}-label`}> 大家都在问: </div>
          </Col>
          <Col flex="1 1" className={`${prefixCls}-content-col`}>
            {!moreMode && (
              <div className={`${prefixCls}-content-col-box`}>
                {questionData.slice(0, 5).map((item, index) => {
                  const { question } = item;
                  return (
                    <>
                      {index !== 0 && '、'}
                      <span
                        key={question}
                        className={`${prefixCls}-question`}
                        onClick={() => {
                          onSelect?.(question);
                        }}
                      >
                        <span dangerouslySetInnerHTML={{ __html: question }} />
                      </span>
                    </>
                  );
                })}
              </div>
            )}
          </Col>
          <Col flex="0 1 30px">
            {!moreMode ? (
              <span
                onClick={() => {
                  setMoreMode(true);
                }}
                className={`${prefixCls}-more`}
              >
                更多
              </span>
            ) : (
              <span
                className={`${prefixCls}-more`}
                onClick={() => {
                  setMoreMode(false);
                }}
              >
                收起
              </span>
            )}
          </Col>
        </Row>
      </div>
      {moreMode && (
        <div className={`${prefixCls}-recommend-questions-content`}>
          <div className={`${prefixCls}-questions`}>
            {questionData.map(item => {
              const { question } = item;
              return (
                <div
                  key={question}
                  className={`${prefixCls}-question`}
                  onClick={() => {
                    onSelect?.(question);
                  }}
                >
                  <span dangerouslySetInnerHTML={{ __html: question }} />
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

export default RecommendQuestions;
