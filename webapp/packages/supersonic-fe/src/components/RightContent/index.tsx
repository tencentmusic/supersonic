import { QuestionCircleOutlined } from '@ant-design/icons';
import { Button, Dropdown, MenuProps, Space } from 'antd';
import React from 'react';

import Avatar from './AvatarDropdown';
import styles from './index.less';

export type SiderTheme = 'light' | 'dark';

const GlobalHeaderRight: React.FC = () => {
  function handleLogin() {}

  const items: MenuProps['items'] = [
    {
      key: '1',
      label: (
        <a
          target="_blank"
          rel="noopener noreferrer"
          href="https://jdy.cvte.com/f/667e2879cb6b171c6bd3f223"
        >
          建议与反馈
        </a>
      ),
    },
    {
      key: '2',
      label: (
        <a target="_blank" rel="noopener noreferrer" href="https://cvte.kdocs.cn/l/ctXXFLzY83ak">
          帮助文档
        </a>
      ),
    },
  ];

  return (
    <Space className={styles.right}>
      <Dropdown menu={{ items }}>
        <Button type="text">
          <QuestionCircleOutlined />
        </Button>
      </Dropdown>
      <Avatar onClickLogin={handleLogin} />
    </Space>
  );
};
export default GlobalHeaderRight;
