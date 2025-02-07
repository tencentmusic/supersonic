import { formatByDecimalPlaces, formatByThousandSeperator } from '../../../utils/utils';
import { Table as AntTable } from 'antd';
import { MsgDataType } from '../../../common/type';
import { CLS_PREFIX } from '../../../common/constants';
import ApplyAuth from '../ApplyAuth';
import { SizeType } from 'antd/es/config-provider/SizeContext';
import moment from 'moment';

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
  const tableColumns: any[] = queryColumns.map(
    ({ name, bizName, showType, dataFormatType, dataFormat, authorized }) => {
      return {
        dataIndex: bizName,
        key: bizName,
        title: name || bizName,
        defaultSortOrder: 'descend',
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
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {`${
                  value
                    ? formatByDecimalPlaces(
                        dataFormat?.needMultiply100 ? +value * 100 : value,
                        dataFormat?.decimalPlaces || 2
                      )
                    : 0
                }%`}
              </div>
            );
          }
          if (showType === 'NUMBER') {
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {/* {getFormattedValue(value as number)} */}
                {formatByThousandSeperator(value)}
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
          return value;
        },
      };
    }
  );

  const getRowClassName = (_: any, index: number) => {
    return index % 2 !== 0 ? `${prefixCls}-even-row` : '';
  };

  const dateColumn = queryColumns.find(column => column.type === 'DATE');
  const dataSource = dateColumn
    ? queryResults.sort((a, b) => moment(a[dateColumn.bizName]).diff(moment(b[dateColumn.bizName])))
    : queryResults;
  return (
    <div className={prefixCls}>
      {question && (
        <div className={`${prefixCls}-top-bar`}>
          <div className={`${prefixCls}-indicator-name`}>{question}</div>
        </div>
      )}

      <AntTable
        pagination={
          queryResults.length <= 10 ? false : { defaultPageSize: 10, position: ['bottomCenter'] }
        }
        columns={tableColumns}
        dataSource={dataSource}
        style={{ width: '100%', overflowX: 'auto', overflowY: 'hidden' }}
        rowClassName={getRowClassName}
        size={size}
        loading={loading}
      />
    </div>
  );
};

export default Table;
