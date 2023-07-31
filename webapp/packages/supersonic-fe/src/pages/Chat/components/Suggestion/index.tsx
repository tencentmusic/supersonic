import { updateMessageContainerScroll } from '@/utils/utils';
import { useEffect, useState } from 'react';
import { querySuggestion } from '../../service';
import { SuggestionType } from '../../type';
import Message from '../Message';
import styles from './style.less';

type Props = {
  domainId: number;
  onSelectSuggestion: (value: string) => void;
};

const Suggestion: React.FC<Props> = ({ domainId, onSelectSuggestion }) => {
  const [data, setData] = useState<SuggestionType>({ dimensions: [], metrics: [] });
  const { metrics } = data;

  const initData = async () => {
    const res = await querySuggestion(domainId);
    setData({
      dimensions: res.data.dimensions.slice(0, 5),
      metrics: res.data.metrics.slice(0, 5),
    });
    updateMessageContainerScroll();
  };

  useEffect(() => {
    initData();
  }, []);

  return (
    <div className={styles.suggestion}>
      <Message position="left" bubbleClassName={styles.suggestionMsg}>
        <div className={styles.row}>
          <div className={styles.rowTitle}>您可能还想问以下指标：</div>
          <div className={styles.rowContent}>
            {metrics.map((metric, index) => {
              return (
                <>
                  <span
                    className={styles.contentItemName}
                    onClick={() => {
                      onSelectSuggestion(metric.name);
                    }}
                  >
                    {metric.name}
                  </span>
                  {index !== metrics.length - 1 && <span>、</span>}
                </>
              );
            })}
          </div>
        </div>
      </Message>
    </div>
  );
};

export default Suggestion;
