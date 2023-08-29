import { useState } from 'react';
import { ChatContextType } from '../../common/type';
import { Popover } from 'antd';
import { DownOutlined } from '@ant-design/icons';
import RecommendOptions from '../RecommendOptions';
import { PREFIX_CLS } from '../../common/constants';

type Props = {
  entityName: string;
  chatContext: ChatContextType;
  onSwitchEntity: (entityId: string) => void;
};

const SwicthEntity: React.FC<Props> = ({ entityName, chatContext, onSwitchEntity }) => {
  const [recommendOptionsOpen, setRecommendOptionsOpen] = useState(false);
  const { modelId, modelName, dimensionFilters } = chatContext || {};

  const prefixCls = `${PREFIX_CLS}-item`;

  const switchEntity = (option: string) => {
    setRecommendOptionsOpen(false);
    onSwitchEntity(option);
  };

  const entityId = dimensionFilters?.find(
    filter => filter?.bizName === 'zyqk_song_id' || filter?.bizName === 'singer_id'
  )?.value;

  return (
    <Popover
      content={
        <RecommendOptions
          entityId={entityId!}
          modelId={modelId}
          modelName={modelName}
          onSelect={switchEntity}
        />
      }
      placement="bottomLeft"
      trigger="click"
      open={recommendOptionsOpen}
      onOpenChange={open => setRecommendOptionsOpen(open)}
    >
      <div className={`${prefixCls}-tip-item-value ${prefixCls}-switch-entity`}>
        {entityName}
        <DownOutlined className={`${prefixCls}-down-icon`} />
      </div>
    </Popover>
  );
};

export default SwicthEntity;
