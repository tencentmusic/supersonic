import { formatByDecimalPlaces, getFormattedValue } from '../../../utils/utils';
import { Table as AntTable } from 'antd';
import { MsgDataType } from '../../../common/type';
import { CLS_PREFIX } from '../../../common/constants';
import ApplyAuth from '../ApplyAuth';

type Props = {
  data: MsgDataType;
  onApplyAuth?: (domain: string) => void;
};

const Table: React.FC<Props> = ({ data, onApplyAuth }) => {
  const { entityInfo, queryColumns, queryResults } = data;

  const prefixCls = `${CLS_PREFIX}-table`;

  const tableColumns: any[] = queryColumns.map(
    ({ name, nameEn, showType, dataFormatType, dataFormat, authorized }) => {
      return {
        dataIndex: nameEn,
        key: nameEn,
        title: name,
        render: (value: string | number) => {
          if (!authorized) {
            return (
              <ApplyAuth domain={entityInfo?.domainInfo.name || ''} onApplyAuth={onApplyAuth} />
            );
          }
          if (dataFormatType === 'percent') {
            return (
              <div className={`${prefixCls}-formatted-value`}>
                {`${formatByDecimalPlaces(
                  dataFormat?.needmultiply100 ? +value * 100 : value,
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

  return (
    <div className={prefixCls}>
      <AntTable
        pagination={queryResults.length <= 10 ? false : undefined}
        size={queryResults.length === 1 ? 'middle' : 'small'}
        columns={tableColumns}
        dataSource={queryResults}
        style={{ width: '100%' }}
      />
    </div>
  );
};

export default Table;
