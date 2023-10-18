import { isMobile } from '../../utils/utils';
import { DislikeOutlined, LikeOutlined } from '@ant-design/icons';
import { CLS_PREFIX } from '../../common/constants';
import { useState } from 'react';
import classNames from 'classnames';
import { updateQAFeedback } from '../../service';

type Props = {
  queryId: number;
  scoreValue?: number;
  isLastMessage?: boolean;
};

const Tools: React.FC<Props> = ({ queryId, scoreValue, isLastMessage }) => {
  const [score, setScore] = useState(scoreValue || 0);

  const prefixCls = `${CLS_PREFIX}-tools`;

  const like = () => {
    setScore(5);
    updateQAFeedback(queryId, 5);
  };

  const dislike = () => {
    setScore(1);
    updateQAFeedback(queryId, 1);
  };

  const likeClass = classNames(`${prefixCls}-like`, {
    [`${prefixCls}-feedback-active`]: score === 5,
  });
  const dislikeClass = classNames(`${prefixCls}-dislike`, {
    [`${prefixCls}-feedback-active`]: score === 1,
  });

  return (
    <div className={prefixCls}>
      {!isMobile && (
        <div className={`${prefixCls}-feedback`}>
          <div>这个回答正确吗？</div>
          <LikeOutlined className={likeClass} onClick={like} />
          <DislikeOutlined
            className={dislikeClass}
            onClick={e => {
              e.stopPropagation();
              dislike();
            }}
          />
        </div>
      )}
    </div>
  );
};

export default Tools;
