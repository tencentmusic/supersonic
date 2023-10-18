import { HistoryMsgItemType } from "../common/type";

export type ShowCaseMapType = Record<number, HistoryMsgItemType[]>;

export type ShowCaseType = {
  showCaseMap: ShowCaseMapType,
  current: number,
  pageSize: number,
}