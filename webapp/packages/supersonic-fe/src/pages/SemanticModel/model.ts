import type { Reducer, Effect } from 'umi';
import { message } from 'antd';
import { ISemantic } from './data';
import { getDimensionList, queryMetric, excuteSql, getDatabaseList } from './service';

export type StateType = {
  current: number;
  pageSize: number;
  selectModelId: number;
  selectDomainId: number;
  selectDomainName: string;
  selectModelName: string;
  dimensionList: ISemantic.IDimensionList;
  metricList: ISemantic.IMetricList;
  searchParams: Record<string, any>;
  dataBaseResultColsMap: any;
  databaseConfigList: any[];
  domainData?: ISemantic.IDomainItem;
  modelData?: ISemantic.IDomainItem;
  domainList: ISemantic.IDomainItem[];
};

export type ModelType = {
  namespace: string;
  state: StateType;
  effects: {
    queryDimensionList: Effect;
    queryMetricList: Effect;
    queryDataBaseExcuteSql: Effect;
    queryDatabaseList: Effect;
  };
  reducers: {
    setSelectDomain: Reducer<StateType>;
    setSelectModel: Reducer<StateType>;
    setDomainList: Reducer<StateType>;
    setPagination: Reducer<StateType>;
    setDimensionList: Reducer<StateType>;
    setDataBaseScriptColumn: Reducer<StateType>;
    setDatabaseConfigList: Reducer<StateType>;
    setMetricList: Reducer<StateType>;
    reset: Reducer<StateType>;
  };
};

export const defaultState: StateType = {
  current: 1,
  pageSize: 20,
  selectDomainId: 0,
  selectModelId: 0,
  modelData: undefined,
  selectDomainName: '',
  selectModelName: '',
  searchParams: {},
  dimensionList: [],
  metricList: [],
  domainData: undefined,
  dataBaseResultColsMap: {},
  databaseConfigList: [],
  // dataBaseConfig: {},
  domainList: [],
};

const Model: ModelType = {
  namespace: 'domainManger',

  state: defaultState,
  effects: {
    *queryDimensionList({ payload }, { call, put }) {
      const { code, data, msg } = yield call(getDimensionList, payload);
      if (code === 200) {
        yield put({ type: 'setDimensionList', payload: { dimensionList: data.list } });
      } else {
        message.error(msg);
      }
    },
    *queryMetricList({ payload }, { call, put }) {
      const { code, data, msg } = yield call(queryMetric, payload);
      if (code === 200) {
        yield put({ type: 'setMetricList', payload: { metricList: data.list } });
      } else {
        message.error(msg);
      }
    },
    *queryDataBaseExcuteSql({ payload }, { call, put, select }) {
      const { tableName } = payload;
      if (!tableName) {
        return;
      }
      const isExists = yield select((state: any) => {
        return state.domainManger.dataBaseResultColsMap[tableName];
      });
      if (isExists) {
        return;
      }
      const { code, data, msg } = yield call(excuteSql, payload);
      if (code === 200) {
        const resultList = data.resultList.map((item, index) => {
          return {
            ...item,
            index,
          };
        });
        const scriptColumns = data.columns;
        yield put({
          type: 'setDataBaseScriptColumn',
          payload: { resultList, scriptColumns, tableName },
        });
      } else {
        message.error(msg);
      }
    },
    *queryDatabaseList({}, { call, put }) {
      const { code, data, msg } = yield call(getDatabaseList);
      if (code === 200) {
        yield put({
          type: 'setDatabaseConfigList',
          payload: { databaseConfigList: data },
        });
      } else {
        message.error(msg);
      }
    },
  },
  reducers: {
    setSelectDomain(state = defaultState, action) {
      return {
        ...state,
        selectDomainId: action.selectDomainId,
        selectDomainName: action.selectDomainName,
        domainData: action.domainData,
      };
    },
    setSelectModel(state = defaultState, action) {
      return {
        ...state,
        selectModelId: action.selectModelId,
        selectModelName: action.selectModelName,
        modelData: action.modelData,
      };
    },
    setDomainList(state = defaultState, action) {
      return {
        ...state,
        ...action.payload,
      };
    },
    setPagination(state = defaultState, action) {
      return {
        ...state,
        ...action.payload,
      };
    },
    setDimensionList(state = defaultState, action) {
      return {
        ...state,
        ...action.payload,
      };
    },
    setMetricList(state = defaultState, action) {
      return {
        ...state,
        ...action.payload,
      };
    },
    setDataBaseScriptColumn(state = defaultState, action) {
      return {
        ...state,
        dataBaseResultColsMap: {
          ...state.dataBaseResultColsMap,
          [action.payload.tableName]: { ...action.payload },
        },
      };
    },
    setDatabaseConfigList(state = defaultState, action) {
      return {
        ...state,
        ...action.payload,
      };
    },
    reset() {
      return defaultState;
    },
  },
};

export default Model;
