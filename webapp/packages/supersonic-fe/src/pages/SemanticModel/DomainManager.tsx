import { message } from 'antd';
import React, { useEffect, useState } from 'react';
import { history, useParams, useModel } from '@umijs/max';
import { ISemantic } from './data';
import { getDomainList, getDataSetList } from './service';
import DomainManagerTab from './components/DomainManagerTab';
import { isArrayOfValues } from '@/utils/utils';

type Props = {};

const DomainManager: React.FC<Props> = ({}) => {
  const defaultTabKey = 'overview';
  const params: any = useParams();
  const domainId = params.domainId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const databaseModel = useModel('SemanticModel.databaseData');
  const { selectDomainId, domainList, setSelectDomain, setDomainList } = domainModel;
  const { selectModelId } = modelModel;
  const { MrefreshDatabaseList } = databaseModel;
  const menuKey = params.menuKey ? params.menuKey : defaultTabKey;
  const [collapsedState, setCollapsedState] = useState(true);
  const [activeKey, setActiveKey] = useState<string>(menuKey);
  const [dataSetList, setDataSetList] = useState<ISemantic.IDatasetItem[]>([]);

  // const initSelectedDomain = (domainList: ISemantic.IDomainItem[]) => {
  //   const targetNode = domainList.filter((item: any) => {
  //     return `${item.id}` === domainId;
  //   })[0];
  //   if (!targetNode) {
  //     const firstRootNode = domainList.filter((item: any) => {
  //       return item.parentId === 0;
  //     })[0];
  //     if (firstRootNode) {
  //       const { id } = firstRootNode;
  //       setSelectDomain(firstRootNode);
  //       setActiveKey(menuKey);
  //       pushUrlMenu(id, 0, menuKey);
  //     }
  //   } else {
  //     setSelectDomain(targetNode);
  //   }
  // };

  // const initProjectTree = async () => {
  //   const { code, data, msg } = await getDomainList();
  //   if (code === 200) {
  //     initSelectedDomain(data);
  //     setDomainList(data);
  //   } else {
  //     message.error(msg);
  //   }
  // };

  // useEffect(() => {
  //   initProjectTree();
  //   MrefreshDatabaseList();
  // }, []);

  // useEffect(() => {
  //   if (!selectDomainId) {
  //     return;
  //   }
  //   // queryModelList();
  //   queryDataSetList();
  // }, [selectDomainId]);

  // const queryDataSetList = async () => {
  //   const { code, data, msg } = await getDataSetList(selectDomainId);
  //   if (code === 200) {
  //     setDataSetList(data);
  //     if (!isArrayOfValues(data)) {
  //       setActiveKey(defaultTabKey);
  //     }
  //   } else {
  //     message.error(msg);
  //   }
  // };

  const pushUrlMenu = (domainId: number, menuKey: string) => {
    history.push(`/model/${domainId}/${menuKey}`);
  };

  const cleanModelInfo = (domainId) => {
    setActiveKey(defaultTabKey);
    pushUrlMenu(domainId, defaultTabKey);
    // setSelectModel(undefined);
  };

  // const handleCollapsedBtn = () => {
  //   setCollapsedState(!collapsedState);
  // };

  return (
    <DomainManagerTab
      activeKey={activeKey}
      dataSetList={dataSetList}
      onBackDomainBtnClick={() => {
        cleanModelInfo(selectDomainId);
      }}
      onMenuChange={(menuKey) => {
        setActiveKey(menuKey);
        pushUrlMenu(selectDomainId, menuKey);
      }}
    />
  );
};

export default DomainManager;
