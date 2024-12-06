import { useState, useEffect } from 'react';
import type { FC } from 'react';
import { Select } from 'antd';
import type { UserItem } from './service';
import { useModel } from '@umijs/max';
import styles from './index.less';
import TMEAvatar from '../TMEAvatar';

interface Props {
  value?: string[];
  placeholder?: string;
  isMultiple?: boolean;
  onChange?: (owners: string | string[]) => void;
}

const SelectTMEPerson: FC<Props> = ({ placeholder, value, isMultiple = true, onChange }) => {
  const [userList, setUserList] = useState<UserItem[]>([]);
  const allUserModel = useModel('allUserData');
  const { allUserList, MrefreshUserList } = allUserModel;

  const queryTmePersonData = async () => {
    const list = await MrefreshUserList();
    setUserList(list);
  };
  useEffect(() => {
    if (Array.isArray(allUserList) && allUserList.length > 0) {
      setUserList(allUserList);
    } else {
      queryTmePersonData();
    }
  }, []);

  return (
    <Select
      value={value}
      placeholder={placeholder ?? '请选择用户名'}
      mode={isMultiple ? 'multiple' : undefined}
      allowClear
      showSearch
      className={styles.selectPerson}
      onChange={onChange}
    >
      {userList.map((item) => {
        return (
          <Select.Option key={item.name} value={item.name}>
            <TMEAvatar size="small" staffName={item.name} />
            <span className={styles.userText}>{item.displayName}</span>
          </Select.Option>
        );
      })}
    </Select>
  );
};

export default SelectTMEPerson;
