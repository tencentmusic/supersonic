import { Button, Result, Spin } from 'antd';
import { history } from '@umijs/max';
import { createContext, useContextSelector } from 'use-context-selector';
import { useDataSetsFetch } from './useDataSetsFetch';
import { LoadingOutlined } from '@ant-design/icons';

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

function Container({ children }) {
  return (
    <div
      style={{ display: 'flex', height: '100%', justifyContent: 'center', alignItems: 'center' }}
    >
      {children}
    </div>
  );
}

export function DataSetsInfoProvider({ children, ids }) {
  const { dataSets, loading } = useDataSetsFetch(ids);
  if (loading)
    return (
      <Container>
        <Spin indicator={<LoadingOutlined style={{ fontSize: 48 }} spin />} />
      </Container>
    );

  return (
    <DataSetsInfoContext.Provider value={dataSets}>
      {dataSets.size > 0 ? (
        children
      ) : (
        <Container>
          <Result
            status="warning"
            title="当前agent没有数据集，请先关联数据集"
            extra={
              <Button type="primary" key="console" onClick={() => history.push('/agent')}>
                去关联
              </Button>
            }
          />
        </Container>
      )}
    </DataSetsInfoContext.Provider>
  );
}
