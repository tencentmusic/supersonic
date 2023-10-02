import { useEffect, useState } from 'react';
import LeftAvatar from '../CopilotAvatar';
import Message from '../Message';
import styles from './style.module.less';
import { queryRecommendQuestions } from '../../service';
import { isMobile } from '../../../utils/utils';

type Props = {
  onSelectQuestion: (value: string) => void;
};

const RecommendQuestions: React.FC<Props> = ({ onSelectQuestion }) => {
  const [questions, setQuestions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const initData = async () => {
    setLoading(true);
    const res = await queryRecommendQuestions();
    setLoading(false);
    setQuestions(
      res.data?.reduce((result: any[], item: any) => {
        result = [
          ...result,
          ...item.recommendedQuestions.slice(0, 20).map((item: any) => item.question),
        ];
        return result;
      }, []) || []
    );
  };

  useEffect(() => {
    initData();
  }, []);

  return (
    <div className={styles.recommendQuestions}>
      {!isMobile && <LeftAvatar />}
      {loading ? (
        <></>
      ) : questions.length > 0 ? (
        <Message position="left" bubbleClassName={styles.recommendQuestionsMsg}>
          <div className={styles.title}>推荐问题：</div>
          <div className={styles.content}>
            {questions.map((question, index) => (
              <div
                key={`${question}_${index}`}
                className={styles.question}
                onClick={() => {
                  onSelectQuestion(question);
                }}
              >
                {question}
              </div>
            ))}
          </div>
        </Message>
      ) : (
        <Message position="left">您好，请问有什么我可以帮您吗？</Message>
      )}
    </div>
  );
};

export default RecommendQuestions;
