export enum SemanticTypeEnum {
  MODEL = 'MODEL',
  DIMENSION = 'DIMENSION',
  METRIC = 'METRIC',
  VALUE = 'VALUE',
  TERM = 'TERM',
}

export const SEMANTIC_TYPE_MAP = {
  [SemanticTypeEnum.MODEL]: '数据模型',
  [SemanticTypeEnum.DIMENSION]: '维度',
  [SemanticTypeEnum.METRIC]: '指标',
  [SemanticTypeEnum.VALUE]: '维度值',
  [SemanticTypeEnum.TERM]: '术语',
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

export const HOLDER_TAG = '@_supersonic_@';

export const DEFAULT_CONVERSATION_NAME = '新问答对话';

export const PLACE_HOLDER = '请输入您的问题，或输入“/”切换助理';

export const SIMPLE_PLACE_HOLDER = '请输入您的问题';
