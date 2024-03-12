import { Tooltip, message } from 'antd';
import React, { useState, useEffect } from 'react';
import { indicatorStarState } from '../service';
import MStar from '@/components/MStar';

export type StarType = 'tag' | 'metric' | 'dimension';

type Props = {
  indicatorId: number;
  initState?: boolean;
  type?: StarType;
  onChange?: (state: boolean) => void;
};

const IndicatorStar: React.FC<Props> = ({ indicatorId, type = 'metric', initState = false }) => {
  const [star, setStar] = useState<boolean>(initState);
  useEffect(() => {
    setStar(initState);
  }, [initState]);

  const starStateChange = async (id: number, state: boolean) => {
    const { code, msg } = await indicatorStarState({ id, type, state });
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
            starStateChange(indicatorId, star);
          }}
        />
      </div>
    </Tooltip>
  );
};

export default IndicatorStar;
