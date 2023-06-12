import { message, Row, Col } from 'antd';
import { isMobile } from '../../utils/utils';
import { ReloadOutlined } from '@ant-design/icons';
import React, { useEffect, useState } from 'react';
import { getRelatedDimensionFromStatInfo } from '../../service';
import { CLS_PREFIX } from '../../common/constants';

type Props = {
  classId?: number;
  uniqueId: string | number;
  onSelect?: (value: string) => void;
};
const PAGE_SIZE = isMobile ? 3 : 10;

const DimensionSection: React.FC<Props> = ({ classId, uniqueId, onSelect }) => {
  const [dimensions, setDimensions] = useState<string[]>([]);
  const [dimensionIndex, setDimensionIndex] = useState(0);

  const prefixCls = `${CLS_PREFIX}-semantic-detail`;

  const queryDimensionList = async () => {
    const { data: resData } = await getRelatedDimensionFromStatInfo({
      classId,
      uniqueId,
    });
    const { code, data, msg } = resData;
    if (code === '0') {
      setDimensions(
        data.map(item => {
          return item.name;
        })
      );
    } else {
      message.error(msg);
    }
  };

  useEffect(() => {
    queryDimensionList();
  }, []);

  const reloadDimensionCmds = () => {
    const dimensionPageCount = Math.ceil(dimensions.length / PAGE_SIZE);
    setDimensionIndex((dimensionIndex + 1) % dimensionPageCount);
  };

  const dimensionList = dimensions.slice(
    dimensionIndex * PAGE_SIZE,
    (dimensionIndex + 1) * PAGE_SIZE
  );

  return (
    <>
      {dimensionList.length > 0 && (
        <div className={`${prefixCls}-content-section`}>
          <Row>
            <Col flex="0 0 80px">
              <div className={`${prefixCls}-label`}> 常用维度：</div>
            </Col>
            <Col flex="1 1" className={`${prefixCls}-content-col`}>
              <div className={`${prefixCls}-content-col-box`}>
                {dimensionList.map((dimension, index) => {
                  return (
                    <>
                      <span
                        className={`${prefixCls}-section-item`}
                        onClick={() => {
                          onSelect?.(dimension);
                        }}
                      >
                        {dimension}
                      </span>
                      {index < dimensionList.length - 1 && '、'}
                    </>
                  );
                })}
              </div>
            </Col>
            <Col flex="0 1 50px">
              <div
                className={`${prefixCls}-reload`}
                onClick={() => {
                  reloadDimensionCmds();
                }}
              >
                <ReloadOutlined className={`${prefixCls}-reload-icon`} />
                {!isMobile && <div className={`${prefixCls}-reload-label`}>换一批</div>}
              </div>
            </Col>
          </Row>
        </div>
      )}
    </>
  );
};

export default DimensionSection;
