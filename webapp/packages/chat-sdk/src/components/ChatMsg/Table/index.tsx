import {
  formatByDataFormatType,
  formatByDecimalPlaces,
  formatByThousandSeperator,
} from '../../../utils/utils';
import { Table as AntTable, Tooltip } from 'antd';
import { MsgDataType } from '../../../common/type';
import { CLS_PREFIX } from '../../../common/constants';
import ApplyAuth from '../ApplyAuth';
import { SizeType } from 'antd/es/config-provider/SizeContext';
import dayjs from 'dayjs';
import { useRef, useEffect, useState } from 'react';

type Props = {
  data: MsgDataType;
  size?: SizeType;
  question?: string;
  loading?: boolean;
  onApplyAuth?: (model: string) => void;
};

const Table: React.FC<Props> = ({ data, size, loading, question, onApplyAuth }) => {
  const { entityInfo, queryColumns, queryResults } = data;

  const prefixCls = `${CLS_PREFIX}-table`;
  const wrapperRef = useRef<HTMLDivElement>(null);
  const [wrapperWidth, setWrapperWidth] = useState<number>(0);

  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => {
      setWrapperWidth(Math.floor(entry.contentRect.width));
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);
  const columnWidth = 150;
  const shouldKeepTwoDecimals = (title: string, bizName: string, dataFormatType?: string) => {
    if (dataFormatType === 'decimal' || dataFormatType === 'percent') {
      return true;
    }
    const fieldText = `${title} ${bizName}`.toLowerCase();
    return /金额|利息|本金|本息|余额|占比|比例|费率|利率|ratio|rate|percent|pct|amt|amount|fee|interest|principal|balance/.test(
      fieldText
    );
  };
  const renderCellText = (text: string | number | null | undefined) => {
    const displayText = text === null || text === undefined || text === '' ? '-' : String(text);
    return (
      <Tooltip title={displayText} placement="topLeft">
        <div className={`${prefixCls}-cell-text`}>{displayText}</div>
      </Tooltip>
    );
  };
  const tableColumns: any[] = queryColumns.map(
    ({ name, bizName, showType, dataFormatType, dataFormat, authorized }) => {
      const title = name || bizName;
      return {
        dataIndex: bizName,
        key: bizName,
        title,
        width: columnWidth,
        ellipsis: { showTitle: false },
        sorter:
          showType === 'NUMBER'
            ? (a, b) => {
                return a[bizName] - b[bizName];
              }
            : undefined,
        render: (value: string | number) => {
          if (!authorized) {
            return (
              <ApplyAuth model={entityInfo?.dataSetInfo.name || ''} onApplyAuth={onApplyAuth} />
            );
          }
          if (dataFormatType === 'percent') {
            const formattedValue = formatByDataFormatType(value ?? 0, dataFormatType, dataFormat);
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {renderCellText(formattedValue)}
              </div>
            );
          }
          if (showType === 'NUMBER') {
            let numStr: string | number = value;
            if (value !== null && value !== undefined && value !== '' && !isNaN(Number(value))) {
              if (dataFormatType === 'decimal') {
                numStr = formatByDataFormatType(value, dataFormatType, dataFormat);
              } else if (typeof dataFormat?.decimalPlaces === 'number') {
                numStr = formatByDecimalPlaces(value, dataFormat.decimalPlaces, true);
              } else if (shouldKeepTwoDecimals(title, bizName, dataFormatType)) {
                numStr = formatByDecimalPlaces(value, 2, true);
              } else if (String(value).includes('.') || Number(value) % 1 !== 0) {
                numStr = formatByDecimalPlaces(value, 2, true);
              }
            }
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {renderCellText(formatByThousandSeperator(numStr))}
              </div>
            );
          }
          if (bizName.includes('photo')) {
            return (
              <div className={`${prefixCls}-photo`}>
                <img width={40} height={40} src={value as string} alt="" />
              </div>
            );
          }
          return renderCellText(value);
        },
      };
    }
  );

  const getRowClassName = (_: any, index: number) => {
    return index % 2 !== 0 ? `${prefixCls}-even-row` : '';
  };

  const dateColumn = queryColumns.find(column => column.type === 'DATE' || column.showType === 'DATE');
  const dataSource = dateColumn
    ? queryResults.sort((a, b) => dayjs(a[dateColumn.bizName]).diff(dayjs(b[dateColumn.bizName])))
    : queryResults;
  const tableScrollX = tableColumns.length * columnWidth;

  // Use measured pixel width as explicit inline style so that Ant Table's
  // scroll.x containment works regardless of the parent flex/percentage chain.
  const containerStyle = wrapperWidth > 0 ? { width: wrapperWidth } : undefined;
  const needsScroll = wrapperWidth > 0 && tableScrollX > wrapperWidth;

  return (
    <div ref={wrapperRef} className={prefixCls} style={containerStyle}>
      <AntTable
        pagination={
          queryResults.length <= 10 ? false : { defaultPageSize: 10, position: ['bottomCenter'] }
        }
        scroll={needsScroll ? { x: tableScrollX } : undefined}
        columns={tableColumns}
        dataSource={dataSource}
        rowClassName={getRowClassName}
        size={size}
        loading={loading}
      />
    </div>
  );
};

export default Table;
