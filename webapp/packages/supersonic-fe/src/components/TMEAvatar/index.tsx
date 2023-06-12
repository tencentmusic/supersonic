import type { FC } from 'react';
import { Avatar } from 'antd';
import type { AvatarProps } from 'antd';
import avatarIcon from './assets/avatar.gif';

interface Props extends AvatarProps {
  staffName?: string;
  avatarImg?: string;
}

const TMEAvatar: FC<Props> = ({ avatarImg, ...restProps }) => (
  <Avatar src={`${avatarImg}`} alt="avatar" icon={<img src={avatarIcon} />} {...restProps} />
);

export default TMEAvatar;
