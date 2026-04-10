export const FIXED_REPORT_STATUS_CONFIG: Record<string, { color: string; text: string }> = {
  AVAILABLE: { color: 'green', text: '可查看' },
  NO_RESULT: { color: 'default', text: '暂无结果' },
  EXPIRED: { color: 'orange', text: '结果过期' },
  RECENTLY_FAILED: { color: 'red', text: '最近失败' },
  NO_DELIVERY: { color: 'volcano', text: '未配置投递' },
  PARTIAL_CHANNEL_ERROR: { color: 'orange', text: '部分渠道异常' },
};

export function getFixedReportStatusInfo(status?: string) {
  if (!status) {
    return null;
  }
  return FIXED_REPORT_STATUS_CONFIG[status] || { color: 'default', text: status };
}
