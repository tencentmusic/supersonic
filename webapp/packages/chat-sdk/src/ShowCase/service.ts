import axios from '../service/axiosInstance';
import { isMobile } from '../utils/utils';
import { ShowCaseType } from './type';

const prefix = isMobile ? '/openapi' : '/api';

export function queryShowCase(agentId: number) {
  return axios.post<ShowCaseType>(
    `${prefix}/chat/manage/queryShowCase?agentId=${agentId}`,
    { current: 1, pageSize: 10 }
  );
}
