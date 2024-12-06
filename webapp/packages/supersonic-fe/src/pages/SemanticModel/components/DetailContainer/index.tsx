import React from 'react';
import styles from './style.less';

type Props = {
  siderNode: React.ReactNode;
  containerNode: React.ReactNode;
};

const DetailContainer: React.FC<Props> = ({ siderNode, containerNode }) => {
  return (
    <>
      <div className={styles.DetailWrapper}>
        <div className={styles.Detail}>
          <div className={styles.siderContainer}>{siderNode}</div>
          <div className={styles.tabContainer}>{containerNode}</div>
        </div>
      </div>
    </>
  );
};

export default DetailContainer;
