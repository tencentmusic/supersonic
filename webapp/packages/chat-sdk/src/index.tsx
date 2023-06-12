import './styles/index.less';

// import React from 'react';
// import ReactDOM from 'react-dom/client';
// import Chat from './demo/Chat';

// const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);

// root.render(
//   <React.StrictMode>
//     <Chat />
//   </React.StrictMode>
// );

export { default as ChatMsg } from './components/ChatMsg';

export { default as ChatItem } from './components/ChatItem';

export type {
  SearchRecommendItem,
  FieldType,
  DomainInfoType,
  EntityInfoType,
  DateInfoType,
  ChatContextType,
  MsgValidTypeEnum,
  MsgDataType,
  ColumnType,
  SuggestionItemType,
  SuggestionType,
  SuggestionDataType,
  FilterItemType,
  HistoryType,
  HistoryMsgItemType,
} from './common/type';

export { getHistoryMsg, searchRecommend, queryContext } from './service';

export { setToken } from './utils/utils';
