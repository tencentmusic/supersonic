import type { CSSProperties, FC } from 'react';
import cx from 'classnames';
import iconfont from './iconfont.css';
import styles from './index.less';

export interface S2IconProps {
  icon: string;
  color?: string;
  size?: string | number;
  style?: CSSProperties;
  className?: string;
}

const S2Icon: FC<S2IconProps> = ({ color, size, icon, style, className }) => {
  return (
    <span
      className={cx(styles.s2icon, iconfont.iconfont, icon, className)}
      style={{ color, fontSize: size, ...style }}
    />
  );
};

export const ICON = iconfont;

export const AssetIcon = <S2Icon icon={ICON.iconzichan} />;

export default S2Icon;
