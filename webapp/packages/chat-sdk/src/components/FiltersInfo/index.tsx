import { useBoolean, useDynamicList } from 'ahooks';
import { createContext } from 'use-context-selector';
import { PREFIX_CLS } from '../../common/constants';
import Explain from './Explain';
import FilterConditions from './FilterConditions';
import { IPill } from './types';
import { translate2ExplainText } from './utils';
import {
  createRef,
  forwardRef,
  useCallback,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
} from 'react';

export const prefixCls = `${PREFIX_CLS}-filters-info-wrap`;

export const mockData: IPill[] = [
  {
    id: '1',
    type: 'text-filter',
    field: 'A',
    fieldId: 1,
    fieldName: 'A',
    operator: 'eq',
    value: '1',
  },
  {
    id: '2',
    type: 'text-filter',
    field: 'B',
    fieldId: 2,
    fieldName: 'B',
    operator: 'eq',
    value: '2',
  },
  {
    id: '5',
    type: 'group',
    fields: [
      {
        field: 'A',
        fieldName: 'A',
      },
      {
        field: 'B',
        fieldName: 'B',
      },
    ],
  },
  {
    id: '6',
    type: 'aggregation',
    fields: [
      {
        field: 'C',
        fieldName: 'C',
        operator: 'sum',
      },
      {
        field: 'D',
        fieldName: 'D',
        operator: 'sum',
      },
    ],
  },
  {
    id: '3',
    type: 'number-filter',
    field: 'C',
    fieldName: 'C',
    operator: 'gt',
    value: 3,
  },
  {
    id: '4',
    type: 'number-filter',
    field: 'D',
    fieldName: 'D',
    operator: 'lt',
    value: 4,
  },
  {
    id: '7',
    type: 'top-n',
    value: 10,
  },
];

type FilterInfosContextType = {
  pillData: IPill[];
  agentId: number | null;
  datasetId: number | null;
  existItemSelected: boolean;
  removePillItem: (index: number) => void;
  updatePillItem: (index: number, item: IPill) => void;
  movePillItem: (oldIndex: number, newIndex: number) => void;
  insertPillItem: (index: number, item: IPill) => void;
  pushPillItem: (item: IPill) => void;
  getPillKey: (index: number) => number;
  getPillIndex: (key: number) => number;
  getPill(index: number): IPill;
  resetList: (list: IPill[]) => void;
};

export const FilterInfosContext = createContext<FilterInfosContextType>({
  agentId: null,
  datasetId: null,
  pillData: [],
  existItemSelected: false,
  removePillItem: () => {},
  updatePillItem: () => {},
  movePillItem: () => {},
  insertPillItem: () => {},
  pushPillItem: () => {},
  getPillKey: () => 0,
  getPillIndex: () => 0,
  getPill: () => ({ id: '' } as IPill),
  resetList: () => {},
});

export interface IPillEditHandleRef {
  resetData: (agentId: number, datasetId: number, list: IPill[]) => void;
}

type Props = {
  onConfirm: (data: {
    pillData: IPill[];
    datasetId: number;
    dimensions: any[];
    metrics: any[];
  }) => void;
};

const Index = forwardRef<IPillEditHandleRef, Props>(({ onConfirm }: Props, ref) => {
  const [editing, { setTrue: startEditing, setFalse: finishEditing }] = useBoolean(false);

  const [agentId, setAgentId] = useState<number | null>(null);

  const [datasetId, setDatasetId] = useState<number | null>(null);

  const {
    list: pillData,
    remove: removePillItem,
    replace: updatePillItem,
    move: movePillItem,
    push: pushPillItem,
    insert: insertPillItem,
    getKey: getPillKey,
    getIndex: getPillIndex,
    resetList,
  } = useDynamicList<IPill>([]);

  const existItemSelected = useMemo(() => pillData.some(item => item.selected), [pillData]);

  const mode = 'detail';

  const text = translate2ExplainText(mode, pillData);

  const getPill = useCallback((index: number) => pillData[index], [pillData]);

  useImperativeHandle(ref, () => ({
    resetData: (id, _datasetId, data) => {
      resetList(data);
      setAgentId(id);
      setDatasetId(_datasetId);
    },
  }));

  return (
    <div className={prefixCls}>
      <FilterInfosContext.Provider
        value={{
          agentId,
          datasetId,
          pillData,
          existItemSelected,
          removePillItem,
          updatePillItem,
          movePillItem,
          pushPillItem,
          insertPillItem,
          getPillKey,
          getPillIndex,
          getPill,
          resetList,
        }}
      >
        {editing ? (
          <FilterConditions mode={mode} onEditFinished={finishEditing} onEditConfirm={onConfirm} />
        ) : (
          <Explain text={text} onClickEdit={startEditing} />
        )}
      </FilterInfosContext.Provider>
    </div>
  );
});

export default Index;
