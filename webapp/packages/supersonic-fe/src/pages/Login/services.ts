import request from '@/services/request';

export interface PostUserLoginRes {
  code: string; // 返回编码
  msg: string; // 返回消息
  data: string;
  traceId: string;
}

export interface PostUserRegesiterRes {
  code: string; // 返回编码
  msg: string; // 返回消息
  data: never;
  traceId: string;
}

export interface OAuthProvidersResponse {
  enabled: boolean;
  providers: string[];
  defaultProvider: string;
}

export interface OAuthTokenResponse {
  access_token?: string;
  token_type?: string;
  expires_in?: number;
  refresh_token?: string;
  session_id?: string;
  error?: string;
  message?: string;
}

export function userRegister(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/register`, {
    method: 'POST',
    data,
  });
}

export function postUserLogin(data: any): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/login`, {
    method: 'POST',
    data,
  });
}

/**
 * Get available OAuth providers.
 */
export function getOAuthProviders(): Promise<OAuthProvidersResponse> {
  return request(`${process.env.AUTH_API_BASE_URL}oauth/providers`, {
    method: 'GET',
  });
}

/**
 * Exchange OAuth code for tokens.
 * The exchange code is automatically sent via HTTP-only cookie.
 */
export function exchangeOAuthCode(): Promise<OAuthTokenResponse> {
  return request(`${process.env.AUTH_API_BASE_URL}oauth/token/exchange`, {
    method: 'POST',
    credentials: 'include', // Important: include cookies
  });
}

/**
 * Refresh access token using refresh token.
 */
export function refreshAccessToken(refreshToken: string): Promise<OAuthTokenResponse> {
  return request(`${process.env.AUTH_API_BASE_URL}token/refresh`, {
    method: 'POST',
    data: { refresh_token: refreshToken },
  });
}

/**
 * Revoke refresh token.
 */
export function revokeRefreshToken(refreshToken: string): Promise<{ success: boolean }> {
  return request(`${process.env.AUTH_API_BASE_URL}token/revoke`, {
    method: 'POST',
    data: { refresh_token: refreshToken },
  });
}

/**
 * Get OAuth authorization URL for a provider.
 */
export function getOAuthAuthorizeUrl(provider: string): string {
  return `${process.env.AUTH_API_BASE_URL}oauth/authorize/${provider}`;
}

/**
 * Get current user info including tenant ID.
 */
export function getCurrentUserInfo(): Promise<any> {
  return request(`${process.env.AUTH_API_BASE_URL}user/info`, {
    method: 'GET',
  });
}
