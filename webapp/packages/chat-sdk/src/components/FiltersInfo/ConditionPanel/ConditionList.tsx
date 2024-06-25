import { Flex } from 'antd';
import { forwardRef, memo, useImperativeHandle } from 'react';
import type { Condition } from '../types';
import ConditionItem from './ConditionItem';
import { useConditionStore } from './Context';

export interface IConditionListHandle {
  getConditions: () => Condition[];
}

const ConditionList = forwardRef<IConditionListHandle>((_, ref) => {
  const { list, remove } = useConditionStore();

  const onClickDelete = (index: number) => {
    remove(index);
  };

  useImperativeHandle(ref, () => ({
    getConditions: () => list,
  }));

  return (
    <Flex vertical gap={7}>
      {list.map((item, index) => (
        <ConditionItem key={item.uuid} index={index} data={item} onClickDelete={onClickDelete} />
      ))}
    </Flex>
  );
});

export default memo(ConditionList);
