import { HistoryMsgItemType } from "../common/type";

export type ShowCaseType = {
  showCaseMap: Record<number, HistoryMsgItemType[]>,
  current: number,
  pageSize: number,
}