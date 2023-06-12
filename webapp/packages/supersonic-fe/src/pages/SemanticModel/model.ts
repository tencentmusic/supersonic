import type { Reducer, Effect } from 'umi';
import { message } from 'antd';
import { getDimensionList, queryMetric } from './service';

export type StateType = {
  current: number;
  pageSize: number;
  selectDomainId: any;
  selectDomainName: string;
  dimensionList: any[];
  metricList: any[];
  searchParams: Record<string, any>;
  domainData: any;
};

export type ModelType = {
  namespace: string;
  state: StateType;
  effects: {
    queryDimensionList: Effect;
    queryMetricList: Effect;
  };
  reducers: {
    setSelectDomain: Reducer<StateType>;
    setPagination: Reducer<StateType>;
    setDimensionList: Reducer<StateType>;
    setMetricList: Reducer<StateType>;
    reset: Reducer<StateType>;
  };
};

export const defaultState: StateType = {
  current: 1,
  pageSize: 20,
  selectDomainId: undefined,
  selectDomainName: '',
  searchParams: {},
  dimensionList: [],
  metricList: [],
  domainData: {},
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
    reset() {
      return defaultState;
    },
  },
};

export default Model;
