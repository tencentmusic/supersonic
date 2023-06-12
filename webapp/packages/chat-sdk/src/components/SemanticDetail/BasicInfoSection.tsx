import { CLS_PREFIX } from '../../common/constants';
import { Row, Col } from 'antd';

type Props = {
  description: string;
  basicInfoList: any[];
};

const BasicInfoSection: React.FC<Props> = ({ description = '', basicInfoList }) => {
  const prefixCls = `${CLS_PREFIX}-semantic-detail`;

  return (
    <>
      <div className={`${prefixCls}-info-bar`}>
        <div className={`${prefixCls}-main-entity-info`}>
          {basicInfoList.map(item => {
            return (
              <div className={`${prefixCls}-info-item`}>
                <div className={`${prefixCls}-info-name`}>{item.name}：</div>
                <div className={`${prefixCls}-info-value`}>{item.value}</div>
              </div>
            );
          })}
        </div>
      </div>
      {description && (
        <>
          <Row>
            <Col flex="0 0 52px">
              <div className={`${prefixCls}-description`}> 口径: </div>
            </Col>
            <Col flex="1 1 auto">
              <div className={`${prefixCls}-description`}>{description}</div>
            </Col>
          </Row>
        </>
      )}
    </>
  );
};

export default BasicInfoSection;
