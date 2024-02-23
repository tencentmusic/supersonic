import { useCallback, useEffect, useState } from 'react';
import { MsgDataType } from '../../../common/type';
import { getToken, isProd } from '../../../utils/utils';
import { webPageHost } from '../../../common/env';

type Props = {
  id: string | number;
  data: MsgDataType;
};

const DEFAULT_HEIGHT = 800;

const WebPage: React.FC<Props> = ({ id, data }) => {
  const [pluginUrl, setPluginUrl] = useState('');
  const [height, setHeight] = useState(DEFAULT_HEIGHT);

  const {
    name,
    webPage: { url, params },
  } = data.response || {};

  const handleMessage = useCallback((event: MessageEvent) => {
    const messageData = event.data;
    const { type, payload } = messageData;
    if (type === 'changeMiniProgramContainerSize') {
      const { msgId, height } = payload;
      if (`${msgId}` === `${id}`) {
        setHeight(height);
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
      titleAreaEl?.scrollHeight || 0
    );
    const dashboardGridEl = doc.getElementsByClassName('dashboardGrid')?.[0];
    const dashboardGridHeight = Math.max(
      dashboardGridEl?.clientHeight || 0,
      dashboardGridEl?.scrollHeight || 0
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
              }
        )
      );
      urlValue = urlValue.replace(
        '?',
        `?token=${getToken()}&miniProgram=true&reportName=${name}&filterData=${filterData}&`
      );
      urlValue = `${webPageHost}${urlValue}`;
    } else {
      const params = Object.keys(valueParams || {}).map(key => `${key}=${valueParams[key]}`);
      if (params.length > 0) {
        if (url.includes('?')) {
          urlValue = urlValue.replace('?', `?${params.join('&')}&`);
        } else {
          urlValue = `${urlValue}?${params.join('&')}`;
        }
      }
    }
    setPluginUrl(urlValue);
  };

  useEffect(() => {
    initData();
  }, []);

  return (
    <iframe
      id={`reportIframe_${id}`}
      name={`reportIframe_${id}`}
      src={pluginUrl}
      style={{ width: '100%', height, border: 'none' }}
      title="reportIframe"
      allowFullScreen
    />
  );
};

export default WebPage;
