import axios from '../service/axiosInstance';
import { isMobile } from '../utils/utils';
import { ShowCaseType } from './type';

const prefix = isMobile ? '/openapi' : '/api';

export function queryShowCase(agentId: number, current: number, pageSize: number) {
  return axios.post<ShowCaseType>(`${prefix}/chat/manage/queryShowCase?agentId=${agentId}`, {
    current,
    pageSize,
  });
}
