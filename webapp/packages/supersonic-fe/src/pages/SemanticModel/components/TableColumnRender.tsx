import { Space, Tag, Typography } from 'antd';
import { StatusEnum } from '../enum';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_COLOR } from '../constant';
import { TagsOutlined, ReadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { ISemantic } from '../data';
import { isString } from 'lodash';
import dayjs from 'dayjs';
import { isArrayOfValues, replaceRouteParams } from '@/utils/utils';
import styles from './style.less';
import IndicatorStar, { StarType } from '../components/IndicatorStar';

interface IndicatorInfo {
  url?: string;
  starType?: StarType;
  onNameClick?: (record: ISemantic.IMetricItem | ISemantic.IDimensionItem) => void | boolean;
}

interface ColumnsConfigParams {
  indicatorInfo?: IndicatorInfo;
}

const { Text, Paragraph } = Typography;

export const ColumnsConfig = (params?: ColumnsConfigParams) => {
  const renderAliasAndClassifications = (
    alias: string | undefined,
    classifications: string[] | undefined,
  ) => (
    <div style={{ marginTop: 8 }}>
      <Space direction="vertical" size={4}>
        {alias && (
          <Space size={4} style={{ color: '#5f748d', fontSize: 12, margin: '5px 0 5px 0' }}>
            <ReadOutlined />
            <div style={{ width: 'max-content' }}>别名:</div>
            <span style={{ marginLeft: 2 }}>
              <Space size={[0, 8]} wrap>
                {isString(alias) &&
                  alias.split(',').map((aliasName: string) => (
                    <Tag
                      color="#eee"
                      key={aliasName}
                      style={{
                        borderRadius: 44,
                        maxWidth: 90,
                        minWidth: 40,
                        backgroundColor: 'rgba(18, 31, 67, 0.04)',
                      }}
                    >
                      <Text
                        style={{
                          maxWidth: 80,
                          color: 'rgb(95, 116, 141)',
                          textAlign: 'center',
                          fontSize: 12,
                        }}
                        ellipsis={{ tooltip: aliasName }}
                      >
                        {aliasName}
                      </Text>
                    </Tag>
                  ))}
              </Space>
            </span>
          </Space>
        )}

        {isArrayOfValues(classifications) && (
          <Space size={4} style={{ color: '#5f748d', fontSize: 12, margin: '5px 0 5px 0' }}>
            <TagsOutlined />
            <div style={{ width: 'max-content' }}>分类:</div>
            <span style={{ marginLeft: 2 }}>
              <Space size={[0, 8]} wrap>
                {classifications.map((tag: string) => (
                  <Tag
                    color="#eee"
                    key={tag}
                    style={{
                      borderRadius: 44,
                      maxWidth: 90,
                      minWidth: 40,
                      backgroundColor: 'rgba(18, 31, 67, 0.04)',
                    }}
                  >
                    <Text
                      style={{
                        maxWidth: 80,
                        color: 'rgb(95, 116, 141)',
                        textAlign: 'center',
                        fontSize: 12,
                      }}
                      ellipsis={{ tooltip: tag }}
                    >
                      {tag}
                    </Text>
                  </Tag>
                ))}
              </Space>
            </span>
          </Space>
        )}
      </Space>
    </div>
  );

  return {
    description: {
      render: (_, record: ISemantic.IMetricItem) => (
        <Paragraph
          ellipsis={{ tooltip: record.description, rows: 3 }}
          style={{ width: 250, marginBottom: 0 }}
        >
          {record.description}
        </Paragraph>
      ),
    },
    dimensionInfo: {
      render: (_, record: ISemantic.IDimensionItem) => {
        const { name, alias, bizName, id, domainId, modelId } = record;
        let url = `/demension/detail/${id}`;
        if (params?.indicatorInfo) {
          url = replaceRouteParams(params.indicatorInfo.url || '', {
            domainId: `${domainId}`,
            modelId: `${modelId}`,
            indicatorId: `${id}`,
          });
        }
        return (
          <>
            <div>
              <Space>
                <a
                  className={styles.textLink}
                  style={{ fontWeight: 500 }}
                  onClick={(event: any) => {
                    if (params?.indicatorInfo?.onNameClick) {
                      const state = params.indicatorInfo.onNameClick(record);
                      if (state === false) {
                        return;
                      }
                    }
                    history.push(url);
                    event.preventDefault();
                    event.stopPropagation();
                  }}
                >
                  {name}
                </a>
                {/* <span style={{ fontWeight: 500 }}>{name}</span> */}
              </Space>
            </div>
            <div style={{ color: '#5f748d', fontSize: 14, marginTop: 5, marginLeft: 0 }}>
              {bizName}
            </div>
            {alias && renderAliasAndClassifications(alias, undefined)}
          </>
        );
      },
    },
    indicatorInfo: {
      render: (_, record: ISemantic.IMetricItem) => {
        const { name, alias, bizName, classifications, id, isCollect, domainId, modelId } = record;

        let url = `/metric/detail/${id}`;
        let starType: StarType = 'metric';
        if (params?.indicatorInfo) {
          url = replaceRouteParams(params.indicatorInfo.url || '', {
            domainId: `${domainId}`,
            modelId: `${modelId}`,
            indicatorId: `${id}`,
          });
          starType = params.indicatorInfo.starType || 'metric';
        }

        return (
          <>
            <div>
              <Space>
                <a
                  className={styles.textLink}
                  style={{ fontWeight: 500 }}
                  onClick={(event: any) => {
                    if (params?.indicatorInfo?.onNameClick) {
                      const state = params.indicatorInfo.onNameClick(record);
                      if (state === false) {
                        return;
                      }
                    }
                    history.push(url);
                    event.preventDefault();
                    event.stopPropagation();
                  }}
                >
                  {name}
                </a>
                <IndicatorStar indicatorId={id} type={starType} initState={isCollect} />
              </Space>
            </div>
            <div style={{ color: '#5f748d', fontSize: 14, marginTop: 5, marginLeft: 0 }}>
              {bizName}
            </div>
            {alias && renderAliasAndClassifications(alias, classifications)}
          </>
        );
      },
    },
    sensitiveLevel: {
      render: (_, record: ISemantic.IMetricItem) => (
        <Tag
          color={SENSITIVE_LEVEL_COLOR[record.sensitiveLevel] || 'default'}
          style={{
            borderRadius: '40px',
            padding: '2px 16px',
            fontSize: '13px',
          }}
        >
          {SENSITIVE_LEVEL_ENUM[record.sensitiveLevel] || '未知'}
        </Tag>
      ),
    },
    state: {
      render: (status) => {
        const tagProps = {
          color: 'default',
          label: '未知',
          style: {},
        };
        switch (status) {
          case StatusEnum.ONLINE:
            tagProps.color = 'geekblue';
            tagProps.label = '已启用';
            break;
          case StatusEnum.OFFLINE:
            tagProps.color = 'default';
            tagProps.label = '未启用';
            tagProps.style = { color: 'rgb(95, 116, 141)', fontWeight: 400 };
            break;
          case StatusEnum.INITIALIZED:
            tagProps.color = 'processing';
            tagProps.label = '初始化';
            break;
          case StatusEnum.DELETED:
            tagProps.color = 'default';
            tagProps.label = '已删除';
            break;
          case StatusEnum.UNAVAILABLE:
            tagProps.color = 'default';
            tagProps.label = '不可用';
            break;
          default:
            break;
        }
        return (
          <Tag
            color={tagProps.color}
            style={{
              borderRadius: '40px',
              padding: '2px 16px',
              fontSize: '13px',
              fontWeight: 500,
              ...tagProps.style,
            }}
          >
            {tagProps.label}
          </Tag>
        );
      },
    },
    createInfo: {
      dataIndex: 'updatedAt',
      title: '创建信息',
      tooltip: '创建人/更新时间',
      width: 180,
      search: false,
      render: (value: any, record: ISemantic.IMetricItem) => (
        <Space direction="vertical">
          <span> {record.createdBy}</span>
          <span>{value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'}</span>
        </Space>
      ),
    },
  };
};
