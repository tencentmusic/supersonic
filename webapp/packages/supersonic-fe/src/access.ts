import { ROUTE_AUTH_CODES } from '../config/routes';

export default function access(params) {
  if (!params) {
    return {};
  }
  const { authCodes } = params;
  return Object.keys(ROUTE_AUTH_CODES).reduce((result, key) => {
    const data = { ...result };
    const code = ROUTE_AUTH_CODES[key];
    const codes = authCodes || [];
    data[code] = codes.includes(code);
    return data;
  }, {});
}
