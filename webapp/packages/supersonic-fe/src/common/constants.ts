// 登陆 token key
export const AUTH_TOKEN_KEY = process.env.APP_TARGET === 'inner' ? 'TME_TOKEN' : 'SUPERSONIC_TOKEN';
// 记录上次访问页面
export const FROM_URL_KEY = 'FROM_URL';

export const BASE_TITLE = 'Supersonic';

export const PRIMARY_COLOR = '#f87653';
export const CHART_BLUE_COLOR = '#446dff';
export const CHAT_BLUE = '#1b4aef';
export const CHART_SECONDARY_COLOR = 'rgba(153, 153, 153, 0.3)';

export enum NumericUnit {
  None = '无',
  TenThousand = '万',
  EnTenThousand = 'w',
  OneHundredMillion = '亿',
  Thousand = 'k',
  Million = 'M',
  Giga = 'G',
}
