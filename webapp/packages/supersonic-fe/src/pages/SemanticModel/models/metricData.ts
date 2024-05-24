import { message } from 'antd';
import { ISemantic } from '../data';
import { useState } from 'react';
import { queryMetric, excuteSql, getDatabaseList } from '../service';

export default function Metric() {
  const [metricList, setMetricList] = useState<ISemantic.IMetricItem[]>([]);

  const queryMetricList = async (params: any) => {
    const { code, data, msg } = await queryMetric({
      ...params,
    });
    const { list } = data || {};
    if (code === 200) {
      setMetricList(list);
    } else {
      message.error(msg);
      setMetricList([]);
    }
  };

  const refreshMetricList = async (params: any) => {
    return await queryMetricList(params);
  };

  return {
    MmetricList: metricList,
    MrefreshMetricList: refreshMetricList,
    MqueryMetricList: queryMetricList,
  };
}
