import request from 'umi-request';

type ExcuteSqlParams = {
  sql: string;
  domainId: number;
};

// 执行脚本
export async function excuteSql(params: ExcuteSqlParams) {
  const data = { ...params };
  return request.post(`${process.env.API_BASE_URL}database/executeSql`, { data });
}
