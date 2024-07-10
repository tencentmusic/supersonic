import styles from './style.less';

type Props = {};

const MainTitleMark: React.FC<Props> = ({}) => {
  return (
    <div className={styles.mainTitleMark}>
      <i className={styles.mark} />
      <i className={styles.mark} />
      <i className={styles.mark} />
    </div>
  );
};

export default MainTitleMark;
