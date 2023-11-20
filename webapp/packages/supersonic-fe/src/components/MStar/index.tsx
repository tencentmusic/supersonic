import React, { useState, useEffect } from 'react';
import { StarOutlined, StarFilled } from '@ant-design/icons';
import styles from './style.less';

type Props = {
  star?: boolean;
  onToggleCollect: (star: boolean) => void;
};

const MStar: React.FC<Props> = ({ star = false, onToggleCollect }) => {
  const [starState, setStarState] = useState(star);
  useEffect(() => {
    setStarState(star);
  }, [star]);

  return (
    <div
      className={`${styles.collectDashboard} ${starState === true ? 'dashboardCollected' : ''}`}
      onClick={(event) => {
        event.stopPropagation();
        setStarState(!starState);
        onToggleCollect(!starState);
      }}
    >
      {starState === false ? <StarOutlined /> : <StarFilled style={{ color: '#eac54f' }} />}
    </div>
  );
};

export default MStar;
