import './styles/index.less';

// import ReactDOM from 'react-dom/client';
// import Chat from './demo/Chat';
// import ChatDemo from './demo/ChatDemo';
// import CopilotDemo from './demo/CopilotDemo';
// const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);
// root.render(<ChatDemo />);

export { default as Chat } from './Chat';

export { default as Copilot } from './Copilot';

export { default as ChatMsg } from './components/ChatMsg';

export { default as ChatItem } from './components/ChatItem';

export { default as ShowCase } from './ShowCase';

export type {
  SearchRecommendItem,
  FieldType,
  ModelInfoType,
  EntityInfoType,
  DateInfoType,
  ChatContextType,
  MsgValidTypeEnum,
  MsgDataType,
  PluginResonseType,
  ColumnType,
  SuggestionItemType,
  SuggestionType,
  SuggestionDataType,
  FilterItemType,
  HistoryType,
  HistoryMsgItemType,
  SendMsgParamsType,
} from './common/type';

export { searchRecommend } from './service';

export { saveConversation, getAllConversations } from './Chat/service';

export { setToken } from './utils/utils';
