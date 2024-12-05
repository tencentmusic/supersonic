import { message, Form } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import { useParams, useModel, Helmet } from '@umijs/max';
import { BASE_TITLE } from '@/common/constants';
import { ISemantic } from '../../data';
import { getAllModelByDomainId, getDataSetDetail } from '../../service';
import DetailContainer from '@/pages/SemanticModel/components/DetailContainer';
import DetailSider from '@/pages/SemanticModel/components/DetailContainer/DetailSider';
import { ProjectOutlined, ConsoleSqlOutlined } from '@ant-design/icons';
import DatasetCreateForm from './DatasetCreateForm';
import DetailFormWrapper from '@/pages/SemanticModel/components/DetailContainer/DetailFormWrapper';

type Props = Record<string, any>;

const DataSetDetail: React.FC<Props> = () => {
  const settingList = [
    {
      icon: <ProjectOutlined />,
      key: 'basic',
      text: '基本信息',
    },
    {
      icon: <ConsoleSqlOutlined />,
      key: 'relation',
      text: '关联信息',
    },
  ];
  const params: any = useParams();
  const detailId = params.datasetId;
  const menuKey = params.menuKey;
  const [detailData, setDetailData] = useState<ISemantic.IDatasetItem>();
  const domainModel = useModel('SemanticModel.domainData');
  const { selectDomainId, setSelectDataSet } = domainModel;
  const [modelList, setModelList] = useState<ISemantic.IModelItem[]>([]);
  const [activeMenu, setActiveMenu] = useState<any>(() => {
    if (menuKey) {
      const target = settingList.find((item) => item.key === menuKey);
      if (target) {
        return target;
      }
    }

    return settingList[0];
  });
  const detailFormRef = useRef<any>();

  useEffect(() => {
    return () => {
      setSelectDataSet(undefined);
    };
  }, []);

  useEffect(() => {
    if (!detailId) {
      return;
    }
    queryDetailData(detailId);
  }, [detailId]);

  useEffect(() => {
    if (!selectDomainId) {
      return;
    }
    queryDomainAllModel();
  }, [selectDomainId]);

  const queryDomainAllModel = async () => {
    const { code, data, msg } = await getAllModelByDomainId(selectDomainId);
    if (code === 200) {
      setModelList(data);
    } else {
      message.error(msg);
    }
  };

  const queryDetailData = async (id: number) => {
    const { code, data, msg } = await getDataSetDetail(id);
    if (code === 200) {
      setDetailData(data);
      setSelectDataSet(data);
      return;
    }
    message.error(msg);
  };

  return (
    <>
      <Helmet title={`[数据集]${detailData?.name}-${BASE_TITLE}`} />
      <DetailContainer
        siderNode={
          <DetailSider
            menuKey={activeMenu.key}
            menuList={settingList}
            detailData={detailData}
            onMenuKeyChange={(key: string, menu) => {
              // setSettingKey(key);
              setActiveMenu(menu);
            }}
          />
        }
        containerNode={
          <DetailFormWrapper
            currentMenu={activeMenu}
            onSave={() => {
              detailFormRef.current.onSave();
            }}
          >
            <DatasetCreateForm
              ref={detailFormRef}
              activeKey={activeMenu.key}
              domainId={selectDomainId}
              datasetItem={detailData}
              modelList={modelList}
            />
          </DetailFormWrapper>
        }
      />
    </>
  );
};

export default DataSetDetail;
