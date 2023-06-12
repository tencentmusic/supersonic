import { getFormattedValueData } from '@/utils/utils';
import moment from 'moment';
import React from 'react';
import styles from './style.less';
import type { EntityInfoType, MsgDataType } from 'supersonic-chat-sdk';

type Props = {
  currentEntity: MsgDataType;
};

const Introduction: React.FC<Props> = ({ currentEntity }) => {
  const { entityInfo } = currentEntity;
  const { dimensions, metrics } = entityInfo || ({} as EntityInfoType);

  return (
    <div className={styles.introduction}>
      {dimensions
        ?.filter((dimension) => !dimension.bizName.includes('photo'))
        .map((dimension) => {
          return (
            <div className={styles.field} key={dimension.name}>
              <span className={styles.fieldName}>{dimension.name}：</span>
              <span className={styles.fieldValue}>
                {dimension.bizName.includes('publish_time')
                  ? moment(dimension.value).format('YYYY-MM-DD')
                  : dimension.value}
              </span>
            </div>
          );
        })}
      {metrics?.map((metric) => (
        <div className={styles.field} key={metric.name}>
          <span className={styles.fieldName}>{metric.name}：</span>
          <span className={styles.fieldValue}>{getFormattedValueData(metric.value)}</span>
        </div>
      ))}
    </div>
  );
};

export default Introduction;
