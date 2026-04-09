import { ROUTE_AUTH_CODES } from '../config/routes';

export default function access(params: any) {
  if (!params) {
    return {};
  }
  const { authCodes } = params;
  return Object.keys(ROUTE_AUTH_CODES).reduce((result, key) => {
    const data = { ...result };
    const code = ROUTE_AUTH_CODES[key as keyof typeof ROUTE_AUTH_CODES];
    const codes = authCodes || [];
    (data as Record<string, any>)[code] = codes.includes(code);
    return data;
  }, {});
}
