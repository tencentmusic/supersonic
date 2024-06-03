import { Space, Tag, Typography } from 'antd';
import { StatusEnum } from '../enum';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_COLOR } from '../constant';
import { TagsOutlined, ReadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { ISemantic } from '../data';
import { isString } from 'lodash';
import dayjs from 'dayjs';
import { isArrayOfValues } from '@/utils/utils';
import styles from './style.less';
import IndicatorStar, { StarType } from '../components/IndicatorStar';

const { Text, Paragraph } = Typography;

export const ColumnsConfig: any = (params?: {
  indicatorInfo?: {
    url?: string;
    starType?: StarType;
  };
}) => {
  return {
    description: {
      render: (_, record: ISemantic.IMetricItem) => {
        const { description } = record;
        return (
          <Paragraph
            ellipsis={{ tooltip: description, rows: 3 }}
            style={{ width: 250, marginBottom: 0 }}
          >
            {description}
          </Paragraph>
        );
      },
    },
    dimensionInfo: {
      render: (_, record: ISemantic.IDimensionItem) => {
        const { name, alias, bizName } = record;
        return (
          <>
            <div>
              <Space>
                <span style={{ fontWeight: 500 }}>{name}</span>
              </Space>
            </div>
            <div style={{ color: '#5f748d', fontSize: 14, marginTop: 5, marginLeft: 0 }}>
              {bizName}
            </div>

            {alias && (
              <div style={{ marginTop: 8 }}>
                <Space direction="vertical" size={4}>
                  {alias && (
                    <Space
                      size={4}
                      style={{ color: '#5f748d', fontSize: 12, margin: '5px 0 5px 0' }}
                    >
                      <ReadOutlined />
                      <div style={{ width: 'max-content' }}>别名:</div>
                      <span style={{ marginLeft: 2 }}>
                        <Space size={[0, 8]} wrap>
                          {isString(alias) &&
                            alias.split(',').map((aliasName: string) => {
                              return (
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
                              );
                            })}
                        </Space>
                      </span>
                    </Space>
                  )}
                </Space>
              </div>
            )}
          </>
        );
      },
    },
    indicatorInfo: {
      render: (_, record: ISemantic.IMetricItem) => {
        const { name, alias, bizName, classifications, id, isCollect } = record;
        let url = `/metric/detail/`;
        let starType: StarType = 'metric';
        if (params) {
          if (params?.indicatorInfo?.url) {
            url = params.indicatorInfo.url;
          }
          if (params?.indicatorInfo?.starType) {
            starType = params.indicatorInfo.starType;
          }
        }
        return (
          <>
            <div>
              <Space>
                <a
                  className={styles.textLink}
                  style={{ fontWeight: 500 }}
                  onClick={(event: any) => {
                    history.push(`${url}${id}`);
                    event.preventDefault();
                    event.stopPropagation();
                  }}
                  href={`/webapp${url}${id}`}
                >
                  {name}
                </a>
                <IndicatorStar indicatorId={id} type={starType} initState={isCollect} />
                {/* <Tag
                color={SENSITIVE_LEVEL_COLOR[sensitiveLevel]}
                style={{ lineHeight: '16px', position: 'relative', top: '-1px' }}
              >
                {SENSITIVE_LEVEL_ENUM[sensitiveLevel]}
              </Tag> */}
              </Space>
            </div>
            <div style={{ color: '#5f748d', fontSize: 14, marginTop: 5, marginLeft: 0 }}>
              {bizName}
            </div>

            {(alias || isArrayOfValues(classifications)) && (
              <div style={{ marginTop: 8 }}>
                <Space direction="vertical" size={4}>
                  {alias && (
                    <Space
                      size={4}
                      style={{ color: '#5f748d', fontSize: 12, margin: '5px 0 5px 0' }}
                    >
                      <ReadOutlined />
                      <div style={{ width: 'max-content' }}>别名:</div>
                      <span style={{ marginLeft: 2 }}>
                        <Space size={[0, 8]} wrap>
                          {isString(alias) &&
                            alias.split(',').map((aliasName: string) => {
                              return (
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
                              );
                            })}
                        </Space>
                      </span>
                    </Space>
                  )}

                  {isArrayOfValues(classifications) && (
                    <Space
                      size={4}
                      style={{ color: '#5f748d', fontSize: 12, margin: '5px 0 5px 0' }}
                    >
                      <TagsOutlined />
                      <div style={{ width: 'max-content' }}>分类:</div>
                      <span style={{ marginLeft: 2 }}>
                        <Space size={[0, 8]} wrap>
                          {classifications.map((tag: string) => {
                            return (
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
                            );
                          })}
                        </Space>
                      </span>
                    </Space>
                  )}
                  {/* <Space size={10}>
        <Space
          size={2}
          style={{ color: '#5f748d', fontSize: 12, position: 'relative', top: '1px' }}
        >
          <FieldNumberOutlined style={{ fontSize: 16, position: 'relative', top: '1px' }} />
          <span>:</span>
          <span style={{ marginLeft: 0 }}>{id}</span>
        </Space>
        <Space size={2} style={{ color: '#5f748d', fontSize: 12 }}>
          <UserOutlined />
          <span>:</span>
          <span style={{ marginLeft: 0 }}>{createdBy}</span>
        </Space>
      </Space> */}
                </Space>
              </div>
            )}
          </>
        );
      },
    },
    sensitiveLevel: {
      render: (_, record: ISemantic.IMetricItem) => {
        const { sensitiveLevel } = record;
        return SENSITIVE_LEVEL_COLOR[sensitiveLevel] ? (
          <Tag
            color={SENSITIVE_LEVEL_COLOR[sensitiveLevel]}
            style={{
              borderRadius: '40px',
              padding: '2px 16px',
              fontSize: '13px',
              // color: '#8ca3ba',
            }}
          >
            {SENSITIVE_LEVEL_ENUM[sensitiveLevel]}
          </Tag>
        ) : (
          <Tag
            style={{
              borderRadius: '40px',
              padding: '2px 16px',
              fontSize: '13px',
            }}
          >
            未知
          </Tag>
        );
      },
    },
    state: {
      render: (status) => {
        let tagProps: { color: string; label: string; style?: any } = {
          color: 'default',
          label: '未知',
          style: {},
        };
        switch (status) {
          case StatusEnum.ONLINE:
            tagProps = {
              // color: 'processing',
              color: 'geekblue',
              label: '已启用',
            };
            break;
          case StatusEnum.OFFLINE:
            tagProps = {
              color: 'default',
              label: '未启用',
              style: {
                color: 'rgb(95, 116, 141)',
                fontWeight: 400,
              },
            };
            break;
          case StatusEnum.INITIALIZED:
            tagProps = {
              color: 'processing',
              label: '初始化',
            };
            break;
          case StatusEnum.DELETED:
            tagProps = {
              color: 'default',
              label: '已删除',
            };
            break;
          case StatusEnum.UNAVAILABLE:
            tagProps = {
              color: 'default',
              label: '不可用',
            };
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
      render: (value: any, record: ISemantic.IMetricItem) => {
        return (
          <Space direction="vertical">
            <span> {record.createdBy}</span>
            <span>{value && value !== '-' ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'}</span>
          </Space>
        );
      },
    },
  };
};
