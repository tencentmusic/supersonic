import { isProd } from '@/utils/utils';
import { MsgDataType } from 'supersonic-chat-sdk';
import classNames from 'classnames';
import { useEffect, useState, useCallback } from 'react';
import Message from '../Message';
import { updateMessageContainerScroll } from '@/utils/utils';
import styles from './style.less';
import LeftAvatar from '../CopilotAvatar';
import { DislikeOutlined, LikeOutlined } from '@ant-design/icons';
import { updateQAFeedback } from '../../service';

type Props = {
  id: string | number;
  followQuestions?: string[];
  data: MsgDataType;
  scoreValue?: number;
  msg: string;
  isHistory?: boolean;
  isLastMessage: boolean;
  isMobileMode?: boolean;
  onReportLoaded: (height: number) => void;
  onCheckMore: (data: MsgDataType) => void;
};

const DEFAULT_HEIGHT = 800;

const Plugin: React.FC<Props> = ({
  id,
  followQuestions,
  data,
  scoreValue,
  msg,
  isHistory,
  isLastMessage,
  isMobileMode,
  onReportLoaded,
  onCheckMore,
}) => {
  const {
    name,
    webPage: { url, params },
  } = data.response || {};

  const [pluginUrl, setPluginUrl] = useState('');
  const [height, setHeight] = useState(DEFAULT_HEIGHT);
  const [score, setScore] = useState(scoreValue || 0);

  const handleMessage = useCallback((event: MessageEvent) => {
    const messageData = event.data;
    const { type, payload } = messageData;
    if (type === 'changeMiniProgramContainerSize') {
      const { msgId, height } = payload;
      if (`${msgId}` === `${id}`) {
        setHeight(height);
        updateMessageContainerScroll();
      }
      return;
    }
    if (messageData === 'storyResize') {
      const ifr: any = document.getElementById(`reportIframe_${id}`);
      const iDoc = ifr.contentDocument || ifr.document || ifr.contentWindow;
      setTimeout(() => {
        setHeight(isProd() ? calcPageHeight(iDoc) : DEFAULT_HEIGHT);
      }, 200);
      return;
    }
  }, []);

  useEffect(() => {
    window.addEventListener('message', handleMessage);
    return () => {
      window.removeEventListener('message', handleMessage);
    };
  }, [handleMessage]);

  function calcPageHeight(doc: any) {
    const titleAreaEl = doc.getElementById('titleArea');
    const titleAreaHeight = Math.max(
      titleAreaEl?.clientHeight || 0,
      titleAreaEl?.scrollHeight || 0,
    );
    const dashboardGridEl = doc.getElementsByClassName('dashboardGrid')?.[0];
    const dashboardGridHeight = Math.max(
      dashboardGridEl?.clientHeight || 0,
      dashboardGridEl?.scrollHeight || 0,
    );
    return Math.max(titleAreaHeight + dashboardGridHeight + 10, DEFAULT_HEIGHT);
  }

  const initData = () => {
    const heightValue =
      params?.find((option: any) => option.paramType === 'FORWARD' && option.key === 'height')
        ?.value || DEFAULT_HEIGHT;
    setHeight(heightValue);
    let urlValue = url;
    const valueParams = (params || [])
      .filter((option: any) => option.paramType !== 'FORWARD')
      .reduce((result: any, item: any) => {
        result[item.key] = item.value;
        return result;
      }, {});
    if (urlValue.includes('?type=dashboard') || urlValue.includes('?type=widget')) {
      const filterData = encodeURIComponent(
        JSON.stringify(
          urlValue.includes('dashboard')
            ? {
                global: valueParams,
              }
            : {
                local: valueParams,
              },
        ),
      );
      urlValue = urlValue.replace(
        '?',
        `?miniProgram=true&reportName=${name}&filterData=${filterData}&`,
      );
      urlValue =
        !isProd() && !urlValue.includes('http') ? `http://s2.tmeoa.com${urlValue}` : urlValue;
    } else {
      const params = Object.keys(valueParams || {}).map((key) => `${key}=${valueParams[key]}`);
      if (params.length > 0) {
        if (url.includes('?')) {
          urlValue = urlValue.replace('?', `?${params.join('&')}&`);
        } else {
          urlValue = `${urlValue}?${params.join('&')}`;
        }
      }
    }
    onReportLoaded(heightValue + 190);
    setPluginUrl(urlValue);
  };

  useEffect(() => {
    initData();
  }, []);

  const reportClass = classNames(styles.report, {
    [styles.mobileMode]: isMobileMode,
  });

  const like = () => {
    setScore(5);
    updateQAFeedback(data.queryId, 5);
  };

  const dislike = () => {
    setScore(1);
    updateQAFeedback(data.queryId, 1);
  };

  const likeClass = classNames(styles.like, {
    [styles.feedbackActive]: score === 5,
  });

  const dislikeClass = classNames(styles.dislike, {
    [styles.feedbackActive]: score === 1,
  });

  return (
    <div className={reportClass}>
      <LeftAvatar />
      <div className={styles.msgContent}>
        <Message position="left" width="100%" height={height} bubbleClassName={styles.reportBubble}>
          <iframe
            id={`reportIframe_${id}`}
            src={pluginUrl}
            className={styles.reportContent}
            style={{ height }}
            allowFullScreen
          />
        </Message>
        {isLastMessage && (
          <div className={styles.tools}>
            <div className={styles.feedback}>
              <div>这个回答正确吗？</div>
              <LikeOutlined className={likeClass} onClick={like} />
              <DislikeOutlined className={dislikeClass} onClick={dislike} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Plugin;
