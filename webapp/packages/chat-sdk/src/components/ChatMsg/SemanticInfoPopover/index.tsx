import { Popover, message, Row, Col, Button, Spin } from 'antd';
import React, { useEffect, useState } from 'react';
import { SemanticTypeEnum } from '../../../common/type';
import { queryMetricInfo } from '../../../service';
import SemanticTypeTag from './SemanticTypeTag';
import { isMobile } from '../../../utils/utils';
import { CLS_PREFIX } from '../../../common/constants';

type Props = {
  children: React.ReactNode;
  classId?: number;
  infoType?: SemanticTypeEnum;
  uniqueId: string | number;
  onDetailBtnClick?: (data: any) => void;
};

const SemanticInfoPopover: React.FC<Props> = ({
  classId,
  infoType,
  uniqueId,
  children,
  onDetailBtnClick,
}) => {
  const [semanticInfo, setSemanticInfo] = useState<any>(undefined);
  const [popoverVisible, setPopoverVisible] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  const prefixCls = `${CLS_PREFIX}-semantic-info-popover`;

  const text = (
    <Row>
      <Col flex="1">
        <SemanticTypeTag infoType={infoType} />
      </Col>
      {onDetailBtnClick && (
        <Col flex="0 1 40px">
          {semanticInfo && (
            <Button
              type="link"
              size="small"
              onClick={() => {
                onDetailBtnClick(semanticInfo);
              }}
            >
              详情
            </Button>
          )}
        </Col>
      )}
    </Row>
  );

  const content = loading ? (
    <div className={`${prefixCls}-spin-box`}>
      <Spin />
    </div>
  ) : (
    <div>
      <span>{semanticInfo?.description || '暂无数据'}</span>
    </div>
  );

  const getMetricInfo = async () => {
    setLoading(true);
    const { data: resData } = await queryMetricInfo({
      classId,
      uniqueId,
    });
    const { code, data, msg } = resData;
    setLoading(false);
    if (code === '0') {
      setSemanticInfo({
        ...data,
        semanticInfoType: SemanticTypeEnum.METRIC,
      });
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    if (popoverVisible && !semanticInfo) {
      getMetricInfo();
    }
  }, [popoverVisible]);

  return (
    <Popover
      placement="top"
      title={text}
      content={content}
      trigger="hover"
      open={classId && !isMobile ? undefined : false}
      onOpenChange={visible => {
        setPopoverVisible(visible);
      }}
      overlayClassName={prefixCls}
    >
      {children}
    </Popover>
  );
};

export default SemanticInfoPopover;
