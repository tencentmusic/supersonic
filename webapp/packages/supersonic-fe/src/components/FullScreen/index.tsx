import type { ReactNode, FC } from 'react';
import { useEffect } from 'react';
import { useImperativeHandle, useState } from 'react';
import { FullscreenExitOutlined } from '@ant-design/icons';
import styles from './index.less';

export interface IProps {
  children: ReactNode;
  maxRef?: any;
  top?: string;
  isFullScreen: boolean;
  triggerBackToNormal: () => void;
}

const FullScreen: FC<IProps> = ({
  children,
  maxRef,
  top = '50px',
  isFullScreen,
  triggerBackToNormal,
}) => {
  const [wrapCls, setWrapCls] = useState(styles.normalState);
  const changeToMax = () => {
    setWrapCls(styles.maxState);
  };

  const changeToNormal = () => {
    setWrapCls(styles.normalState);
  };

  const handleBackToNormal = () => {
    if (typeof triggerBackToNormal === 'function') {
      triggerBackToNormal();
    }
  };

  useEffect(() => {
    if (isFullScreen) {
      changeToMax();
    } else {
      changeToNormal();
    }
  }, [isFullScreen]);

  useImperativeHandle(maxRef, () => ({
    changeToMax,
    changeToNormal,
  }));

  return (
    <div className={wrapCls} style={wrapCls === styles.maxState ? { paddingTop: top } : {}}>
      <div
        className={styles.innerWrap}
        style={wrapCls === styles.maxState ? { top } : { height: '100%' }}
      >
        <div className={styles.backNormal}>
          <FullscreenExitOutlined
            className={styles.fullscreenExitIcon}
            title="退出全屏"
            onClick={handleBackToNormal}
          />
        </div>
        {children}
      </div>
    </div>
  );
};

export default FullScreen;
