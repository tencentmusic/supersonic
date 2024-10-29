import React, { useState } from 'react';
import { format } from 'sql-formatter';
import { CopyToClipboard } from 'react-copy-to-clipboard';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { solarizedlight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Button, message } from 'antd';
import { PREFIX_CLS } from '../../common/constants';
import { CheckCircleFilled, DownloadOutlined, UpOutlined } from '@ant-design/icons';
import { SqlInfoType } from '../../common/type';
import { exportTextFile } from '../../utils/utils';

type Props = {
  agentId?: number;
  queryId?: number;
  question: string;
  llmReq?: any;
  llmResp?: any;
  integrateSystem?: string;
  queryMode?: string;
  sqlInfo: SqlInfoType;
  sqlTimeCost?: number;
  executeErrorMsg: string;
};

const SqlItem: React.FC<Props> = ({
  agentId,
  queryId,
  question,
  llmReq,
  llmResp,
  integrateSystem,
  queryMode,
  sqlInfo,
  sqlTimeCost,
  executeErrorMsg,
}) => {
  const [sqlType, setSqlType] = useState('');

  const tipPrefixCls = `${PREFIX_CLS}-item`;
  const prefixCls = `${PREFIX_CLS}-sql-item`;

  const handleCopy = (_: string, result: any) => {
    result ? message.success('复制SQL成功', 1) : message.error('复制SQL失败', 1);
  };

  const onCollapse = () => {
    setSqlType('');
  };

  if (!llmReq && !sqlInfo.parsedS2SQL && !sqlInfo.correctedS2SQL && !sqlInfo.querySQL) {
    return null;
  }

  const { schema, terms, priorExts } = llmReq || {};

  const fewShots = (Object.values(llmResp?.sqlRespMap || {})[0] as any)?.fewShots || [];

  const getSchemaMapText = () => {
    return `
Schema映射
${schema?.fieldNameList?.length > 0 ? `名称：${schema.fieldNameList.join('、')}` : ''}${
      schema?.values?.length > 0
        ? `
取值：${schema.values
            .map((item: any) => {
              return `${item.fieldName}: ${item.fieldValue}`;
            })
            .join('、')}`
        : ''
    }${
      priorExts
        ? `
附加：${priorExts}`
        : ''
    }${
      terms?.length > 0
        ? `
术语：${terms
            .map((item: any) => {
              return `${item.name}${item.alias?.length > 0 ? `(${item.alias.join(',')})` : ''}: ${
                item.description
              }`;
            })
            .join('、')}`
        : ''
    }

`;
  };

  const getFewShotText = () => {
    return `
Few-shot示例${fewShots
      .map((item: any, index: number) => {
        return `

示例${index + 1}：
问题：${item.question}
SQL：
${format(item.sql)}
`;
      })
      .join('')}
`;
  };

  const getParsedS2SQLText = () => {
    return `
${queryMode === 'LLM_S2SQL' || queryMode === 'PLAIN_TEXT' ? 'LLM' : 'Rule'}解析S2SQL

${format(sqlInfo.parsedS2SQL)}
`;
  };

  const getCorrectedS2SQLText = () => {
    return `
修正S2SQL

${format(sqlInfo.correctedS2SQL)}
`;
  };

  const getQuerySQLText = () => {
    return `
最终执行SQL

${format(sqlInfo.querySQL)}
`;
  };

  const getErrorMsgText = () => {
    return `
异常日志

${executeErrorMsg}
`;
  };

  const onExportLog = () => {
    let text = '';
    if (question) {
      text += `
问题：${question}
`;
    }
    if (llmReq) {
      text += getSchemaMapText();
    }
    if (fewShots.length > 0) {
      text += getFewShotText();
    }
    if (sqlInfo.parsedS2SQL) {
      text += getParsedS2SQLText();
    }
    if (sqlInfo.correctedS2SQL) {
      text += getCorrectedS2SQLText();
    }
    if (sqlInfo.querySQL) {
      text += getQuerySQLText();
    }
    if (!!executeErrorMsg) {
      text += getErrorMsgText();
    }
    exportTextFile(text, `supersonic-debug-${agentId}-${queryId}.log`);
  };

  return (
    <div className={`${tipPrefixCls}-parse-tip`}>
      <div className={`${tipPrefixCls}-title-bar`}>
        <CheckCircleFilled className={`${tipPrefixCls}-step-icon`} />
        <div className={`${tipPrefixCls}-step-title`}>
          SQL生成
          {!!sqlTimeCost && (
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
          {fewShots.length > 0 && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'fewShots' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'fewShots' ? '' : 'fewShots');
              }}
            >
              Few-shot示例
            </div>
          )}
          {sqlInfo.parsedS2SQL && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'parsedS2SQL' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'parsedS2SQL' ? '' : 'parsedS2SQL');
              }}
            >
              {queryMode === 'LLM_S2SQL' || queryMode === 'PLAIN_TEXT' ? 'LLM' : 'Rule'}解析S2SQL
            </div>
          )}
          {sqlInfo.correctedS2SQL && (
            <div
              className={`${tipPrefixCls}-content-option ${
                sqlType === 'correctedS2SQL' ? `${tipPrefixCls}-content-option-active` : ''
              }`}
              onClick={() => {
                setSqlType(sqlType === 'correctedS2SQL' ? '' : 'correctedS2SQL');
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
              最终执行SQL
            </div>
          )}
          <Button className={`${prefixCls}-export-log`} size="small" onClick={onExportLog}>
            <DownloadOutlined />
            导出日志
          </Button>
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
            {schema?.values?.length > 0 && (
              <div className={`${prefixCls}-schema-row`}>
                <div className={`${prefixCls}-schema-title`}>取值：</div>
                <div className={`${prefixCls}-schema-content`}>
                  {schema.values
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
            {terms?.length > 0 && (
              <div className={`${prefixCls}-schema-row`}>
                <div className={`${prefixCls}-schema-title`}>术语：</div>
                <div className={`${prefixCls}-schema-content`}>
                  {terms
                    .map((item: any) => {
                      return `${item.name}${
                        item.alias?.length > 0 ? `(${item.alias.join(',')})` : ''
                      }: ${item.description}`;
                    })
                    .join('、')}
                </div>
              </div>
            )}
          </div>
        )}
        {sqlType === 'fewShots' && (
          <div className={`${prefixCls}-code`}>
            {fewShots.map((item: any, index: number) => {
              return (
                <div key={index} className={`${prefixCls}-few-shot-item`}>
                  <div className={`${prefixCls}-few-shot-title`}>示例{index + 1}：</div>
                  <div className={`${prefixCls}-few-shot-content`}>
                    <div className={`${prefixCls}-few-shot-content-item`}>
                      <div className={`${prefixCls}-few-shot-content-title`}>问题：</div>
                      <div className={`${prefixCls}-few-shot-content-text`}>{item.question}</div>
                    </div>
                    <div className={`${prefixCls}-few-shot-content-item`}>
                      <div className={`${prefixCls}-few-shot-content-title`}>SQL：</div>
                      <div className={`${prefixCls}-few-shot-content-text`}>
                        <SyntaxHighlighter
                          className={`${prefixCls}-few-shot-code`}
                          language="sql"
                          style={solarizedlight}
                        >
                          {item.sql}
                        </SyntaxHighlighter>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
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
