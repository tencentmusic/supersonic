import { useState } from 'react';
import type { FC } from 'react';
import type { SqlParamsItem, OprType } from '../data';

import styles from '../style.less';
import { AppstoreAddOutlined, DeleteTwoTone, EditTwoTone } from '@ant-design/icons';
import SqlParamsDetailModal from './SqlParamsDetailModal';
import { List } from 'antd';

type Props = {
  value?: SqlParamsItem[];
  onChange?: (e: SqlParamsItem[]) => void;
};

const defalutItem: SqlParamsItem = {
  name: '',
  type: 'query',
  defaultValues: [],
  valueType: 'string',
  udf: false,
};

const SqlParams: FC<Props> = ({ value, onChange }) => {
  const [oprType, setOprType] = useState<OprType>('add');
  const [visible, setVisible] = useState<boolean>(false);
  const [initValue, setInitValue] = useState<SqlParamsItem>();
  const paramsChange = (params: SqlParamsItem[]) => {
    if (onChange) {
      onChange(params);
    }
  };
  const handleAdd = () => {
    setOprType('add');
    setVisible(true);
    setInitValue(defalutItem);
  };
  const handleSave = async (values: SqlParamsItem) => {
    const newValue = value ? [...value] : [];
    const { index, ...rest } = values;
    if (index || index === 0) {
      newValue[index] = rest;
    } else {
      newValue.push(rest);
    }
    setVisible(false);
    setInitValue(undefined);
    paramsChange(newValue);
  };

  const handleDelete = (index: number) => {
    const newValue = value ? [...value] : [];
    newValue.splice(index, 1);
    paramsChange(newValue);
  };
  const handleEdit = (index: number) => {
    const paramsItem = value ? value[index] : defalutItem;
    setInitValue({ ...paramsItem, index });
    setOprType('edit');
    setVisible(true);
  };
  return (
    <>
      <div className={styles.sqlParamsBody}>
        <div className={styles.header}>
          <span className={styles.title}>变量</span>
          <AppstoreAddOutlined className={styles.icon} onClick={handleAdd} />
        </div>
        <List
          className={styles.paramsList}
          dataSource={value}
          renderItem={(item, index) => (
            <List.Item
              title={item.name}
              className={styles.paramsItem}
              key={item.name}
              actions={[
                <>
                  <EditTwoTone
                    className={styles.icon}
                    onClick={() => {
                      handleEdit(index);
                    }}
                  />
                  <DeleteTwoTone
                    className={styles.icon}
                    onClick={() => {
                      handleDelete(index);
                    }}
                  />
                </>,
              ]}
            >
              <div className={styles.name}>{item.name}</div>
            </List.Item>
          )}
        />
      </div>
      <SqlParamsDetailModal
        nameList={value?.map((item) => item.name)}
        oprType={oprType}
        modalVisible={visible}
        onSave={handleSave}
        onCancel={() => {
          setVisible(false);
        }}
        initValue={initValue}
      />
    </>
  );
};

export default SqlParams;
