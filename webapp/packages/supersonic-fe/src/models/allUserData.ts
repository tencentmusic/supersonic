import { useState } from 'react';
import { getAllUser } from '../components/SelectTMEPerson/service';
import type { UserItem } from '../components/SelectTMEPerson/service';

export default function Model() {
  const [allUserList, setAllUserList] = useState<UserItem[]>([]);

  const queryUserData = async () => {
    const { code, data } = await getAllUser();
    if (code === 200 || Number(code) === 0) {
      setAllUserList(data);
      return data;
    }
    return [];
  };

  const MrefreshUserList = async () => {
    return await queryUserData();
  };

  return {
    allUserList,
    MrefreshUserList,
  };
}
