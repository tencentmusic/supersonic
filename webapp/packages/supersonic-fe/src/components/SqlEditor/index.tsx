/* eslint-disable */

import React, { useRef, useEffect, useCallback, useState, useMemo } from 'react';
import AceEditor, { IAceOptions } from 'react-ace';
import languageTools from 'ace-builds/src-min-noconflict/ext-language_tools';
import 'ace-builds/src-min-noconflict/ext-searchbox';
import 'ace-builds/src-min-noconflict/theme-sqlserver';
import 'ace-builds/src-min-noconflict/theme-monokai';
import 'ace-builds/src-min-noconflict/mode-sql';
import ReactAce, { IAceEditorProps } from 'react-ace/lib/ace';
import { Typography } from 'antd';
import { debounce } from 'lodash';
import FullScreen from '../FullScreen';
import styles from './index.less';

type TMode = 'sql' | 'mysql' | 'sqlserver';

enum EHintMeta {
  table = 'table',
  variable = 'variable',
  column = 'column',
}
const DEFAULT_FONT_SIZE = '14px';
// const THEME_DEFAULT = 'sqlserver';
const MODE_DEFAULT = 'sql';
// const HEIGHT_DEFAULT = '300px';
const HEIGHT_DEFAULT = '100%';
const EDITOR_OPTIONS: IAceOptions = {
  behavioursEnabled: true,
  enableSnippets: false,
  enableBasicAutocompletion: true,
  enableLiveAutocompletion: true,
  autoScrollEditorIntoView: true,
  wrap: true,
  useWorker: false,
};
export interface ISqlEditorProps {
  hints?: { [name: string]: string[] };
  value?: string;
  height?: string;
  /**
   * 需引入对应的包 'ace-builds/src-min-noconflict/mode-${mode}'
   */
  mode?: TMode;
  /**
   * 需引入对应的包 'ace-builds/src-min-noconflict/theme-${theme}'
   */
  // theme?: TTheme;
  isRightTheme?: boolean;
  editorConfig?: IAceEditorProps;
  sizeChanged?: number;
  isFullScreen?: boolean;
  fullScreenBtnVisible?: boolean;
  onSqlChange?: (sql: string) => void;
  onChange?: (sql: string) => void;
  onSelect?: (sql: string) => void;
  onCmdEnter?: () => void;
  triggerBackToNormal?: () => void;
}

/**
 * Editor Component
 * @param props ISqlEditorProps
 */
function SqlEditor(props: ISqlEditorProps) {
  const refEditor = useRef<ReactAce>();
  const {
    hints = {},
    value,
    height = HEIGHT_DEFAULT,
    mode = MODE_DEFAULT,
    isRightTheme = false,
    sizeChanged,
    editorConfig,
    fullScreenBtnVisible = true,
    isFullScreen = false,
    onSqlChange,
    onChange,
    onSelect,
    onCmdEnter,
    triggerBackToNormal,
  } = props;
  const resize = useCallback(
    debounce(() => {
      refEditor.current?.editor.resize();
    }, 300),
    [],
  );

  const change = useCallback((sql: string) => {
    onSqlChange?.(sql);
    onChange?.(sql);
  }, []);

  const selectionChange = useCallback(
    debounce((selection: any) => {
      const rawSelectedQueryText: any = refEditor.current?.editor.session.doc.getTextRange(
        selection.getRange(),
      );
      const selectedQueryText = rawSelectedQueryText?.length > 1 ? rawSelectedQueryText : null;
      onSelect?.(selectedQueryText);
    }, 300),
    [],
  );

  const commands = useMemo(
    () => [
      {
        name: 'execute',
        bindKey: { win: 'Ctrl-Enter', mac: 'Command-Enter' },
        exec: onCmdEnter,
      },
    ],
    [],
  );

  useEffect(() => {
    resize();
  }, [sizeChanged, height]);

  useEffect(() => {
    setHintsPopover(hints);
  }, [hints]);

  const [isSqlIdeFullScreen, setIsSqlIdeFullScreen] = useState<boolean>(isFullScreen);

  useEffect(() => {
    setIsSqlIdeFullScreen(isFullScreen);
  }, [isFullScreen]);

  const handleNormalScreenSqlIde = () => {
    setIsSqlIdeFullScreen(false);
    triggerBackToNormal?.();
  };
  return (
    <div className={styles.sqlEditor} style={{ height }}>
      <FullScreen
        isFullScreen={isSqlIdeFullScreen}
        top={`${0}px`}
        triggerBackToNormal={handleNormalScreenSqlIde}
      >
        <AceEditor
          ref={refEditor}
          name="aceEditor"
          width="100%"
          height="100%"
          fontSize={DEFAULT_FONT_SIZE}
          mode={mode}
          theme={isRightTheme ? 'sqlserver' : 'monokai'}
          value={value}
          showPrintMargin={false}
          highlightActiveLine={true}
          setOptions={EDITOR_OPTIONS}
          commands={commands as any}
          onChange={change}
          onSelectionChange={selectionChange}
          // autoScrollEditorIntoView={true}
          {...editorConfig}
        />
      </FullScreen>
      {fullScreenBtnVisible && (
        <span
          className={styles.fullScreenBtnBox}
          onClick={() => {
            setIsSqlIdeFullScreen(true);
          }}
        >
          <Typography.Link>全屏查看</Typography.Link>
        </span>
      )}
    </div>
  );
}

