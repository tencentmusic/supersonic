import Message from '../ChatMsg/Message';
import { Space, Row, Col, Divider } from 'antd';
import BasicInfoSection from './BasicInfoSection';
import DimensionSection from './DimensionSection';
import RecommendSection from './RecommendSection';
import SemanticTypeTag from '../ChatMsg/SemanticInfoPopover/SemanticTypeTag';
import { CLS_PREFIX } from '../../common/constants';

type Props = {
  dataSource?: any;
  onDimensionSelect?: (value: any) => void;
};

const SemanticDetail: React.FC<Props> = ({ dataSource, onDimensionSelect }) => {
  const { name, nameEn, createdBy, description, className, classId, semanticInfoType } = dataSource;

  const semanticDetailCls = `${CLS_PREFIX}-semantic-detail`;

  return (
    <Message position="left" width="100%" noTime>
      <div>
        <div>
          <Row>
            <Col flex="1">
              <Space size={20}>
                <span className={`${semanticDetailCls}-title`}>{`指标详情: ${name}`}</span>
              </Space>
            </Col>
            <Col flex="0 1 40px">
              <SemanticTypeTag infoType={semanticInfoType} />
            </Col>
          </Row>
        </div>
        <BasicInfoSection
          description={description}
          basicInfoList={[
            {
              name: '主题域',
              value: className,
            },
            { name: '创建人', value: createdBy },
          ]}
        />
        <Divider style={{ margin: '12px 0 16px 0px' }} />
        <DimensionSection classId={classId} uniqueId={nameEn} onSelect={onDimensionSelect} />
        <Divider style={{ margin: '6px 0 12px 0px' }} />
        <RecommendSection classId={classId} metricName={name} onSelect={onDimensionSelect} />
      </div>
    </Message>
  );
};

export default SemanticDetail;
