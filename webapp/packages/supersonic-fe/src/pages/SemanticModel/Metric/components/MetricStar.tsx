import { Tooltip, message } from 'antd';
import React, { useState } from 'react';
import { metricStarState } from '../../service';
import MStar from '@/components/MStar';

type Props = {
  metricId: number;
  initState?: boolean;
  onChange?: (state: boolean) => void;
};

const MetricStar: React.FC<Props> = ({ metricId, initState = false }) => {
  const [star, setStar] = useState<boolean>(initState);

  const starStateChange = async (id: number, state: boolean) => {
    const { code, msg } = await metricStarState({ id, state });
    if (code === 200) {
      setStar(state);
    } else {
      message.error(msg);
    }
  };

  return (
    <Tooltip title={`${star ? '取消' : '加入'}收藏`}>
      <div>
        <MStar
          star={star}
          onToggleCollect={(star: boolean) => {
            starStateChange(metricId, star);
          }}
        />
      </div>
    </Tooltip>
  );
};

export default MetricStar;
