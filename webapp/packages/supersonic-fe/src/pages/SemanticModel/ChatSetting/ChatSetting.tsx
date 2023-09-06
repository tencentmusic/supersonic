import React from 'react';
import { connect, Helmet } from 'umi';
import type { StateType } from '../model';
import OverviewContainer from '../OverviewContainer';
import type { Dispatch } from 'umi';

type Props = {
  domainManger: StateType;
  dispatch: Dispatch;
};
const ChatSetting: React.FC<Props> = () => {
  return (
    <>
      <Helmet title={'语义模型-超音数'} />
      <OverviewContainer mode={'chatSetting'} />
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(ChatSetting);
