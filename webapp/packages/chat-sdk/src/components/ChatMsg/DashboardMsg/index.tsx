import { Card, Row, Col, Statistic, Table as AntTable, Tooltip, Alert, Tag, Collapse } from 'antd';
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  BulbOutlined,
  PieChartOutlined,
} from '@ant-design/icons';
import * as echarts from 'echarts';
import { useEffect, useRef } from 'react';
import {
  MsgDataType,
  KpiMetricType,
  TrendDataPointType,
  DashboardDataType,
  AnalysisResultType,
  AnomalyType,
  InsightType,
  AttributionType,
} from '../../../common/type';
import { PREFIX_CLS, THEME_COLOR_LIST } from '../../../common/constants';
import { formatByThousandSeperator, formatNumberWithCN } from '../../../utils/utils';
import './style.less';

type Props = {
  data: MsgDataType;
  onSendMsg?: (msg: string) => void;
};

const DashboardMsg: React.FC<Props> = ({ data }) => {
  const prefixCls = `${PREFIX_CLS}-dashboard`;
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts>();

  // Parse dashboard data from response
  const response = data.response as any;
  const dashboardData: DashboardDataType | null = response?.dashboardData || parseLegacyData(data);

  // Parse legacy query results to dashboard format
  function parseLegacyData(msgData: MsgDataType): DashboardDataType | null {
    const { queryColumns, queryResults } = msgData;
    if (!queryColumns || !queryResults || queryResults.length === 0) {
      return null;
    }

    const dateColumn = queryColumns.find(
      (col) => col.showType === 'DATE' || col.type === 'DATE'
    );
    const metricColumns = queryColumns.filter((col) => col.showType === 'NUMBER');

    if (!dateColumn || metricColumns.length === 0) {
      return null;
    }

    // Get latest and previous day data for KPI calculation
    const sortedResults = [...queryResults].sort(
      (a, b) => new Date(b[dateColumn.bizName]).getTime() - new Date(a[dateColumn.bizName]).getTime()
    );
    const latestData = sortedResults[0];
    const previousData = sortedResults[1];

    // Build KPI metrics (take first 4 metrics)
    const kpiMetrics: KpiMetricType[] = metricColumns.slice(0, 4).map((col) => {
      const currentValue = Number(latestData?.[col.bizName]) || 0;
      const prevValue = Number(previousData?.[col.bizName]) || 0;
      const trendPercent = prevValue !== 0 ? ((currentValue - prevValue) / prevValue) * 100 : 0;

      return {
        name: col.name,
        bizName: col.bizName,
        value: currentValue,
        previousValue: prevValue,
        trend: trendPercent > 0 ? 'up' : trendPercent < 0 ? 'down' : 'flat',
        trendPercent: Math.abs(trendPercent),
        description: col.name,
      };
    });

    // Build trend data (last 7 days)
    const trendData: TrendDataPointType[] = sortedResults
      .slice(0, 7)
      .reverse()
      .map((row) => ({
        date: row[dateColumn.bizName],
        ...metricColumns.reduce((acc, col) => {
          acc[col.bizName] = Number(row[col.bizName]) || 0;
          return acc;
        }, {} as Record<string, number>),
      }));

    return {
      kpiMetrics,
      trendData,
      trendMetrics: metricColumns.slice(0, 4).map((col) => col.bizName),
      detailColumns: queryColumns.map((col) => ({
        name: col.name,
        bizName: col.bizName,
        type: col.showType || col.type,
      })),
      detailData: sortedResults.slice(0, 10),
      reportDate: latestData?.[dateColumn.bizName],
    };
  }

  // Render trend chart
  useEffect(() => {
    if (!dashboardData?.trendData || !chartRef.current) return;

    if (!chartInstance.current) {
      chartInstance.current = echarts.init(chartRef.current);
    }

    const { trendData, trendMetrics, kpiMetrics } = dashboardData;
    const xAxisData = trendData.map((item) => {
      const date = item.date;
      return date.length === 10 ? date.slice(5) : date; // MM-DD format
    });

    const series = trendMetrics.slice(0, 4).map((metricBizName, index) => {
      const metricInfo = kpiMetrics.find((m) => m.bizName === metricBizName);
      return {
        name: metricInfo?.name || metricBizName,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: trendData.map((item) => item[metricBizName]),
        itemStyle: {
          color: THEME_COLOR_LIST[index],
        },
        lineStyle: {
          width: 2,
        },
      };
    });

    chartInstance.current.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
        },
      },
      legend: {
        data: series.map((s) => s.name),
        bottom: 0,
        type: 'scroll',
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '10%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: xAxisData,
        axisLine: {
          lineStyle: {
            color: '#ccc',
          },
        },
        axisLabel: {
          color: '#666',
        },
      },
      yAxis: {
        type: 'value',
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#eee',
          },
        },
        axisLabel: {
          color: '#666',
          formatter: (value: number) => formatNumberWithCN(value),
        },
      },
      series,
    });

    const handleResize = () => {
      chartInstance.current?.resize();
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [dashboardData]);

  if (!dashboardData) {
    return <div className={`${prefixCls}-empty`}>暂无仪表盘数据</div>;
  }

  const { kpiMetrics, detailColumns, detailData, title, reportDate, analysis } = dashboardData;

  // Build table columns
  const tableColumns = detailColumns.map((col) => ({
    dataIndex: col.bizName,
    key: col.bizName,
    title: col.name,
    render: (value: any) => {
      if (col.type === 'NUMBER') {
        return (
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>
            {formatByThousandSeperator(value)}
          </span>
        );
      }
      return value;
    },
  }));

  // Get status icon and color
  const getStatusConfig = (status: string) => {
    switch (status) {
      case 'CRITICAL':
        return { icon: <ExclamationCircleOutlined />, color: '#ff4d4f', text: '异常', bgColor: '#fff2f0' };
      case 'WARNING':
        return { icon: <WarningOutlined />, color: '#faad14', text: '关注', bgColor: '#fffbe6' };
      case 'GOOD':
      default:
        return { icon: <CheckCircleOutlined />, color: '#52c41a', text: '正常', bgColor: '#f6ffed' };
    }
  };

  // Render analysis summary section
  const renderAnalysisSection = () => {
    if (!analysis) return null;

    const statusConfig = getStatusConfig(analysis.overallStatus);
    const hasAnomalies = analysis.anomalies && analysis.anomalies.length > 0;
    const hasInsights = analysis.insights && analysis.insights.length > 0;

    return (
      <Card
        className={`${prefixCls}-analysis-section`}
        size="small"
        style={{ marginBottom: 12, background: statusConfig.bgColor, borderColor: statusConfig.color }}
      >
        {/* Summary Header */}
        <div className={`${prefixCls}-analysis-header`}>
          <Tag color={statusConfig.color} icon={statusConfig.icon}>
            {statusConfig.text}
          </Tag>
          <span className={`${prefixCls}-analysis-summary`}>{analysis.summary}</span>
        </div>

        {/* LLM Insight */}
        {analysis.llmInsight && (
          <div className={`${prefixCls}-llm-insight`}>
            <div className={`${prefixCls}-llm-insight-title`}>
              <span style={{ marginRight: 4 }}>🤖</span>
              AI 分析师解读
            </div>
            <div className={`${prefixCls}-llm-insight-content`}>
              {analysis.llmInsight}
            </div>
          </div>
        )}

        {/* Insights */}
        {hasInsights && (
          <div className={`${prefixCls}-insights`}>
            <div className={`${prefixCls}-insights-title`}>
              <BulbOutlined style={{ marginRight: 4 }} />
              关键洞察
            </div>
            <div className={`${prefixCls}-insights-list`}>
              {analysis.insights.slice(0, 4).map((insight, idx) => (
                <div key={idx} className={`${prefixCls}-insight-item`}>
                  <Tag
                    color={
                      insight.type === 'CONSECUTIVE'
                        ? insight.description.includes('下')
                          ? 'red'
                          : 'green'
                        : insight.type === 'THRESHOLD'
                        ? 'orange'
                        : 'blue'
                    }
                    style={{ marginRight: 8 }}
                  >
                    {insight.type === 'TREND'
                      ? '趋势'
                      : insight.type === 'CONSECUTIVE'
                      ? '连续'
                      : insight.type === 'THRESHOLD'
                      ? '阈值'
                      : '分析'}
                  </Tag>
                  <span>{insight.title}</span>
                  {insight.confidence >= 0.8 && (
                    <Tooltip title={`置信度: ${(insight.confidence * 100).toFixed(0)}%`}>
                      <span style={{ marginLeft: 4, color: '#999', fontSize: 12 }}>
                        ({(insight.confidence * 100).toFixed(0)}%)
                      </span>
                    </Tooltip>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Anomalies */}
        {hasAnomalies && (
          <Collapse
            ghost
            size="small"
            className={`${prefixCls}-anomalies-collapse`}
            items={[
              {
                key: 'anomalies',
                label: (
                  <span style={{ color: '#ff4d4f' }}>
                    <WarningOutlined style={{ marginRight: 4 }} />
                    检测到 {analysis.anomalyCount} 个异常点
                  </span>
                ),
                children: (
                  <div className={`${prefixCls}-anomalies-list`}>
                    {analysis.anomalies.map((anomaly, idx) => (
                      <div key={idx} className={`${prefixCls}-anomaly-item`}>
                        <Tag
                          color={
                            anomaly.severity === 'HIGH'
                              ? 'red'
                              : anomaly.severity === 'MEDIUM'
                              ? 'orange'
                              : 'gold'
                          }
                        >
                          {anomaly.severity}
                        </Tag>
                        <span>{anomaly.description}</span>
                      </div>
                    ))}
                  </div>
                ),
              },
            ]}
          />
        )}
      </Card>
    );
  };

  // Render attribution analysis section
  const renderAttributionSection = () => {
    if (!analysis?.attributions || analysis.attributions.length === 0) return null;

    // Group attributions by metric
    const attributionsByMetric: Record<string, AttributionType[]> = {};
    analysis.attributions.forEach((attr) => {
      const key = attr.metricName;
      if (!attributionsByMetric[key]) {
        attributionsByMetric[key] = [];
      }
      attributionsByMetric[key].push(attr);
    });

    return (
      <Card
        className={`${prefixCls}-attribution-section`}
        size="small"
        title={
          <span>
            <PieChartOutlined style={{ marginRight: 8 }} />
            变化归因分析
          </span>
        }
        style={{ marginBottom: 12 }}
      >
        {Object.entries(attributionsByMetric).map(([metricName, attrs]) => (
          <div key={metricName} className={`${prefixCls}-attribution-metric`}>
            <div className={`${prefixCls}-attribution-metric-title`}>{metricName}</div>
            <div className={`${prefixCls}-attribution-list`}>
              {attrs.map((attr, idx) => {
                const barWidth = Math.min(Math.abs(attr.contribution), 100);
                const barColor = attr.direction === 'INCREASE' ? '#52c41a' : '#ff4d4f';
                return (
                  <div key={idx} className={`${prefixCls}-attribution-item`}>
                    <div className={`${prefixCls}-attribution-label`}>
                      <Tag color="blue" style={{ marginRight: 4 }}>
                        {attr.dimensionName}
                      </Tag>
                      <span className={`${prefixCls}-attribution-value`}>{attr.dimensionValue}</span>
                    </div>
                    <div className={`${prefixCls}-attribution-bar-container`}>
                      <div
                        className={`${prefixCls}-attribution-bar`}
                        style={{
                          width: `${barWidth}%`,
                          backgroundColor: barColor,
                        }}
                      />
                      <span
                        className={`${prefixCls}-attribution-percent`}
                        style={{ color: barColor }}
                      >
                        {attr.contribution > 0 ? '+' : ''}
                        {attr.contribution.toFixed(1)}%
                      </span>
                    </div>
                    <Tooltip
                      title={`${attr.previousValue.toLocaleString()} → ${attr.currentValue.toLocaleString()} (${attr.changePercent > 0 ? '+' : ''}${attr.changePercent.toFixed(1)}%)`}
                    >
                      <span className={`${prefixCls}-attribution-change`}>
                        {attr.direction === 'INCREASE' ? (
                          <ArrowUpOutlined style={{ color: '#52c41a' }} />
                        ) : (
                          <ArrowDownOutlined style={{ color: '#ff4d4f' }} />
                        )}
                        {Math.abs(attr.change).toLocaleString()}
                      </span>
                    </Tooltip>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
      </Card>
    );
  };

  // Render KPI card
  const renderKpiCard = (metric: KpiMetricType, index: number) => {
    const trendColor = metric.trend === 'up' ? '#52c41a' : metric.trend === 'down' ? '#ff4d4f' : '#999';
    const TrendIcon = metric.trend === 'up' ? ArrowUpOutlined : metric.trend === 'down' ? ArrowDownOutlined : null;

    return (
      <Col xs={12} sm={12} md={6} key={metric.bizName}>
        <Card
          className={`${prefixCls}-kpi-card`}
          size="small"
          style={{ borderTop: `3px solid ${THEME_COLOR_LIST[index]}` }}
        >
          <div className={`${prefixCls}-kpi-title`}>
            {metric.name}
            {metric.description && (
              <Tooltip title={metric.description}>
                <InfoCircleOutlined style={{ marginLeft: 4, color: '#999', fontSize: 12 }} />
              </Tooltip>
            )}
          </div>
          <Statistic
            value={metric.value}
            formatter={(value) => formatNumberWithCN(Number(value))}
            valueStyle={{ fontSize: 24, fontWeight: 600 }}
            suffix={metric.unit}
          />
          {metric.trendPercent !== undefined && metric.trendPercent !== 0 && (
            <div className={`${prefixCls}-kpi-trend`} style={{ color: trendColor }}>
              {TrendIcon && <TrendIcon />}
              <span style={{ marginLeft: 4 }}>
                {metric.trendPercent.toFixed(1)}% 较前日
              </span>
            </div>
          )}
        </Card>
      </Col>
    );
  };

  return (
    <div className={prefixCls}>
      {/* Header */}
      {(title || reportDate) && (
        <div className={`${prefixCls}-header`}>
          {title && <span className={`${prefixCls}-title`}>{title}</span>}
          {reportDate && <span className={`${prefixCls}-date`}>数据日期: {reportDate}</span>}
        </div>
      )}

      {/* Analysis Section */}
      {renderAnalysisSection()}

      {/* KPI Cards */}
      <Row gutter={[12, 12]} className={`${prefixCls}-kpi-section`}>
        {kpiMetrics.map((metric, index) => renderKpiCard(metric, index))}
      </Row>

      {/* Trend Chart */}
      <Card className={`${prefixCls}-trend-section`} size="small" title="近7天趋势">
        <div ref={chartRef} style={{ width: '100%', height: 280 }} />
      </Card>

      {/* Attribution Analysis */}
      {renderAttributionSection()}

      {/* Detail Table */}
      <Card className={`${prefixCls}-table-section`} size="small" title="明细数据">
        <AntTable
          columns={tableColumns}
          dataSource={detailData}
          rowKey={(_, index) => String(index)}
          pagination={detailData.length > 5 ? { pageSize: 5, size: 'small' } : false}
          size="small"
          scroll={{ x: 'max-content' }}
        />
      </Card>
    </div>
  );
};

export default DashboardMsg;
