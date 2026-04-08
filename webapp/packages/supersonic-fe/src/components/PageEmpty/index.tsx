import type { FC, ReactNode } from 'react';
import { Empty, type EmptyProps } from 'antd';

export type PageEmptyProps = {
  /** 主说明，建议一句行动导向文案 */
  description: ReactNode;
  /** 可选主操作（如「创建」按钮） */
  action?: ReactNode;
  image?: EmptyProps['image'];
  className?: string;
};

/**
 * 列表/区块空态薄封装：内部仍用 antd Empty，与阶段三规范一致。
 * @see docs/product/ui-commercial-saas-landing-plan.md 阶段三
 */
const PageEmpty: FC<PageEmptyProps> = ({
  description,
  action,
  image = Empty.PRESENTED_IMAGE_SIMPLE,
  className,
}) => (
  <Empty className={className} image={image} description={description}>
    {action}
  </Empty>
);

export default PageEmpty;
