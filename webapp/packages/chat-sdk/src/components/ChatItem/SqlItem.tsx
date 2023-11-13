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
  llmReq?: any;
  integrateSystem?: string;
  sqlInfo: SqlInfoType;
  sqlTimeCost?: number;
};

const SqlItem: React.FC<Props> = ({ llmReq, integrateSystem, sqlInfo, sqlTimeCost }) => {
  const [sqlType, setSqlType] = useState('');

  const tipPrefixCls = `${PREFIX_CLS}-item`;
  const prefixCls = `${PREFIX_CLS}-sql-item`;

  const handleCopy = (_: string, result: any) => {
    result ? message.success('复制SQL成功', 1) : message.error('复制SQL失败', 1);
  };

  const onCollapse = () => {
    setSqlType('');
  };

  if (!llmReq && !sqlInfo.s2SQL && !sqlInfo.correctS2SQL && !sqlInfo.querySQL) {
    return null;
  }

  const { schema, linking, priorExts } = llmReq || {};

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
          {llmReq && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'schemaMap' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'schemaMap' ? '' : 'schemaMap');
              }}
            >
              Schema映射
            </div>
          )}
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
        {sqlType === 'schemaMap' && (
          <div className={`${prefixCls}-code`}>
            {schema?.fieldNameList?.length > 0 && (
              <div className={`${prefixCls}-schema-row`}>
                <div className={`${prefixCls}-schema-title`}>名称：</div>
                <div className={`${prefixCls}-schema-content`}>
                  {schema.fieldNameList.join('、')}
                </div>
              </div>
            )}
            {linking?.length > 0 && (
              <div className={`${prefixCls}-schema-row`}>
                <div className={`${prefixCls}-schema-title`}>取值：</div>
                <div className={`${prefixCls}-schema-content`}>
                  {linking
                    .map((item: any) => {
                      return `${item.fieldName}: ${item.fieldValue}`;
                    })
                    .join('、')}
                </div>
              </div>
            )}
            {priorExts && (
              <div className={`${prefixCls}-schema-row`}>
                <div className={`${prefixCls}-schema-title`}>附加：</div>
                <div className={`${prefixCls}-schema-content`}>{priorExts}</div>
              </div>
            )}
          </div>
        )}
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
