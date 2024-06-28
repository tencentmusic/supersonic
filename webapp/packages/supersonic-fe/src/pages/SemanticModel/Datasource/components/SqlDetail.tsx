import React, { useState, useEffect, useRef } from 'react';
import { Button, Table, message, Tooltip, Space, Dropdown } from 'antd';
import SplitPane from 'react-split-pane';
import Pane from 'react-split-pane/lib/Pane';
import { useModel } from '@umijs/max';
import { format } from 'sql-formatter';
import {
  FullscreenOutlined,
  WarningOutlined,
  EditOutlined,
  PlayCircleTwoTone,
  SwapOutlined,
  PlayCircleOutlined,
  CloudServerOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import { isFunction } from 'lodash';
import FullScreen from '@/components/FullScreen';
import SqlEditor from '@/components/SqlEditor';
import type { TaskResultItem, TaskResultColumn } from '../data';
import { excuteSql } from '@/pages/SemanticModel/service';
import type { StateType } from '../../model';
import SqlParams from './SqlParams';
import styles from '../style.less';
import 'ace-builds/src-min-noconflict/ext-searchbox';
import 'ace-builds/src-min-noconflict/theme-sqlserver';
import 'ace-builds/src-min-noconflict/theme-monokai';
import 'ace-builds/src-min-noconflict/mode-sql';
import { IDataSource, ISemantic } from '../../data';

export type DataSourceSubmitData = {
  sql: string;
  databaseId: number;
  columns: any[];
  sqlParams: any[];
};

type IProps = {
  dataSourceItem: IDataSource.IDataSourceItem;
  onUpdateSql?: (sql: string) => void;
  sql?: string;
  onSubmitSuccess?: (dataSourceInfo: DataSourceSubmitData) => void;
};

type ResultTableItem = Record<string, any>;

type ResultColItem = {
  key: string;
  title: string;
  dataIndex: string;
};

type ScreenSize = 'small' | 'middle' | 'large';

type DatabaseItem = {
  label: string;
  key: number;
};

const SqlDetail: React.FC<IProps> = ({
  dataSourceItem,
  onSubmitSuccess,
  sql = '',
  onUpdateSql,
}) => {
  const databaseModel = useModel('SemanticModel.databaseData');
  const { databaseConfigList } = databaseModel;

  const [resultTable, setResultTable] = useState<ResultTableItem[]>([]);
  const [resultTableLoading, setResultTableLoading] = useState(false);
  const [resultCols, setResultCols] = useState<ResultColItem[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [dataBaseItems, setDataBaseItems] = useState<DatabaseItem[]>([]);
  const [currentDatabaseItem, setCurrentDatabaseItem] = useState<DatabaseItem>();

  const [tableScroll, setTableScroll] = useState({
    scrollToFirstRowOnChange: true,
    x: '100%',
    y: 200,
  });

  const [runState, setRunState] = useState<boolean | undefined>();

  const [taskLog, setTaskLog] = useState('');
  const [isSqlExcLocked, setIsSqlExcLocked] = useState<boolean>(false);
  const [screenSize, setScreenSize] = useState<ScreenSize>('middle');

  const [isSqlIdeFullScreen, setIsSqlIdeFullScreen] = useState<boolean>(false);
  const [isSqlResFullScreen, setIsSqlResFullScreen] = useState<boolean>(false);

  const resultInnerWrap = useRef<HTMLDivElement>();

  const [editorSize, setEditorSize] = useState<number>(0);
  const DEFAULT_FULLSCREEN_TOP = 0;

  const [partialSql, setPartialSql] = useState('');
  const [isPartial, setIsPartial] = useState<boolean>(false);
  const [isRight, setIsRight] = useState<boolean>(false);

  const [variableCollapsed, setVariableCollapsed] = useState<boolean>(true);
  const [sqlParams, setSqlParams] = useState<IDataSource.ISqlParamsItem[]>([]);

  const [scriptColumns, setScriptColumns] = useState<any[]>([]);

  useEffect(() => {
    const list = databaseConfigList.map((item: ISemantic.IDatabaseItem) => {
      return {
        label: item.name,
        key: item.id,
        disabled: !item.hasUsePermission,
      };
    });
    setDataBaseItems(list);
    let targetDataBase = list[0];
    if (dataSourceItem?.id) {
      const { databaseId } = dataSourceItem;
      const target = list.find((item) => item.key === databaseId);
      if (target) {
        targetDataBase = target;
      }
    }
    setCurrentDatabaseItem(targetDataBase);
  }, [dataSourceItem, databaseConfigList]);

  useEffect(() => {
    setSqlParams(dataSourceItem?.modelDetail?.sqlVariables || []);
  }, [dataSourceItem]);

  useEffect(() => {
    setRunState(undefined);
  }, [currentDatabaseItem, sql]);

  function creatCalcItem(key: string, data: string) {
    const line = document.createElement('div'); // 需要每条数据一行，这样避免数据换行的时候获得的宽度不准确
    const child = document.createElement('span');
    child.classList.add(`resultCalcItem_${key}`);
    child.innerText = data;
    line.appendChild(child);
    return line;
  }

  const handleVariable = () => {
    const collapsedValue = !variableCollapsed;
    setVariableCollapsed(collapsedValue);
  };

  // 计算每列的宽度，通过容器插入文档中动态得到该列数据(包括表头)的最长宽度，设为列宽度，保证每列的数据都能一行展示完
  function getKeyWidthMap(list: TaskResultItem[]): TaskResultItem {
    const widthMap = {};
    const container = document.createElement('div');
    container.id = 'resultCalcWrap';
    container.style.position = 'fixed';
    container.style.left = '-99999px';
    container.style.top = '-99999px';
    container.style.width = '19999px';
    container.style.fontSize = '12px';
    list.forEach((item, index) => {
      if (index === 0) {
        Object.keys(item).forEach((key, keyIndex) => {
          // 因为key可能存在一些特殊字符，导致querySelectorAll获取的时候报错，所以用keyIndex(而不用key)拼接className
          container.appendChild(creatCalcItem(`${keyIndex}`, key));
          container.appendChild(creatCalcItem(`${keyIndex}`, `${item[key]}`));
        });
      } else {
        Object.keys(item).forEach((key, keyIndex) => {
          container.appendChild(creatCalcItem(`${keyIndex}`, `${item[key]}`));
        });
      }
    });
    document.body.appendChild(container);
    Object.keys(list[0]).forEach((key, keyIndex) => {
      // 因为key可能存在一些特殊字符，导致querySelectorAll获取的时候报错，所以用keyIndex(而不用key)拼接className
      const widthArr = Array.from(container.querySelectorAll(`.resultCalcItem_${keyIndex}`)).map(
        (node: any) => node.offsetWidth,
      );
      widthMap[key] = Math.max(...widthArr);
    });
    document.body.removeChild(container);
    return widthMap;
  }

  const updateResultCols = (list: TaskResultItem[], columns: TaskResultColumn[]) => {
    if (list.length) {
      const widthMap = getKeyWidthMap(list);
      const cols = columns.map(({ nameEn }) => {
        return {
          key: nameEn,
          title: nameEn,
          dataIndex: nameEn,
          width: `${(widthMap[nameEn] as number) + 22}px`, // 字宽度 + 20px(比左右padding宽几像素，作为一个buffer值)
        };
      });
      setResultCols(cols);
    }
  };

  const fetchTaskResult = (params) => {
    setResultTable(
      params.resultList.map((item, index) => {
        return {
          ...item,
          index,
        };
      }),
    );
    setPagination({
      current: 1,
      pageSize: 20,
      total: params.resultList.length,
    });
    setScriptColumns(params.columns);
    updateResultCols(params.resultList, params.columns);
  };

  const changePaging = (paging: Pagination) => {
    setPagination({
      ...pagination,
      ...paging,
    });
  };

  const onSqlChange = (sqlString: string) => {
    if (onUpdateSql && isFunction(onUpdateSql)) {
      onUpdateSql(sqlString);
    }
  };

  const formatSQL = () => {
    const sqlvalue = format(sql);
    if (onUpdateSql && isFunction(onUpdateSql)) {
      onUpdateSql(sqlvalue);
    }
    // eslint-disable-next-line no-param-reassign
    sql = sqlvalue;
  };

  const separateSql = async (value: string) => {
    if (!currentDatabaseItem?.key) {
      return;
    }
    setResultTableLoading(true);
    const { code, data, msg } = await excuteSql({
      sql: value,
      id: currentDatabaseItem.key,
      sqlVariables: sqlParams,
    });
    setResultTableLoading(false);
    if (code === 200) {
      fetchTaskResult(data);
      setRunState(true);
    } else {
      setRunState(false);
      setTaskLog(msg);
    }
  };

  const onSelect = (value: string) => {
    if (value) {
      setIsPartial(true);
      setPartialSql(value);
    } else {
      setIsPartial(false);
    }
  };

  const excuteScript = () => {
    if (!sql) {
      return message.error('SQL查询语句不可以为空！');
    }
    if (isSqlExcLocked) {
      return message.warning('请间隔5s再重新执行！');
    }
    const waitTime = 5000;
    setIsSqlExcLocked(true); // 加锁，5s后再解锁
    setTimeout(() => {
      setIsSqlExcLocked(false);
    }, waitTime);

    return isPartial ? separateSql(partialSql) : separateSql(sql);
  };

  // const showDataSetModal = () => {
  //   setDataSourceModalVisible(true);
  // };

  // const startCreatDataSource = async () => {
  //   showDataSetModal();
  // };

  const updateNormalResScroll = () => {
    const node = resultInnerWrap?.current;
    if (node) {
      setTableScroll({
        scrollToFirstRowOnChange: true,
        x: '100%',
        y: node.clientHeight - 120,
      });
    }
  };

  const updateFullScreenResScroll = () => {
    const windowHeight = window.innerHeight;
    const paginationHeight = 96;
    setTableScroll({
      scrollToFirstRowOnChange: true,
      x: '100%',
      y: windowHeight - DEFAULT_FULLSCREEN_TOP - paginationHeight - 30, // 30为退出全屏按钮的高度
    });
  };

  const handleFullScreenSqlIde = () => {
    setIsSqlIdeFullScreen(true);
  };

  const handleNormalScreenSqlIde = () => {
    setIsSqlIdeFullScreen(false);
  };

  const handleFullScreenSqlResult = () => {
    setIsSqlResFullScreen(true);
  };

  const handleNormalScreenSqlResult = () => {
    setIsSqlResFullScreen(false);
  };

  const handleThemeChange = () => {
    setIsRight(!isRight);
  };

  const renderResult = () => {
    if (runState === false) {
      return (
        <>
          {
            <div className={styles.taskFailed}>
              <WarningOutlined className={styles.resultFailIcon} />
              任务执行失败
            </div>
          }
          <div
            className={styles.sqlResultLog}
            dangerouslySetInnerHTML={{
              __html: taskLog ? (
                taskLog.replace(/\r\n/g, '<br/>').replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;')
              ) : (
                <></>
              ),
            }}
          />
        </>
      );
    }

    if (runState) {
      return (
        <>
          <div className={styles.detail} />
          <Table<TaskResultItem>
            loading={resultTableLoading}
            dataSource={resultTable}
            columns={resultCols}
            onChange={changePaging}
            pagination={pagination}
            scroll={tableScroll}
            className={styles.resultTable}
            rowClassName="resultTableRow"
            rowKey="index"
          />
        </>
      );
    }
    return <div className={styles.sqlResultContent}>请点击左侧任务列表查看执行详情</div>;
  };

  // 更新任务结果列表的高度，使其撑满容器
  useEffect(() => {
    if (isSqlResFullScreen) {
      updateFullScreenResScroll();
    } else {
      updateNormalResScroll();
    }
  }, [resultTable, isSqlResFullScreen]);

  useEffect(() => {
    const windowHeight = window.innerHeight;
    let size: ScreenSize = 'small';
    if (windowHeight > 1100) {
      size = 'large';
    } else if (windowHeight > 850) {
      size = 'middle';
    }
    setScreenSize(size);
  }, []);

  return (
    <>
      <div className={styles.sqlOprBar}>
        <div className={styles.sqlOprBarLeftBox}>
          <Tooltip title="数据类型">
            <Dropdown
              menu={{
                items: dataBaseItems,
                onClick: (e) => {
                  const value = e.key;
                  const target: any = dataBaseItems.filter((item: any) => {
                    return item.key === Number(value);
                  })[0];
                  if (target) {
                    setCurrentDatabaseItem(target);
                  }
                },
              }}
              placement="bottom"
            >
              <Button style={{ marginRight: '15px', minWidth: '140px' }}>
                <Space>
                  <CloudServerOutlined className={styles.sqlOprIcon} style={{ marginRight: 0 }} />
                  <span>{currentDatabaseItem?.label}</span>
                </Space>
              </Button>
            </Dropdown>
          </Tooltip>
          <Tooltip title="全屏">
            <FullscreenOutlined className={styles.sqlOprIcon} onClick={handleFullScreenSqlIde} />
          </Tooltip>
          <Tooltip title="格式化SQL语句">
            <EditOutlined className={styles.sqlOprIcon} onClick={formatSQL} />
          </Tooltip>
          <Tooltip title="动态变量">
            <ApiOutlined className={styles.sqlOprIcon} onClick={handleVariable} />
          </Tooltip>
          <Tooltip title="改变主题">
            <SwapOutlined className={styles.sqlOprIcon} onClick={handleThemeChange} />
          </Tooltip>
          <Tooltip title="执行脚本">
            <Button
              style={{
                lineHeight: '24px',
                top: '3px',
                position: 'relative',
              }}
              type="primary"
              shape="round"
              icon={
                isPartial ? '' : isSqlExcLocked ? <PlayCircleOutlined /> : <PlayCircleTwoTone />
              }
              size={'small'}
              className={
                isSqlExcLocked ? `${styles.disableIcon} ${styles.sqlOprIcon}` : styles.sqlOprBtn
              }
              onClick={excuteScript}
            >
              {isPartial ? '部分运行' : '运行'}
            </Button>
          </Tooltip>
        </div>
      </div>
      <SplitPane
        split="horizontal"
        onChange={(size) => {
          setEditorSize(size);
        }}
      >
        <Pane initialSize={'500px'}>
          <div className={styles.sqlMain}>
            <div className={styles.sqlEditorWrapper}>
              <SqlEditor
                value={sql}
                isFullScreen={isSqlIdeFullScreen}
                triggerBackToNormal={handleNormalScreenSqlIde}
                // theme="monokai"
                isRightTheme={isRight}
                sizeChanged={editorSize}
                onSqlChange={onSqlChange}
                onSelect={onSelect}
              />
            </div>
            <div className={variableCollapsed ? styles.hideSqlParams : styles.sqlParams}>
              <SqlParams
                value={sqlParams}
                onChange={(params) => {
                  setSqlParams(params);
                }}
              />
            </div>
          </div>
        </Pane>
        <div className={`${styles.sqlBottmWrap} ${screenSize}`}>
          <div className={styles.sqlResultWrap}>
            <div className={styles.sqlToolBar}>
              <Button
                className={styles.sqlToolBtn}
                type="primary"
                onClick={() => {
                  onSubmitSuccess?.({
                    columns: scriptColumns,
                    databaseId: currentDatabaseItem?.key || 0,
                    sql,
                    sqlParams,
                  });
                }}
                disabled={!runState}
              >
                完成
              </Button>
              <Button
                className={styles.sqlToolBtn}
                type="primary"
                onClick={handleFullScreenSqlResult}
                disabled={!runState}
              >
                全屏查看
              </Button>
            </div>
            <div
              className={styles.sqlResultPane}
              ref={resultInnerWrap as React.MutableRefObject<HTMLDivElement | null>}
            >
              <FullScreen
                isFullScreen={isSqlResFullScreen}
                top={`${DEFAULT_FULLSCREEN_TOP}px`}
                triggerBackToNormal={handleNormalScreenSqlResult}
              >
                {renderResult()}
              </FullScreen>
            </div>
          </div>
        </div>
      </SplitPane>
    </>
  );
};

export default SqlDetail;
