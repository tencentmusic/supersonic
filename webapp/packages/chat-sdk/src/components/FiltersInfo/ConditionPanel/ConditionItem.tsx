import {
  DeleteOutlined,
  FilterOutlined,
  InsertRowRightOutlined,
  SortAscendingOutlined,
} from '@ant-design/icons';
import { Button, Flex, Tooltip } from 'antd';
import type { Condition } from '../types';
import ConditionItemColumn from './ConditionItemColumn';
import ConditionItemFilter from './ConditionItemFilter';
import ConditionItemTopN from './ConditionItemTopN';
import { useConditionStore } from './Context';

type Props = {
  index: number;
  data: Condition;
  onClickDelete: (index: number) => void;
};

const IconMap = {
  filter: FilterOutlined,
  column: InsertRowRightOutlined,
  topN: SortAscendingOutlined,
};

const BaseItemMap = {
  filter: ConditionItemFilter,
  column: ConditionItemColumn,
  topN: ConditionItemTopN,
};

export default function ConditionItem({ index, data, onClickDelete }: Props) {
  const Icon = IconMap[data.type];
  const BaseItem = BaseItemMap[data.type];
  const { replace } = useConditionStore();

  const handleChange = (data: Condition) => {
    replace(index, data);
  };

  return (
    <Flex gap={'small'} justify="space-between" className="condition-item-wrap">
      <div className="condition-item-sort-number">{index + 1}</div>
      <div
        className="condition-item"
        style={{ background: '#f7f8fa', flex: 'auto', paddingLeft: '7px', paddingRight: '5px' }}
      >
        <Flex gap={5} align="center">
          <Icon />
          <BaseItem
            // @ts-ignore
            data={data}
            onChange={handleChange}
          />
        </Flex>
      </div>
      <div className="condition-item-remove">
        <Tooltip title="删除">
          <Button
            className="condition-item-remove-btn"
            type="link"
            size="small"
            icon={<DeleteOutlined />}
            onClick={() => onClickDelete(index)}
          />
        </Tooltip>
      </div>
    </Flex>
  );
}
