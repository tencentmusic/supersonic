import { useDynamicList } from 'ahooks';
import { createContext, useContextSelector } from 'use-context-selector';
import type { Condition } from '../types';
import {
  createColumnConditionData,
  createFilterConditionData,
  createTopNConditionData,
} from '../utils';

type ConditionContextType = {
  list: Condition[];
  addCondition: () => void;
  addColumn: () => void;
  addTopN: () => void;
  clearAll: () => void;
  replace: (index: number, condition: Condition) => void;
  remove: (index: number) => void;
};

const ConditionContext = createContext<ConditionContextType>({} as ConditionContextType);

function createInitialConditionData() {
  return [createFilterConditionData()] as Condition[];
}

export function useConditionStore() {
  const context = useContextSelector(ConditionContext, context => context);
  return context;
}

export function ConditionContextProvider({ children }: { children: React.ReactNode }) {
  const { list, push, resetList, replace, remove } = useDynamicList(createInitialConditionData());

  const addCondition = () => {
    push(createFilterConditionData());
  };

  const addColumn = () => {
    push(createColumnConditionData());
  };

  const addTopN = () => {
    push(createTopNConditionData());
  };

  const clearAll = () => {
    // 清空所有field、operator、value
    const newList = [] as Condition[];
    list.forEach(item => {
      if (item.type === 'filter') {
        newList.push(createFilterConditionData());
      } else if (item.type === 'column') {
        newList.push(createColumnConditionData());
      } else if (item.type === 'topN') {
        newList.push(createTopNConditionData());
      }
    });

    resetList(newList);
  };

  return (
    <ConditionContext.Provider
      value={{ addCondition, addColumn, addTopN, clearAll, replace, remove, list }}
    >
      {children}
    </ConditionContext.Provider>
  );
}
