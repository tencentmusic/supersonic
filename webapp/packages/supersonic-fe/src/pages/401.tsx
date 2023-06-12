import { Button, Result } from 'antd';
import React from 'react';
import { history } from 'umi';

const NoAuthPage: React.FC = () => (
  <Result
    status="403"
    title="当前页面无权限"
    subTitle={1 ? '请联系项目管理员 jerryjzhang 开通权限' : '请申请加入自己业务的项目'}
    extra={
      <Button type="primary" onClick={() => history.push('/homepage')}>
        回到首页
      </Button>
    }
  />
);

export default NoAuthPage;
