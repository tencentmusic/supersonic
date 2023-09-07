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
  const [htmlCode, setHtmlCode] = useState<string>();
  const [referenceExpanded, setRederenceExpanded] = useState(false);
  const [referenceData, setReferenceData] = useState<any[]>([]);

  const prefixCls = `${CLS_PREFIX}-text`;

  const initData = () => {
    let textValue = dataSource[0][columns[0].nameEn];
    let htmlCodeValue: string;
    const match = textValue.match(/```html([\s\S]*?)```/);
    htmlCodeValue = match && match[1].trim();
    if (htmlCodeValue) {
      textValue = textValue.replace(/```html([\s\S]*?)```/, '');
    }
    let scriptCode: string;
    let scriptSrc: string;
    if (htmlCodeValue) {
      scriptSrc = htmlCodeValue.match(/<script src="([\s\S]*?)"><\/script>/)?.[1] || '';
      scriptCode =
        htmlCodeValue.match(/<script type="text\/javascript">([\s\S]*?)<\/script>/)?.[1] || '';
      if (scriptSrc) {
        const script = document.createElement('script');
        script.src = scriptSrc;
        document.body.appendChild(script);
      }
      if (scriptCode) {
        const script = document.createElement('script');
        script.innerHTML = scriptCode;
        setTimeout(() => {
          document.body.appendChild(script);
        }, 1500);
      }
    }
    setText(textValue);
    setHtmlCode(htmlCodeValue);
    if (referenceColumn) {
      const referenceDataValue = dataSource[0][referenceColumn.nameEn];
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
      {htmlCode ? <pre>{text}</pre> : text}
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
      {!!htmlCode && <div dangerouslySetInnerHTML={{ __html: htmlCode }} />}
    </div>
  );
};

export default Text;
