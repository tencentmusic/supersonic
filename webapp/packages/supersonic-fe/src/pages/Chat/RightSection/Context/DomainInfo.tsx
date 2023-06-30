import { DomainType } from '../../type';
import styles from './style.less';

type Props = {
  domain: DomainType;
};

const DomainInfo: React.FC<Props> = ({ domain }) => {
  return (
    <div className={styles.context}>
      <div className={styles.title}>相关信息</div>
      <div className={styles.content}>
        <div className={styles.field}>
          <span className={styles.fieldName}>主题域：</span>
          <span className={styles.fieldValue}>{domain.name}</span>
        </div>
      </div>
    </div>
  );
};

export default DomainInfo;
