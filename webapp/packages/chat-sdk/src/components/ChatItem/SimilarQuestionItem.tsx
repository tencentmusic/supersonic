import { CheckCircleFilled, DownOutlined, LoadingOutlined, UpOutlined } from '@ant-design/icons';
import { PREFIX_CLS } from '../../common/constants';
import { SimilarQuestionType } from '../../common/type';
import { useEffect, useState } from 'react';
import { querySimilarQuestions } from '../../service';

type Props = {
  queryId?: number;
  similarQueries?: SimilarQuestionType[];
  defaultExpanded?: boolean;
  onSelectQuestion: (question: SimilarQuestionType) => void;
};

const SimilarQuestions: React.FC<Props> = ({
  queryId,
  similarQueries,
  defaultExpanded,
  onSelectQuestion,
}) => {
  const [similarQuestions, setSimilarQuestions] = useState<SimilarQuestionType[]>(
    similarQueries || []
  );
  const [expanded, setExpanded] = useState(defaultExpanded || false);
  const [loading, setLoading] = useState(false);

  const tipPrefixCls = `${PREFIX_CLS}-item`;
  const prefixCls = `${PREFIX_CLS}-similar-questions`;

  const initData = async () => {
    setLoading(true);
    const res = await querySimilarQuestions(queryId!);
    setLoading(false);
    setSimilarQuestions(res.data?.similarQueries || []);
  };

  useEffect(() => {
    if (expanded && similarQuestions?.length === 0 && queryId) {
      initData();
    }
  }, [expanded, queryId]);

  const onToggleExpanded = () => {
    setExpanded(!expanded);
  };

  return (
    <div className={`${tipPrefixCls}-parse-tip`}>
      <div className={`${tipPrefixCls}-title-bar`}>
        <CheckCircleFilled className={`${tipPrefixCls}-step-icon`} />
        <div className={`${tipPrefixCls}-step-title`}>
          推荐相似问题
          <span className={`${prefixCls}-toggle-expand-btn`} onClick={onToggleExpanded}>
            {loading ? <LoadingOutlined /> : expanded ? <UpOutlined /> : <DownOutlined />}
          </span>
        </div>
      </div>
      <div className={prefixCls}>
        {expanded && (
          <div className={`${prefixCls}-content`}>
            {similarQuestions?.slice(0, 5).map((question, index) => {
              return (
                <div
                  className={`${prefixCls}-question`}
                  key={question.queryText}
                  onClick={() => {
                    onSelectQuestion(question);
                  }}
                >
                  {index + 1}. {question.queryText}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default SimilarQuestions;