interface ICompleters {
  value: string;
  name?: string;
  caption?: string;
  meta?: string;
  type?: string;
  score?: number;
}

function setHintsPopover(hints: ISqlEditorProps['hints']) {
  const {
    textCompleter,
    keyWordCompleter,
    // snippetCompleter,
    setCompleters,
  } = languageTools;
  const customHintsCompleter = {
    identifierRegexps: [/[a-zA-Z_0-9.\-\u00A2-\uFFFF]/],
    getCompletions: (editor, session, pos, prefix, callback) => {
      const { tableKeywords, tableColumnKeywords, variableKeywords, columns } =
        formatCompleterFromHints(hints);
      if (prefix[prefix.length - 1] === '.') {
        const tableName = prefix.substring(0, prefix.length - 1);
        const AliasTableColumnKeywords = genAliasTableColumnKeywords(editor, tableName, hints);
        const hintList = tableKeywords.concat(
          variableKeywords,
          AliasTableColumnKeywords,
          tableColumnKeywords[tableName] || [],
        );
        return callback(null, hintList);
      }
      callback(null, tableKeywords.concat(variableKeywords, columns));
    },
  };
  const completers = [
    textCompleter,
    keyWordCompleter,
    // snippetCompleter,
    customHintsCompleter,
  ];
  setCompleters(completers);
}

function formatCompleterFromHints(hints: ISqlEditorProps['hints']) {
  const variableKeywords: ICompleters[] = [];
  const tableKeywords: ICompleters[] = [];
  const tableColumnKeywords: { [tableName: string]: ICompleters[] } = {};
  const columns: ICompleters[] = [];
  let score = 1000;
  Object.keys(hints).forEach((key) => {
    const meta: EHintMeta = isVariable(key) as any;
    if (!meta) {
      const { columnWithTableName, column } = genTableColumnKeywords(hints[key], key);
      tableColumnKeywords[key] = columnWithTableName;
      columns.push(...column);
      tableKeywords.push({
        name: key,
        value: key,
        score: score--,
        meta: isTable(),
      });
    } else {
      variableKeywords.push({ score: score--, value: key, meta });
    }
  });

  return { tableKeywords, tableColumnKeywords, variableKeywords, columns };
}

function genTableColumnKeywords(table: string[], tableName: string) {
  let score = 100;
  const columnWithTableName: ICompleters[] = [];
  const column: ICompleters[] = [];
  table.forEach((columnVal) => {
    const basis = { score: score--, meta: isColumn() };
    columnWithTableName.push({
      caption: `${tableName}.${columnVal}`,
      name: `${tableName}.${columnVal}`,
      value: `${tableName}.${columnVal}`,
      ...basis,
    });
    column.push({ value: columnVal, name: columnVal, ...basis });
  });
  return { columnWithTableName, column };
}

function genAliasTableColumnKeywords(
  editor,
  aliasTableName: string,
  hints: ISqlEditorProps['hints'],
) {
  const content = editor.getSession().getValue();
  const tableName = Object.keys(hints).find((tableName) => {
    const reg = new RegExp(`.+${tableName}\\s*(as|AS)?(?=\\s+${aliasTableName}\\s*)`, 'im');
    return reg.test(content);
  });
  if (!tableName) {
    return [];
  }
  const { columnWithTableName } = genTableColumnKeywords(hints[tableName], aliasTableName);
  return columnWithTableName;
}

function isVariable(key: string) {
  return key.startsWith('$') && key.endsWith('$') && EHintMeta.variable;
}

function isTable(key?: string) {
  return EHintMeta.table;
}

function isColumn(key?: string) {
  return EHintMeta.column;
}

export default SqlEditor;
