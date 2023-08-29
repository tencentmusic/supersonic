import { isMobile } from '../../utils/utils';
import { DislikeOutlined, LikeOutlined } from '@ant-design/icons';
import { CLS_PREFIX } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import { useState } from 'react';
import classNames from 'classnames';
import { updateQAFeedback } from '../../service';

type Props = {
  data: MsgDataType;
  scoreValue?: number;
  isLastMessage?: boolean;
};

const Tools: React.FC<Props> = ({ data, scoreValue, isLastMessage }) => {
  const { queryResults, queryId, chatContext, queryMode } = data || {};
  const [score, setScore] = useState(scoreValue || 0);

  const prefixCls = `${CLS_PREFIX}-tools`;

  const singleData = queryResults?.length === 1;
  const isMetricCard =
    queryMode.includes('METRIC') &&
    (singleData || chatContext?.dateInfo?.startDate === chatContext?.dateInfo?.endDate);

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
      {!isMobile && isLastMessage && (
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
