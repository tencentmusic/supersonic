import { CheckCard } from '@ant-design/pro-components';
import React from 'react';
import { ISemantic } from '../data';
import { connect } from 'umi';
import icon from '../../../assets/icon/cloudEditor.svg';
import type { Dispatch } from 'umi';
import type { StateType } from '../model';
import { formatNumber } from '../../../utils/utils';
import styles from './style.less';

type Props = {
  modelList: ISemantic.IDomainItem[];
  domainManger: StateType;
  dispatch: Dispatch;
};

const OverView: React.FC<Props> = ({ domainManger, dispatch, modelList }) => {
  const { selectDomainId } = domainManger;

  const extraNode = (model: ISemantic.IDomainItem) => {
    const { metricCnt, dimensionCnt } = model;
    return (
      <div className={styles.overviewExtraContainer}>
        <div className={styles.extraWrapper}>
          <div className={styles.extraStatistic}>
            <div className={styles.extraTitle}>维度数</div>
            <div className={styles.extraValue}>
              <span className="ant-statistic-content-value">{formatNumber(dimensionCnt || 0)}</span>
            </div>
          </div>
        </div>
        <div className={styles.extraWrapper}>
          <div className={styles.extraStatistic}>
            <div className={styles.extraTitle}>指标数</div>
            <div className={styles.extraValue}>
              <span className="ant-statistic-content-value">{formatNumber(metricCnt || 0)}</span>
            </div>
          </div>
        </div>
      </div>
    );
  };
  return (
    <>
      <CheckCard.Group value={selectDomainId} defaultValue={selectDomainId}>
        {modelList &&
          modelList.map((model: ISemantic.IDomainItem) => {
            return (
              <CheckCard
                avatar={icon}
                title={model.name}
                key={model.id}
                value={model.id}
                // description={model.description || '模型描述...'}
                description={extraNode(model)}
                onClick={() => {
                  const { id, name } = model;
                  dispatch({
                    type: 'domainManger/setSelectDomain',
                    selectDomainId: id,
                    selectDomainName: name,
                    domainData: model,
                  });
                }}
              />
            );
          })}
      </CheckCard.Group>
    </>
  );
};

export default connect(({ domainManger }: { domainManger: StateType }) => ({
  domainManger,
}))(OverView);
