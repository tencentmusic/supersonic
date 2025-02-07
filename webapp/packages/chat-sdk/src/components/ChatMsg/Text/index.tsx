import { useEffect, useState } from 'react';
import { UpOutlined, DownOutlined } from '@ant-design/icons';
import { CLS_PREFIX } from '../../../common/constants';
import { ColumnType } from '../../../common/type';

type Props = {
  columns: ColumnType[];
  referenceColumn?: ColumnType;
  dataSource: any[];
};

const Text: React.FC<Props> = ({ columns, referenceColumn, dataSource }) => {
  const [text, setText] = useState<string>();
  const [referenceExpanded, setRederenceExpanded] = useState(false);
  const [referenceData, setReferenceData] = useState<any[]>([]);

  const prefixCls = `${CLS_PREFIX}-text`;

  const initData = () => {
    let textValue = dataSource[0][columns[0].bizName];
    setText(textValue === undefined ? '暂无数据' : textValue);
    if (referenceColumn) {
      const referenceDataValue = dataSource[0][referenceColumn.bizName];
      setReferenceData(referenceDataValue || []);
    }
  };

  useEffect(() => {
    initData();
  }, []);

  const onToggleMore = () => {
    setRederenceExpanded(!referenceExpanded);
  };

  return (
    <div
      style={{
        lineHeight: '24px',
        width: 'fit-content',
        maxWidth: '100%',
        overflowX: 'hidden',
      }}
    >
      {text}
      {referenceData.length > 0 && (
        <span className={`${prefixCls}-check-more`} onClick={onToggleMore}>
          {referenceExpanded ? '收起' : '查看'}更多
          {referenceExpanded ? (
            <UpOutlined className={`${prefixCls}-arrow-icon`} />
          ) : (
            <DownOutlined className={`${prefixCls}-arrow-icon`} />
          )}
        </span>
      )}
      {referenceData.length > 0 && referenceExpanded && (
        <div className={`${prefixCls}-reference-data`}>
          {referenceData.map(item => (
            <div className={`${prefixCls}-reference-item`} key={item.doc_title}>
              <div className={`${prefixCls}-reference-item-title`}>{item.doc_title}</div>
              <div className={`${prefixCls}-reference-item-value`}>{item.doc_chunk}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Text;
