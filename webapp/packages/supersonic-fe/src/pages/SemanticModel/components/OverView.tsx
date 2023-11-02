import { CheckCard } from '@ant-design/pro-components';
import React, { useState } from 'react';
import { Button, Dropdown, message, Popconfirm } from 'antd';
import { PlusOutlined, EllipsisOutlined } from '@ant-design/icons';
import { ISemantic } from '../data';
import { connect } from 'umi';
import icon from '../../../assets/icon/cloudEditor.svg';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';
import { formatNumber } from '../../../utils/utils';
import { deleteModel } from '../service';
import ModelCreateFormModal from './ModelCreateFormModal';
import ModelTable from './ModelTable';
import styles from './style.less';

type Props = {
  disabledEdit?: boolean;
  modelList: ISemantic.IModelItem[];
  onModelChange?: (model?: ISemantic.IModelItem) => void;
  domainManger: StateType;
  dispatch: Dispatch;
};

const OverView: React.FC<Props> = ({
  modelList,
  disabledEdit = false,
  onModelChange,
  domainManger,
}) => {
  // const { selectDomainId, selectModelId } = domainManger;
  // const [currentModel, setCurrentModel] = useState<any>({});
  // const [modelCreateFormModalVisible, setModelCreateFormModalVisible] = useState<boolean>(false);

  // const descNode = (model: ISemantic.IDomainItem) => {
  //   const { metricCnt, dimensionCnt } = model;
  //   return (
  //     <div className={styles.overviewExtraContainer}>
  //       <div className={styles.extraWrapper}>
  //         <div className={styles.extraStatistic}>
  //           <div className={styles.extraTitle}>维度数</div>
  //           <div className={styles.extraValue}>
  //             <span className="ant-statistic-content-value">{formatNumber(dimensionCnt || 0)}</span>
  //           </div>
  //         </div>
  //       </div>
  //       <div className={styles.extraWrapper}>
  //         <div className={styles.extraStatistic}>
  //           <div className={styles.extraTitle}>指标数</div>
  //           <div className={styles.extraValue}>
  //             <span className="ant-statistic-content-value">{formatNumber(metricCnt || 0)}</span>
  //           </div>
  //         </div>
  //       </div>
  //     </div>
  //   );
  // };

  // const extraNode = (model: ISemantic.IDomainItem) => {
  //   return (
  //     <Dropdown
  //       placement="top"
  //       menu={{
  //         onClick: ({ key, domEvent }) => {
  //           domEvent.stopPropagation();
  //           if (key === 'edit') {
  //             setCurrentModel(model);
  //             setModelCreateFormModalVisible(true);
  //           }
  //         },
  //         items: [
  //           {
  //             label: '编辑',
  //             key: 'edit',
  //           },
  //           {
  //             label: (
  //               <Popconfirm
  //                 title="确认删除？"
  //                 okText="是"
  //                 cancelText="否"
  //                 onConfirm={async () => {
  //                   const { code, msg } = await deleteModel(model.id);
  //                   if (code === 200) {
  //                     onModelChange?.();
  //                   } else {
  //                     message.error(msg);
  //                   }
  //                 }}
  //               >
  //                 <a key="modelDeleteBtn">删除</a>
  //               </Popconfirm>
  //             ),
  //             key: 'delete',
  //           },
  //         ],
  //       }}
  //     >
  //       <EllipsisOutlined
  //         style={{ fontSize: 22, color: 'rgba(0,0,0,0.5)' }}
  //         onClick={(e) => e.stopPropagation()}
  //       />
  //     </Dropdown>
  //   );
  // };

  return (
    <div style={{ padding: '0px 20px 20px' }}>
      <ModelTable modelList={modelList} disabledEdit={disabledEdit} onModelChange={onModelChange} />
      {/* {!disabledEdit && (
        <div style={{ paddingBottom: '20px' }}>
          <Button
            onClick={() => {
              setModelCreateFormModalVisible(true);
            }}
            type="primary"
          >
            <PlusOutlined />
            新增模型
          </Button>
        </div>
      )}

      <CheckCard.Group value={selectModelId} defaultValue={selectModelId}>
        {modelList &&
          modelList.map((model: ISemantic.IDomainItem) => {
            return (
              <CheckCard
                avatar={icon}
                title={`${model.name}`}
                key={model.id}
                value={model.id}
                description={descNode(model)}
                extra={!disabledEdit && extraNode(model)}
                onClick={() => {
                  onModelChange?.(model);
                }}
              />
            );
          })}
      </CheckCard.Group> */}
      {/* {modelCreateFormModalVisible && (
        <ModelCreateFormModal
          domainId={selectDomainId}
          basicInfo={currentModel}
          onSubmit={() => {
            setModelCreateFormModalVisible(false);
            onModelChange?.();
          }}
          onCancel={() => {
            setModelCreateFormModalVisible(false);
          }}
        />
      )} */}
    </div>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(OverView);
