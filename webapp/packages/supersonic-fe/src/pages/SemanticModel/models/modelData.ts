import { ISemantic } from '../data';
import { message } from 'antd';
import { useState } from 'react';
import { getModelList } from '../service';

export default function Model() {
  const [selectModel, setSelectModel] = useState<ISemantic.IModelItem>();
  const [modelList, setModelList] = useState<ISemantic.IModelItem[]>([]);
  const [modelTableHistoryParams, setModelTableHistoryParams] = useState<Record<string, any>>({});

  const mergeParams = (params: Record<string, any>) => {
    setModelTableHistoryParams({
      ...modelTableHistoryParams,
      ...params,
    });
  };

  const queryModelList = async (domainId: number) => {
    const { code, data } = await getModelList(domainId);
    if (code === 200) {
      setModelList(data);
      return data;
    } else {
      message.error('获取模型列表失败!');
    }
    return [];
  };

  const MrefreshModelList = async (domainId: number) => {
    return await queryModelList(domainId);
  };

  return {
    selectModel,
    selectModelId: selectModel?.id,
    selectModelName: selectModel?.name,
    modelList,
    queryModelList,
    MrefreshModelList,
    setSelectModel,
    setModelTableHistoryParams: mergeParams,
    modelTableHistoryParams,
  };
}
