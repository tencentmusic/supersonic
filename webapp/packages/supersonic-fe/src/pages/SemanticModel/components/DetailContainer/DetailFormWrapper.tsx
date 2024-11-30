import { Button } from 'antd';
import React, { useState, useEffect } from 'react';
import { MenuItem } from './type';
import styles from './style.less';

type Props = {
  detailData?: any;
  currentMenu: MenuItem;
  onSave?: (data?: any) => void;
} & { children: React.ReactNode };

const DetailFormWrapper: React.FC<Props> = ({ children, currentMenu, onSave }) => {
  const [settingKey, setSettingKey] = useState<string>(currentMenu?.key);

  useEffect(() => {
    if (currentMenu) {
      setSettingKey(currentMenu.key);
    }
  }, [currentMenu]);

  return (
    <div className={styles.infoCard}>
      <div className={styles.infoCardTitle}>
        <span style={{ flex: 'auto' }}>{currentMenu?.text}</span>

        <span style={{ flex: 'none' }}>
          <Button
            type="primary"
            onClick={() => {
              onSave?.();
            }}
          >
            保 存
          </Button>
          {/* <Button
          size="middle"
          type="link"
          key="backListBtn"
          onClick={() => {
            history.back();
          }}
        >
          <Space>
            <ArrowLeftOutlined />
            返回列表页
          </Space>
        </Button> */}
        </span>
      </div>
      <div className={styles.infoCardContainer}>{children}</div>
    </div>
  );
};

export default DetailFormWrapper;
