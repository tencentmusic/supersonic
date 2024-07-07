import { EditOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import { prefixCls } from '.';

type Props = {
  text: string;
  onClickEdit: () => void;
};

function Explain({ text, onClickEdit }: Props) {
  return (
    <div className={`${prefixCls}-explain`}>
      {text}{' '}
      <Tooltip title="修改查询条件" placement="top">
        <EditOutlined style={{ color: 'blue', marginLeft: '5px' }} onClick={onClickEdit} />
      </Tooltip>
    </div>
  );
}

export default Explain;
