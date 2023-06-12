import { isMobile } from '../../utils/utils';
import { DislikeOutlined, LikeOutlined } from '@ant-design/icons';
import { Button, message } from 'antd';
import { CLS_PREFIX } from '../../common/constants';

type Props = {
  isLastMessage?: boolean;
};

const Tools: React.FC<Props> = ({ isLastMessage }) => {
  const prefixCls = `${CLS_PREFIX}-tools`;

  const changeChart = () => {
    message.info('正在开发中，敬请期待');
  };

  const addToDashboard = () => {
    message.info('正在开发中，敬请期待');
  };

  const lockDomain = () => {
    message.info('正在开发中，敬请期待');
  };

  const like = () => {
    message.info('正在开发中，敬请期待');
  };

  const dislike = () => {
    message.info('正在开发中，敬请期待');
  };

  const lockDomainSection = isLastMessage && (
    <Button shape="round" onClick={lockDomain}>
      锁定主题域
    </Button>
  );

  const feedbackSection = isLastMessage && (
    <div className={`${prefixCls}-feedback`}>
      <div>这个回答正确吗？</div>
      <LikeOutlined className={`${prefixCls}-like`} onClick={like} />
      <DislikeOutlined className={`${prefixCls}-dislike`} onClick={dislike} />
    </div>
  );

  if (isMobile) {
    return (
      <div className={`${prefixCls}-mobile-tools`}>
        {isLastMessage && <div className={`${prefixCls}-tools`}>{lockDomainSection}</div>}
        {feedbackSection}
      </div>
    );
  }

  return (
    <div className={prefixCls}>
      <Button shape="round" onClick={changeChart}>
        切换图表
      </Button>
      <Button shape="round" onClick={addToDashboard}>
        加入看板
      </Button>
      {lockDomainSection}
      {feedbackSection}
    </div>
  );
};

export default Tools;
