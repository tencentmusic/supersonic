import { MsgDataType } from '../../../common/type';
import { CLS_PREFIX } from '../../../common/constants';
import { SizeType } from 'antd/es/config-provider/SizeContext';
import rehypeHighlight from 'rehype-highlight';
import remarkGfm from 'remark-gfm';
import Markdown from 'react-markdown';
import 'github-markdown-css/github-markdown.css';
import 'highlight.js/styles/github.css';

type Props = {
  // data: MsgDataType;
  // size?: SizeType;
  markdown: string;
  loading?: boolean;
  onApplyAuth?: (model: string) => void;
};

const MarkDown: React.FC<Props> = ({ markdown, loading, onApplyAuth }) => {
  // const { textResult } = data;
  const prefixCls = `${CLS_PREFIX}-markdown`;

  return (
    <div className={`${prefixCls} markdown-body`} style={{ fontSize: 14 }}>
      <Markdown rehypePlugins={[rehypeHighlight]} remarkPlugins={[remarkGfm]}>
        {markdown}
      </Markdown>
    </div>
  );
};

export default MarkDown;
