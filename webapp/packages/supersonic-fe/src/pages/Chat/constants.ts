export const THEME_COLOR_LIST = [
  '#3369FF',
  '#36D2B8',
  '#DB8D76',
  '#47B359',
  '#8545E6',
  '#E0B18B',
  '#7258F3',
  '#0095FF',
  '#52CC8F',
  '#6675FF',
  '#CC516E',
  '#5CA9E6',
];

export enum SemanticTypeEnum {
  MODEL = 'MODEL',
  DIMENSION = 'DIMENSION',
  METRIC = 'METRIC',
  VALUE = 'VALUE',
}

export const SEMANTIC_TYPE_MAP = {
  [SemanticTypeEnum.MODEL]: '主题域',
  [SemanticTypeEnum.DIMENSION]: '维度',
  [SemanticTypeEnum.METRIC]: '指标',
  [SemanticTypeEnum.VALUE]: '维度值',
};

export const AGENT_ICONS = [
  'icon-fukuanbaobiaochaxun',
  'icon-hangweifenxi1',
  'icon-xiaofeifenxi',
  'icon-renwuchaxun',
  'icon-baobiao',
  'icon-liushuichaxun',
  'icon-cangkuchaxun',
  'icon-xiaoshoushuju',
  'icon-tongji',
  'icon-shujutongji',
  'icon-mendiankanban',
];

export const CHAT_TITLE = '';

export const DEFAULT_CONVERSATION_NAME = '新问答对话';

export const PAGE_TITLE = '问答对话';

export const WEB_TITLE = '问答对话';

export const PLACE_HOLDER = '请输入您的问题';
