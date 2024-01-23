import { Space, Tag, Typography } from 'antd';
import { StatusEnum } from '../enum';
import { SENSITIVE_LEVEL_ENUM, SENSITIVE_LEVEL_COLOR } from '../constant';
import { TagsOutlined, UserOutlined, FieldNumberOutlined, ReadOutlined } from '@ant-design/icons';
import { history } from 'umi';
import { ISemantic } from '../data';
import { isString } from 'lodash';
import { isArrayOfValues } from '@/utils/utils';
import styles from './style.less';
import MetricStar from '../Metric/components/MetricStar';

const { Text, Link } = Typography;

export const ColumnsConfig = {
  metricInfo: {
    render: (_, record: ISemantic.IMetricItem) => {
      const { name, alias, bizName, sensitiveLevel, createdBy, tags, id, isCollect } = record;
      return (
        <>
          <div>
            <Space>
              <MetricStar metricId={id} initState={isCollect} />

              <a
                className={styles.textLink}
                style={{ fontWeight: 500, marginRight: 10 }}
                onClick={() => {
                  history.push(`/metric/detail/${id}`);
                }}
              >
                {name}
              </a>
              <Tag
                color={SENSITIVE_LEVEL_COLOR[sensitiveLevel]}
                style={{ lineHeight: '16px', position: 'relative', top: '-1px' }}
              >
                {SENSITIVE_LEVEL_ENUM[sensitiveLevel]}
              </Tag>
            </Space>
          </div>
          <div style={{ color: '#5f748d', fontSize: 14, marginTop: 5 }}>{bizName}</div>

          <div style={{ marginTop: 8 }}>
            <Space direction="vertical" size={4}>
              {alias && (
                <Space size={4} style={{ color: '#5f748d', fontSize: 12 }}>
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

              {isArrayOfValues(tags) && (
                <Space size={2} style={{ color: '#5f748d', fontSize: 12 }}>
                  <TagsOutlined />
                  <div style={{ width: 'max-content' }}>标签:</div>
                  <span style={{ marginLeft: 2 }}>
                    <Space size={[0, 8]} wrap>
                      {tags.map((tag: string) => {
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
              <Space size={10}>
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
              </Space>
            </Space>
          </div>
        </>
      );
    },
  },
  state: {
    render: (status) => {
      let tagProps = {
        color: 'default',
        label: '未知',
      };
      switch (status) {
        case StatusEnum.ONLINE:
          tagProps = {
            color: 'success',
            // color: '#87d068',
            label: '已启用',
          };
          break;
        case StatusEnum.OFFLINE:
          tagProps = {
            color: 'warning',
            label: '未启用',
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
          }}
        >
          {tagProps.label}
        </Tag>
      );
    },
  },
};
