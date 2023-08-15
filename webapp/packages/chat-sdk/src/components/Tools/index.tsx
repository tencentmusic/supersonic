import { isMobile } from '../../utils/utils';
import { DislikeOutlined, LikeOutlined } from '@ant-design/icons';
import { Button, Popover, message } from 'antd';
import { CLS_PREFIX } from '../../common/constants';
import { MsgDataType } from '../../common/type';
import RecommendOptions from '../RecommendOptions';
import { useState } from 'react';
import classNames from 'classnames';
import { updateQAFeedback } from '../../service';

type Props = {
  data: MsgDataType;
  scoreValue?: number;
  isLastMessage?: boolean;
  isMobileMode?: boolean;
  onSwitchEntity: (entityId: string) => void;
  onChangeChart: () => void;
};

const Tools: React.FC<Props> = ({
  data,
  scoreValue,
  isLastMessage,
  isMobileMode,
  onSwitchEntity,
  onChangeChart,
}) => {
  const [recommendOptionsOpen, setRecommendOptionsOpen] = useState(false);
  const { queryColumns, queryResults, queryId, chatContext, queryMode, entityInfo } = data || {};
  const [score, setScore] = useState(scoreValue || 0);

  const prefixCls = `${CLS_PREFIX}-tools`;

  const singleData = queryResults.length === 1;
  const isMetricCard =
    queryMode.includes('METRIC') &&
    (singleData || chatContext?.dateInfo?.startDate === chatContext?.dateInfo?.endDate);

  const noDashboard =
    (queryColumns?.length === 1 &&
      queryColumns[0].showType === 'CATEGORY' &&
      queryResults?.length === 1) ||
    (!queryMode.includes('METRIC') && !queryMode.includes('ENTITY')) ||
    isMetricCard;

  const changeChart = () => {
    onChangeChart();
  };

  const addToDashboard = () => {
    message.info('正在开发中，敬请期待');
  };

  const like = () => {
    setScore(5);
    updateQAFeedback(queryId, 5);
  };

  const dislike = () => {
    setScore(1);
    updateQAFeedback(queryId, 1);
  };

  const switchEntity = (option: string) => {
    setRecommendOptionsOpen(false);
    onSwitchEntity(option);
  };

  const likeClass = classNames(`${prefixCls}-like`, {
    [`${prefixCls}-feedback-active`]: score === 5,
  });
  const dislikeClass = classNames(`${prefixCls}-dislike`, {
    [`${prefixCls}-feedback-active`]: score === 1,
  });

  return (
    <div className={prefixCls}>
      {/* {isLastMessage && chatContext?.modelId && entityInfo?.entityId && (
        <Popover
          content={
            <RecommendOptions
              entityId={entityInfo.entityId}
              modelId={chatContext.modelId}
              modelName={chatContext.modelName}
              isMobileMode={isMobileMode}
              onSelect={switchEntity}
            />
          }
          placement={isMobileMode ? 'top' : 'right'}
          trigger="click"
          open={recommendOptionsOpen}
          onOpenChange={open => setRecommendOptionsOpen(open)}
        >
          <Button shape="round">切换其他匹配内容</Button>
        </Popover>
      )} */}
      {!isMobile && (
        <>
          {queryMode === 'METRIC_FILTER' && (
            <Button shape="round" onClick={changeChart}>
              切换图表
            </Button>
          )}
          {!noDashboard && (
            <Button shape="round" onClick={addToDashboard}>
              加入看板
            </Button>
          )}
          {isLastMessage && !isMetricCard && (
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
        </>
      )}
    </div>
  );
};

export default Tools;
