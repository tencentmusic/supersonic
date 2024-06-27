import { useBoolean, useDynamicList } from 'ahooks';
import { createContext } from 'use-context-selector';
import { PREFIX_CLS } from '../../common/constants';
import Explain from './Explain';
import FilterConditions from './FilterConditions';
import { IPill } from './types';
import { getPillsByParseInfo, translate2ExplainText } from './utils';
import { useCallback, useEffect, useMemo } from 'react';
import { ChatContextType } from '../../common/type';
import { useDatasetInfo } from './hooks/useDatasetInfo';

export const prefixCls = `${PREFIX_CLS}-filters-info-wrap`;

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
  resetData: (agentId: number, chatContent: ChatContextType) => void;
}

type Props = {
  agentId: number;
  chatContext: ChatContextType;
  onConfirm: (data: {
    pillData: IPill[];
    datasetId: number;
    dimensions: any[];
    metrics: any[];
  }) => void;
};

const Index = ({ onConfirm, agentId, chatContext }: Props) => {
  const [editing, { setTrue: startEditing, setFalse: finishEditing }] = useBoolean(false);

  const datasetId = chatContext.dataSet.id;

  const { getTypeByBizName } = useDatasetInfo(datasetId);

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

  useEffect(() => {
    resetList(getPillsByParseInfo(chatContext, getTypeByBizName));
  }, [chatContext]);

  return (
    <div className={prefixCls}>
      <FilterInfosContext.Provider
        value={{
          agentId,
          datasetId: chatContext.dataSet.id,
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
        {datasetId !== null &&
          datasetId !== undefined &&
          (editing ? (
            <FilterConditions
              mode={mode}
              onEditFinished={finishEditing}
              onEditConfirm={onConfirm}
            />
          ) : (
            <Explain text={text} onClickEdit={startEditing} />
          ))}
      </FilterInfosContext.Provider>
    </div>
  );
};

export default Index;
