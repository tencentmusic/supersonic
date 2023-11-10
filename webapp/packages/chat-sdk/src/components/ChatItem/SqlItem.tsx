import React, { useState } from 'react';
import { format } from 'sql-formatter';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { solarizedlight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { message } from 'antd';
import { PREFIX_CLS } from '../../common/constants';
import { CheckCircleFilled, UpOutlined } from '@ant-design/icons';
import { SqlInfoType } from '../../common/type';

type Props = {
  integrateSystem?: string;
  sqlInfo: SqlInfoType;
  sqlTimeCost?: number;
};

const SqlItem: React.FC<Props> = ({ integrateSystem, sqlInfo, sqlTimeCost }) => {
  const [sqlType, setSqlType] = useState('');

  const tipPrefixCls = `${PREFIX_CLS}-item`;
  const prefixCls = `${PREFIX_CLS}-sql-item`;

  const handleCopy = (text, result) => {
    result ? message.success('复制SQL成功', 1) : message.error('复制SQL失败', 1);
  };

  const onCollapse = () => {
    setSqlType('');
  };

  if (!sqlInfo.s2SQL && !sqlInfo.correctS2SQL && !sqlInfo.querySQL) {
    return null;
  }

  return (
    <div className={`${tipPrefixCls}-parse-tip`}>
      <div className={`${tipPrefixCls}-title-bar`}>
        <CheckCircleFilled className={`${tipPrefixCls}-step-icon`} />
        <div className={`${tipPrefixCls}-step-title`}>
          SQL生成
          {sqlTimeCost && (
            <span className={`${tipPrefixCls}-title-tip`}>(耗时: {sqlTimeCost}ms)</span>
          )}
          ：
          {sqlType && (
            <span className={`${prefixCls}-toggle-expand-btn`} onClick={onCollapse}>
              <UpOutlined />
            </span>
          )}
        </div>
        <div className={`${tipPrefixCls}-content-options`}>
          {sqlInfo.s2SQL && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 's2SQL' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 's2SQL' ? '' : 's2SQL');
              }}
            >
              解析S2SQL
            </div>
          )}
          {sqlInfo.correctS2SQL && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'correctS2SQL' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'correctS2SQL' ? '' : 'correctS2SQL');
              }}
            >
              修正S2SQL
            </div>
          )}
          {sqlInfo.querySQL && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'querySQL' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'querySQL' ? '' : 'querySQL');
              }}
            >
              执行SQL
            </div>
          )}
        </div>
      </div>
      <div
        className={`${prefixCls} ${
          !window.location.pathname.includes('/chat') &&
          integrateSystem &&
          integrateSystem !== 'wiki'
            ? `${prefixCls}-copilot`
            : ''
        }`}
      >
        {sqlType && sqlInfo[sqlType] && (
          <>
            <SyntaxHighlighter
              className={`${prefixCls}-code`}
              language="sql"
              style={solarizedlight}
            >
              {format(sqlInfo[sqlType])}
            </SyntaxHighlighter>
            <CopyToClipboard
              text={format(sqlInfo[sqlType])}
              onCopy={(text, result) => handleCopy(text, result)}
            >
              <button className={`${prefixCls}-copy-btn`}>复制代码</button>
            </CopyToClipboard>
          </>
        )}
      </div>
    </div>
  );
};

export default SqlItem;
