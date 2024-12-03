import { message } from 'antd';
import { ISemantic } from '../data';
import { useState } from 'react';
import { getDimensionList } from '../service';

export default function Dimension() {
  const [dimensionList, setDimensionList] = useState<ISemantic.IDimensionItem[]>([]);
  const [selectDimension, setSelectDimension] = useState<ISemantic.IDimensionItem>();

  const queryDimensionList = async (params: any) => {
    const { code, data, msg } = await getDimensionList({
      ...params,
    });
    const { list } = data || {};
    if (code === 200) {
      setDimensionList(list);
    } else {
      message.error(msg);
      setDimensionList([]);
    }
  };

  const refreshDimensionList = async (params: any) => {
    return await queryDimensionList(params);
  };

  return {
    MdimensionList: dimensionList,
    MrefreshDimensionList: refreshDimensionList,
    selectDimension,
    setSelectDimension,
  };
}
