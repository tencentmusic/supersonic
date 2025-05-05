import { PREFIX_CLS } from '../../../common/constants';
import { MsgDataType } from '../../../common/type';
import { useRef, useState } from 'react';
import NoPermissionChart from '../NoPermissionChart';
import { ColumnType } from '../../../common/type';
import { Spin, Select } from 'antd';
import PieChart from './PieChart';
import Bar from '../Bar';

type Props = {
  data: MsgDataType;
  question: string;
  triggerResize?: boolean;
  loading: boolean;
  metricField: ColumnType;
  categoryField: ColumnType;
  onApplyAuth?: (model: string) => void;
};

const metricChartSelectOptions = [
  {
    value: 'pie',
    label: '饼图',
  },
  {
    value: 'bar',
    label: '柱状图',
  },
];

const Pie: React.FC<Props> = ({
  data,
  question,
  triggerResize,
  loading,
  metricField,
  categoryField,
  onApplyAuth,
}) => {
  const [chartType, setChartType] = useState('pie');
  const { entityInfo } = data;

  if (metricField && !metricField?.authorized) {
    return (
      <NoPermissionChart
        model={entityInfo?.dataSetInfo?.name || ''}
        chartType="pieChart"
        onApplyAuth={onApplyAuth}
      />
    );
  }

  const prefixCls = `${PREFIX_CLS}-pie`;

  return (
    <div className={prefixCls}>
      <div className={`${prefixCls}-metric-fields ${prefixCls}-metric-field-single`}>
        {question}
      </div>
      <div className={`${prefixCls}-select-options`}>
        <Select
          defaultValue="pie"
          bordered={false}
          options={metricChartSelectOptions}
          onChange={(value: string) => setChartType(value)}
        />
      </div>
      {chartType === 'pie' ? (
        <PieChart
          data={data}
          metricField={metricField}
          categoryField={categoryField}
          triggerResize={triggerResize}
        />
      ) : (
        <Bar
          data={data}
          triggerResize={triggerResize}
          loading={loading}
          metricField={metricField}
          onApplyAuth={onApplyAuth}
        />
      )}
    </div>
  );
};

export default Pie;
