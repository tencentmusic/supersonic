import { message } from 'antd';
import React, { useEffect } from 'react';
import { useParams, useModel, Outlet } from '@umijs/max';
import { ISemantic } from './data';
import { getDomainList, getModelDetail } from './service';
import PageBreadcrumb from './PageBreadcrumb';

type Props = {};

const SemanticModel: React.FC<Props> = ({}) => {
  const params: any = useParams();
  const domainId = params.domainId;
  const modelId = params.modelId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const databaseModel = useModel('SemanticModel.databaseData');
  const { setSelectDomain, setDomainList } = domainModel;
  const { selectModel, setSelectModel } = modelModel;
  const { MrefreshDatabaseList } = databaseModel;

  const initSelectedDomain = (domainList: ISemantic.IDomainItem[]) => {
    const targetNode = domainList.filter((item: any) => {
      return `${item.id}` === domainId;
    })[0];
    if (!targetNode) {
      const firstRootNode = domainList.filter((item: any) => {
        return item.parentId === 0;
      })[0];
      if (firstRootNode) {
        setSelectDomain(firstRootNode);
      }
    } else {
      setSelectDomain(targetNode);
    }
  };

  const initProjectTree = async () => {
    const { code, data, msg } = await getDomainList();
    if (code === 200) {
      initSelectedDomain(data);
      setDomainList(data);
    } else {
      message.error(msg);
    }
  };

  const initModelData = async () => {
    const { code, data, msg } = await getModelDetail({ modelId });
    if (code === 200) {
      setSelectModel(data);
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    initProjectTree();
    MrefreshDatabaseList();
    if (modelId && modelId !== selectModel) {
      initModelData();
    }

    return () => {
      setSelectDomain(undefined);
    };
  }, []);

  return (
    <div>
      <div style={{ background: '#fff' }}>
        <PageBreadcrumb />
      </div>
      <div>
        <Outlet />
      </div>
    </div>
  );
};

export default SemanticModel;
