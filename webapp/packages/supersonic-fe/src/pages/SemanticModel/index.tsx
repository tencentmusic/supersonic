import { message } from 'antd';
import React, { useEffect, useState } from 'react';
import { history, useParams, useModel, Outlet } from '@umijs/max';
import DomainListTree from './components/DomainList';
import styles from './components/style.less';
import { LeftOutlined, RightOutlined } from '@ant-design/icons';
import { ISemantic } from './data';
import { getDomainList, getDataSetList, getModelDetail } from './service';
import PageBreadcrumb from './PageBreadcrumb';

type Props = {};

const SemanticModel: React.FC<Props> = ({}) => {
  const params: any = useParams();
  const domainId = params.domainId;
  const modelId = params.modelId;
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const databaseModel = useModel('SemanticModel.databaseData');
  const metricModel = useModel('SemanticModel.metricData');
  const { setSelectDomain, setDomainList, selectDomainId } = domainModel;
  const { selectModel, setSelectModel, setModelTableHistoryParams, MrefreshModelList } = modelModel;
  const { MrefreshDatabaseList } = databaseModel;

  const { selectMetric, setSelectMetric } = metricModel;

  // useEffect(() => {

  //   return () => {
  //     setSelectMetric(undefined);
  //   }
  // }, [])

  const initSelectedDomain = (domainList: ISemantic.IDomainItem[]) => {
    const targetNode = domainList.filter((item: any) => {
      return `${item.id}` === domainId;
    })[0];
    if (!targetNode) {
      const firstRootNode = domainList.filter((item: any) => {
        return item.parentId === 0;
      })[0];
      if (firstRootNode) {
        const { id } = firstRootNode;
        setSelectDomain(firstRootNode);
        // pushUrlMenu(id, menuKey);
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
        {/* <OverviewContainer /> */}
      </div>
    </div>
  );
};

export default SemanticModel;
