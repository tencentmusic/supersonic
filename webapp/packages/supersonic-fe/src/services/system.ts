import request from 'umi-request';

export function testLLMConn(data: any) {
  return request('/api/chat/agent/testLLMConn', {
    method: 'POST',
    data,
  });
}
