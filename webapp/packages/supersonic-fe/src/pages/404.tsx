import { Button, Result } from 'antd';
import React from 'react';
import { history } from '@umijs/max';

const NoFoundPage: React.FC = () => (
  <Result
    status="404"
    title="404"
    subTitle="Sorry, the page you visited does not exist."
    extra={
      <Button type="primary" onClick={() => history.push('/homepage')}>
        Back Home
      </Button>
    }
  />
);

export default NoFoundPage;
