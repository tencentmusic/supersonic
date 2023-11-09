import { formatByDecimalPlaces, getFormattedValue, isMobile } from '../../../utils/utils';
import { Table as AntTable } from 'antd';
import { MsgDataType } from '../../../common/type';
import { CLS_PREFIX } from '../../../common/constants';
import ApplyAuth from '../ApplyAuth';
import { SizeType } from 'antd/es/config-provider/SizeContext';
import moment from 'moment';

type Props = {
  data: MsgDataType;
  size?: SizeType;
  onApplyAuth?: (model: string) => void;
};

const Table: React.FC<Props> = ({ data, size, onApplyAuth }) => {
  const { entityInfo, queryColumns, queryResults } = data;

  const prefixCls = `${CLS_PREFIX}-table`;

  const tableColumns: any[] = queryColumns.map(
    ({ name, nameEn, showType, dataFormatType, dataFormat, authorized }) => {
      return {
        dataIndex: nameEn,
        key: nameEn,
        title: name || nameEn,
        render: (value: string | number) => {
          if (!authorized) {
            return <ApplyAuth model={entityInfo?.modelInfo.name || ''} onApplyAuth={onApplyAuth} />;
          }
          if (dataFormatType === 'percent') {
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {`${formatByDecimalPlaces(
                  dataFormat?.needMultiply100 ? +value * 100 : value,
                  dataFormat?.decimalPlaces || 2
                )}%`}
              </div>
            );
          }
          if (showType === 'NUMBER') {
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {getFormattedValue(value as number)}
              </div>
            );
          }
          if (nameEn.includes('photo')) {
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
    ? queryResults.sort((a, b) => moment(a[dateColumn.nameEn]).diff(moment(b[dateColumn.nameEn])))
    : queryResults;

  return (
    <div className={prefixCls}>
      <AntTable
        pagination={
          queryResults.length <= 10
            ? false
            : {
                defaultPageSize: 10,
                position: ['bottomCenter'],
                size: isMobile ? 'small' : 'default',
              }
        }
        columns={tableColumns}
        dataSource={dataSource}
        style={{ width: '100%', overflowX: 'auto', overflowY: 'hidden' }}
        rowClassName={getRowClassName}
        size={size}
      />
    </div>
  );
};

export default Table;
