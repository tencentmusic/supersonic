import request from 'umi-request';

export function testLLMConn(data: any) {
  return request(`${process.env.CHAT_API_BASE_URL}model/testConnection`, {
    method: 'POST',
    data,
  });
}

export function getLlmModelTypeList(): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}model/getModelTypeList`, {
    method: 'GET',
  });
}

export function getLlmModelAppList(): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}model/getModelAppList`, {
    method: 'GET',
  });
}

export function getLlmList(): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}model/getModelList`, {
    method: 'GET',
  });
}

export function getLlmConfig(): Promise<any> {
  return request(`${process.env.CHAT_API_BASE_URL}model/getModelParameters`, {
    method: 'GET',
  });
}
