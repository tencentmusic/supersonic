import request from 'umi-request';

export function testLLMConn(data: any) {
  return request(`${process.env.CHAT_API_BASE_URL}model/testConnection`, {
    method: 'POST',
    data,
  });
}
