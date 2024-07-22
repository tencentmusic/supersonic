import { isMobile } from '../../utils/utils';
import { DislikeOutlined, LikeOutlined } from '@ant-design/icons';
import { CLS_PREFIX } from '../../common/constants';
import { useState } from 'react';
import classNames from 'classnames';
import { updateQAFeedback } from '../../service';
import { message } from 'antd';

type Props = {
  queryId: number;
  scoreValue?: number;
  isLastMessage?: boolean;
};

const Tools: React.FC<Props> = ({ queryId, scoreValue, isLastMessage }) => {
  const [score, setScore] = useState(scoreValue || 0);
  const [messageApi, contextHolder] = message.useMessage();

  const prefixCls = `${CLS_PREFIX}-tools`;

  const like = async () => {
    setScore(5);
    await updateQAFeedback(queryId, 5);
    // toast提示成功
    messageApi.success('点赞成功');
  };

  const dislike = async () => {
    setScore(1);
    await updateQAFeedback(queryId, 1);
    messageApi.success('点踩成功');
  };

  const likeClass = classNames(`${prefixCls}-like`, {
    [`${prefixCls}-feedback-active`]: score === 5,
  });
  const dislikeClass = classNames(`${prefixCls}-dislike`, {
    [`${prefixCls}-feedback-active`]: score === 1,
  });

  return (
    <>
      {contextHolder}
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
    </>
  );
};

export default Tools;
