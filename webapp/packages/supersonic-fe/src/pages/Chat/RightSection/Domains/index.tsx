import classNames from 'classnames';
import { DomainType } from '../../type';
import styles from './style.less';

type Props = {
  domains: DomainType[];
  currentDomain?: DomainType;
  onSelectDomain: (domain: DomainType) => void;
};

const Domains: React.FC<Props> = ({ domains, currentDomain, onSelectDomain }) => {
  return (
    <div className={styles.domains}>
      <div className={styles.titleBar}>
        <div className={styles.title}>主题列表</div>
        <div className={styles.subTitle}>(可在输入框@)</div>
      </div>
      <div className={styles.domainList}>
        {domains
          .filter((domain) => domain.id !== -1)
          .map((domain) => {
            const domainItemClass = classNames(styles.domainItem, {
              [styles.activeDomainItem]: currentDomain?.id === domain.id,
            });
            return (
              <div key={domain.id}>
                <div
                  className={domainItemClass}
                  onClick={() => {
                    onSelectDomain(domain);
                  }}
                >
                  {/* <IconFont type="icon-yinleku" className={styles.domainIcon} /> */}
                  <div className={styles.domainName}>{domain.name}</div>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
};

export default Domains;
