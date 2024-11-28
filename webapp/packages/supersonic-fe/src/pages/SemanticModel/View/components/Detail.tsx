import { message, Form } from 'antd';
import React, { useState, useEffect } from 'react';
import { getMetricData } from '../../service';
import { useParams, useModel, Helmet } from '@umijs/max';
import { BASE_TITLE } from '@/common/constants';
import { ISemantic } from '../../data';
import { createView, updateView, getAllModelByDomainId, getDataSetDetail } from '../../service';
import DetailContainer from '@/pages/SemanticModel/components/DetailContainer';
import DetailSider from '@/pages/SemanticModel/components/DetailContainer/DetailSider';
import { ProjectOutlined, ConsoleSqlOutlined } from '@ant-design/icons';
import DatasetCreateForm from './DatasetCreateForm';
import DetailFormWrapper from '@/pages/SemanticModel/components/DetailContainer/DetailFormWrapper';
// import { MetricSettingKey, MetricSettingWording } from './constants';

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
  const [detailData, setDetailData] = useState<ISemantic.IMetricItem>();
  const domainModel = useModel('SemanticModel.domainData');
  const modelModel = useModel('SemanticModel.modelData');
  const { modelList } = modelModel;
  const { selectDomainId } = domainModel;
  const [settingKey, setSettingKey] = useState<string>();
  // const [modelList, setModelList] = useState<ISemantic.IModelItem[]>([]);
  const [activeMenu, setActiveMenu] = useState<any>(settingList[0]);

  const [form] = Form.useForm();

  useEffect(() => {
    if (!detailId) {
      return;
    }
    queryDetailData(detailId);
  }, [detailId]);

  // useEffect(() => {
  //   if (!selectDomainId) {
  //     return;
  //   }
  //   queryDomainAllModel();
  // }, [selectDomainId]);

  // const queryDomainAllModel = async () => {
  //   const { code, data, msg } = await getAllModelByDomainId(selectDomainId);
  //   if (code === 200) {
  //     setModelList(data);
  //   } else {
  //     message.error(msg);
  //   }
  // };

  const queryDetailData = async (id: number) => {
    const { code, data, msg } = await getDataSetDetail(id);
    if (code === 200) {
      setDetailData({ ...data });
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
            menuKey={'basic'}
            menuList={settingList}
            detailData={detailData}
            onMenuKeyChange={(key: string, menu) => {
              setSettingKey(key);
              setActiveMenu(menu);
            }}
          />
        }
        containerNode={
          <DetailFormWrapper
            currentMenu={activeMenu}
            onSave={() => {
              console.log(form.getFieldsValue());
            }}
          >
            <DatasetCreateForm
              form={form}
              activeKey={activeMenu.key}
              domainId={selectDomainId}
              viewItem={detailData}
              modelList={modelList}
              onSubmit={() => {
                // queryDataSetList();
                // setCreateDataSourceModalOpen(false);
              }}
              onCancel={() => {
                // setCreateDataSourceModalOpen(false);
              }}
            />
          </DetailFormWrapper>
        }
      />
    </>
  );
};

export default DataSetDetail;
