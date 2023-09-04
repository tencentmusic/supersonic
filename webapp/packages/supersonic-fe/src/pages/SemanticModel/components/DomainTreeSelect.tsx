import { message, TreeSelect } from 'antd';
import type { DataNode } from 'antd/lib/tree';
import { useEffect, useState } from 'react';
import type { FC } from 'react';
import { connect } from 'umi';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';
import { getDomainList } from '../../SemanticModel/service';
import { constructorClassTreeFromList, addPathInTreeData } from '../utils';
import styles from './style.less';
import { ISemantic } from '../data';

type Props = {
  value?: any;
  width?: number | string;
  onChange?: () => void;
  treeSelectProps?: Record<string, any>;
  domainList: ISemantic.IDomainItem[];
  dispatch: Dispatch;
};

const DomainTreeSelect: FC<Props> = ({
  value,
  width = 300,
  onChange,
  treeSelectProps = {},
  domainList,
  dispatch,
}) => {
  const [domainTree, setDomainTree] = useState<DataNode[]>([]);

  const initProjectTree = async () => {
    const { code, data, msg } = await getDomainList();
    if (code === 200) {
      dispatch({
        type: 'domainManger/setDomainList',
        payload: { domainList: data },
      });
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    if (domainList.length === 0) {
      initProjectTree();
    }
  }, []);

  useEffect(() => {
    const treeData = addPathInTreeData(constructorClassTreeFromList(domainList));
    setDomainTree(treeData);
  }, [domainList]);

  return (
    <div
      className={styles.domainTreeSelect}
      style={{
        width,
      }}
    >
      <TreeSelect
        showSearch
        style={{ width: '100%' }}
        value={value}
        dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
        placeholder={'请选择主题域'}
        allowClear
        multiple
        treeNodeFilterProp="title"
        treeDefaultExpandAll
        onChange={onChange}
        treeData={domainTree}
        {...treeSelectProps}
      />
    </div>
  );
};

export default connect(
  ({
    domainManger: { selectDomainId, selectDomainName, domainList },
  }: {
    domainManger: StateType;
  }) => ({
    selectDomainId,
    selectDomainName,
    domainList,
  }),
)(DomainTreeSelect);
