import { useState } from 'react';
import { Modal, Radio } from 'antd';
import DimensionValueFilterTable from './DimensionValueFilterTable';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { KnowledgeConfigTypeEnum, DimensionValueListType } from '../../enum';
import type { ISemantic } from '../../data';
import CommonEditList from '../../components/CommonEditList';

type Props = {
  config: ISemantic.IDictKnowledgeConfigItem;
  type?: KnowledgeConfigTypeEnum;
  onSubmit?: () => void;
  onCancel?: () => void;
  onMenuClick?: (key: string, selectedKes: React.Key[]) => void;
};

const DimensionValueFilterModal: React.FC<Props> = ({
  config: knowledgeConfig,
  onCancel,
  onMenuClick,
}) => {
  const [listType, setListType] = useState(DimensionValueListType.BLACK_LIST);
  const [filterRulesList, setFilterRulesList] = useState<string[]>([]);

  return (
    <>
      <Modal
        width={800}
        destroyOnClose
        title={`维度值过滤`}
        style={{ top: 48 }}
        maskClosable={false}
        open={true}
        cancelText="关闭"
        okButtonProps={{ hidden: true }}
        onCancel={onCancel}
      >
        <div>
          <Radio.Group
            buttonStyle="solid"
            value={listType}
            onChange={(e) => {
              setListType(e.target.value);
            }}
          >
            <Radio.Button value={DimensionValueListType.BLACK_LIST}>黑名单</Radio.Button>
            <Radio.Button value={DimensionValueListType.WHITE_LIST}>白名单</Radio.Button>
            <Radio.Button value={DimensionValueListType.RULE_LIST}>自定义过滤规则</Radio.Button>
          </Radio.Group>

          <div>
            {[DimensionValueListType.BLACK_LIST, DimensionValueListType.WHITE_LIST].includes(
              listType,
            ) && (
              <div key={listType}>
                <DimensionValueFilterTable
                  dataSource={knowledgeConfig?.config?.[listType]}
                  listType={listType}
                  onMenuClick={onMenuClick}
                />
              </div>
            )}
            {DimensionValueListType.RULE_LIST === listType && (
              <div style={{ marginTop: 20 }}>
                <CommonEditList
                  title={
                    <FormItemTitle
                      title={`过滤规则`}
                      subTitle={`将尚不存在的维度值以自定义的方式`}
                    />
                  }
                  createBtnType="primary"
                  onChange={(list) => {
                    setFilterRulesList(list);
                    onMenuClick?.('batchAddRuleList', list);
                  }}
                  value={filterRulesList}
                />
              </div>
            )}
          </div>
        </div>
      </Modal>
    </>
  );
};

export default DimensionValueFilterModal;
