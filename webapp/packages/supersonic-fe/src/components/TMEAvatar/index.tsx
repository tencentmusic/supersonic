import type { FC } from 'react';
import { Avatar } from 'antd';
import type { AvatarProps } from 'antd';
import avatarIcon from './assets/avatar.gif';

interface Props extends AvatarProps {
  staffName?: string;
  avatarImg?: string;
}
const { tmeAvatarUrl } = process.env;
const TMEAvatar: FC<Props> = ({ staffName, avatarImg, ...restProps }) => {
  const avatarSrc = tmeAvatarUrl ? `${tmeAvatarUrl}${staffName}.png` : avatarImg;
  return (
    <Avatar src={`${avatarSrc}`} alt="avatar" icon={<img src={avatarIcon} />} {...restProps} />
  );
};
export default TMEAvatar;
