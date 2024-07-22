import './styles/index.less';

import ReactDOM from 'react-dom/client';
import Chat from './demo/Chat';
import ChatDemo from './demo/ChatDemo';
import CopilotDemo from './demo/CopilotDemo';
const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);
root.render(<ChatDemo />);

