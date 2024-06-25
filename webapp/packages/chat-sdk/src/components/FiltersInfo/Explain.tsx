import { EditOutlined } from '@ant-design/icons';
import { prefixCls } from '.';

type Props = {
  text: string;
  onClickEdit: () => void;
};

function Explain({ text, onClickEdit }: Props) {
  return (
    <div className={`${prefixCls}-explain`}>
      {text} <EditOutlined style={{ color: 'blue', marginLeft: '5px' }} onClick={onClickEdit} />
    </div>
  );
}

export default Explain;
