import { message } from 'antd';
import React, { useState, useEffect, useRef } from 'react';
import { useParams, useModel, Helmet } from '@umijs/max';
import { BASE_TITLE } from '@/common/constants';
import { ISemantic } from '../data';
import { getDimensionList } from '../service';
import DetailContainer from '@/pages/SemanticModel/components/DetailContainer';
import DetailSider from '@/pages/SemanticModel/components/DetailContainer/DetailSider';
import { ProjectOutlined, ConsoleSqlOutlined } from '@ant-design/icons';
import DimensionInfoForm from '../components/DimensionInfoForm';
import DetailFormWrapper from '@/pages/SemanticModel/components/DetailContainer/DetailFormWrapper';

type Props = Record<string, any>;

const DataSetDetail: React.FC<Props> = () => {
  const settingList = [
    {
      icon: <ProjectOutlined />,
      key: 'basic',
      text: '基本信息',
    },
  ];
  const params: any = useParams();
  const detailId = params.dimensionId;
  const modelId = params.modelId;
  const domainId = params.domainId;
  const menuKey = params.menuKey;
  const [detailData, setDetailData] = useState<ISemantic.IDimensionItem>();
  const dimensionModel = useModel('SemanticModel.dimensionData');
  const { setSelectDimension } = dimensionModel;
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
      setSelectDimension(undefined);
    };
  }, []);

  useEffect(() => {
    if (!detailId) {
      return;
    }
    queryDetailData(detailId);
  }, [detailId]);

  const queryDetailData = async (id: number) => {
    const { code, data, msg } = await getDimensionList({ ids: [id] });
    if (code === 200) {
      const target = data?.list?.[0];
      setDetailData(target);
      setSelectDimension(target);
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
            <DimensionInfoForm
              ref={detailFormRef}
              modelId={modelId}
              domainId={domainId}
              dimensionItem={detailData}
            />
          </DetailFormWrapper>
        }
      />
    </>
  );
};

export default DataSetDetail;
