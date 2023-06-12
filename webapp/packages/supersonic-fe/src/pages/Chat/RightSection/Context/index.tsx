import moment from 'moment';
import styles from './style.less';
import type { ChatContextType } from 'supersonic-chat-sdk';

type Props = {
  chatContext: ChatContextType;
};

const Context: React.FC<Props> = ({ chatContext }) => {
  const { domainName, metrics, dateInfo, filters } = chatContext;

  return (
    <div className={styles.context}>
      <div className={styles.title}>相关信息</div>
      <div className={styles.content}>
        <div className={styles.field}>
          <span className={styles.fieldName}>主题域：</span>
          <span className={styles.fieldValue}>{domainName}</span>
        </div>
        {dateInfo && (
          <div className={styles.field}>
            <span className={styles.fieldName}>时间范围：</span>
            <span className={styles.fieldValue}>
              {dateInfo.text ||
                `近${moment(dateInfo.endDate).diff(moment(dateInfo.startDate), 'days') + 1}天`}
            </span>
          </div>
        )}
        {metrics && metrics.length > 0 && (
          <div className={styles.field}>
            <span className={styles.fieldName}>指标：</span>
            <span className={styles.fieldValue}>
              {metrics.map((metric) => metric.name).join('、')}
            </span>
          </div>
        )}
        {filters && filters.length > 0 && (
          <div className={styles.filterSection}>
            <div className={styles.fieldName}>筛选条件：</div>
            <div className={styles.filterValues}>
              {filters.map((filter) => {
                return (
                  <div className={styles.filterItem} key={filter.name}>
                    {filter.name}：{filter.value}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Context;
