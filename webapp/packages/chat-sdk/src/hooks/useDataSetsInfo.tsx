import { createContext, useContextSelector } from 'use-context-selector';
import { useDataSetsFetch } from './useDataSetsFetch';

const DataSetsInfoContext = createContext<
  Map<
    number,
    {
      id: number;
      name: string;
      description: string | null;
      dimensions: any[];
      metrics: any[];
    }
  >
>(new Map());

export function useDataSetsInfo() {
  return useContextSelector(DataSetsInfoContext, context => context);
}

export function DataSetsInfoProvider({ children, ids }) {
  const { dataSets } = useDataSetsFetch(ids);
  return (
    <DataSetsInfoContext.Provider value={dataSets}>
      {dataSets.size > 0 ? children : null}
    </DataSetsInfoContext.Provider>
  );
}
