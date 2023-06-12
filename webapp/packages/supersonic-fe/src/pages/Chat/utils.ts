import { ChatContextType } from 'supersonic-chat-sdk';
import moment from 'moment';

export function getConversationContext(chatContext: ChatContextType) {
  if (!chatContext) return '';
  const { domainName, metrics, dateInfo } = chatContext;
  // const dimensionStr =
  //   dimensions?.length > 0 ? dimensions.map((dimension) => dimension.name).join('、') : '';
  const timeStr =
    dateInfo?.text ||
    `近${moment(dateInfo?.endDate).diff(moment(dateInfo?.startDate), 'days') + 1}天`;

  return `${domainName}${
    metrics?.length > 0 ? `${timeStr}${metrics.map((metric) => metric.name).join('、')}` : ''
  }`;
}
