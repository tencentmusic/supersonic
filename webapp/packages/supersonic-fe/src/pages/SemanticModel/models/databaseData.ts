import { ISemantic } from '../data';
import { useState } from 'react';
import { getDatabaseList } from '../service';

export default function Database() {
  const [databaseConfigList, setDatabaseConfigList] = useState<ISemantic.IDatabaseItemList>([]);
  const queryDatabaseList = async () => {
    const { code, data } = await getDatabaseList();
    if (code === 200) {
      setDatabaseConfigList(data);
    } else {
      setDatabaseConfigList([]);
    }
  };

  const refreshDatabaseList = async () => {
    return await queryDatabaseList();
  };

  return {
    databaseConfigList,
    setDatabaseConfigList,
    MrefreshDatabaseList: refreshDatabaseList,
  };
}
