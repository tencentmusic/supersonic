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
};

const SqlItem: React.FC<Props> = ({ integrateSystem, sqlInfo }) => {
  const [sqlType, setSqlType] = useState('');

  const tipPrefixCls = `${PREFIX_CLS}-item`;
  const prefixCls = `${PREFIX_CLS}-sql-item`;

  const handleCopy = (text, result) => {
    result ? message.success('复制SQL成功', 1) : message.error('复制SQL失败', 1);
  };

  const onCollapse = () => {
    setSqlType('');
  };

  if (!sqlInfo.s2QL && !sqlInfo.logicSql && !sqlInfo.querySql) {
    return null;
  }

  return (
    <div className={`${tipPrefixCls}-parse-tip`}>
      <div className={`${tipPrefixCls}-title-bar`}>
        <CheckCircleFilled className={`${tipPrefixCls}-step-icon`} />
        <div className={`${tipPrefixCls}-step-title`}>
          SQL生成：
          {sqlType && (
            <span className={`${prefixCls}-toggle-expand-btn`} onClick={onCollapse}>
              <UpOutlined />
            </span>
          )}
        </div>
        <div className={`${tipPrefixCls}-content-options`}>
          {sqlInfo.s2QL && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 's2QL' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 's2QL' ? '' : 's2QL');
              }}
            >
              S2QL
            </div>
          )}
          {sqlInfo.logicSql && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'logicSql' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'logicSql' ? '' : 'logicSql');
              }}
            >
              逻辑SQL
            </div>
          )}
          {sqlInfo.querySql && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'querySql' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'querySql' ? '' : 'querySql');
              }}
            >
              物理SQL
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
        {sqlType && (
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
