import React, { useState, useEffect, useRef } from 'react';
import { message, Row, Col, Button, Space, Select, Form, Tooltip } from 'antd';
import { queryStruct } from '@/pages/SemanticModel/service';
import { DownloadOutlined, PoweroffOutlined, SearchOutlined } from '@ant-design/icons';
import TrendChart from '@/pages/SemanticModel/Metric/components/MetricTrend';
import MetricTrendDimensionFilterContainer from './MetricTrendDimensionFilterContainer';
import MDatePicker from '@/components/MDatePicker';
import { DateRangeType, DateSettingType } from '@/components/MDatePicker/type';
import { getDatePickerDynamicInitialValues } from '@/components/MDatePicker/utils';
import StandardFormRow from '@/components/StandardFormRow';
import MetricTable from './Table';
import { ColumnConfig } from '../data';
import dayjs from 'dayjs';
import { ISemantic } from '../../data';
import { DateFieldMap } from '@/pages/SemanticModel/constant';
import { ProCard } from '@ant-design/pro-components';

import styles from '../style.less';

const FormItem = Form.Item;

type Props = {
  relationDimensionOptions: { value: string; label: string; modelId: number }[];
  dimensionList: ISemantic.IDimensionItem[];
  metircData?: ISemantic.IMetricItem;
  [key: string]: any;
};

const MetricTrendSection: React.FC<Props> = ({
  metircData,
  relationDimensionOptions,
  dimensionList,
}) => {
  const indicatorFields = useRef<{ name: string; column: string }[]>([]);
  const [metricTrendData, setMetricTrendData] = useState<ISemantic.IMetricTrendItem[]>([]);
  const [metricTrendLoading, setMetricTrendLoading] = useState<boolean>(false);
  const [metricColumnConfig, setMetricColumnConfig] = useState<ISemantic.IMetricTrendColumn>();
  const [authMessage, setAuthMessage] = useState<string>('');
  const [downloadLoding, setDownloadLoding] = useState<boolean>(false);
  const [queryParams, setQueryParams] = useState<any>({});
  const [downloadBtnDisabledState, setDownloadBtnDisabledState] = useState<boolean>(true);
  const [periodDate, setPeriodDate] = useState<{
    startDate: string;
    endDate: string;
    dateField: string;
    period: DateRangeType;
  }>({
    startDate: dayjs().subtract(6, 'days').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD'),
    dateField: DateFieldMap[DateRangeType.DAY],
    period: DateRangeType.DAY,
  });
  const [rowNumber, setRowNumber] = useState<number>(5);

  const [tableColumnConfig, setTableColumnConfig] = useState<ColumnConfig[]>([]);

  const [transformState, setTransformState] = useState<boolean>(false);

  const [groupByDimensionFieldName, setGroupByDimensionFieldName] = useState<string>();

  const getMetricTrendData = async (params: any = { download: false }) => {
    const { download, dimensionGroup = [], dimensionFilters = [] } = params;
    if (download) {
      setDownloadLoding(true);
    } else {
      setMetricTrendLoading(true);
    }
    if (!metircData) {
      return;
    }
    const { bizName, id, domainId, name } = metircData;
    indicatorFields.current = [{ name, column: bizName }];

    const dimensionIds = dimensionList.reduce(
      (idList: number[], item: ISemantic.IDimensionItem) => {
        if (dimensionGroup.includes(item.bizName)) {
          idList.push(item.id);
        }
        return idList;
      },
      [],
    );
    const res = await queryStruct({
      domainId,
      metricIds: [id],
      dimensionIds,
      filters: dimensionFilters,
      period: periodDate.period,
      dateField: periodDate.dateField,
      startDate: periodDate.startDate,
      endDate: periodDate.endDate,
      download,
      isTransform: transformState,
    });
    if (download) {
      setDownloadLoding(false);
      return;
    }
    const { code, data, msg } = res;
    setMetricTrendLoading(false);
    if (code === 200) {
      const { resultList, columns, queryAuthorization } = data;

      setTableColumnConfig(columns);
      const message = queryAuthorization?.message;
      if (message) {
        setAuthMessage(message);
      }
      const targetConfig = columns.find((item: ISemantic.IMetricTrendColumn) => {
        return item.nameEn === bizName;
      });
      if (targetConfig) {
        setMetricColumnConfig(targetConfig);
      }

      const dateConfig = columns.find((item: ISemantic.IMetricTrendColumn) => {
        return item.type === 'DATE';
      });

      if (dateConfig) {
        const sortDateField = dateConfig.nameEn;
        setMetricTrendData(
          [...resultList].sort((a, b) => {
            return a[sortDateField].localeCompare(b[sortDateField]);
          }),
        );
      } else {
        setMetricTrendData(resultList);
      }

      setDownloadBtnDisabledState(false);
      if (dimensionGroup[dimensionGroup.length - 1]) {
        setGroupByDimensionFieldName(dimensionGroup[dimensionGroup.length - 1]);
      }
    } else {
      if (code === 401 || code === 400) {
        setAuthMessage(msg);
      } else {
        message.error(msg);
      }
      setDownloadBtnDisabledState(true);
      setMetricTrendData([]);
      setMetricColumnConfig(undefined);
    }
  };

  useEffect(() => {
    if (metircData?.id) {
      getMetricTrendData({ ...queryParams });
    }
  }, [metircData]);

  return (
    <div className={styles.metricTrendSection}>
      <div className={styles.sectionBox}>
        <Row style={{ padding: '10px 10px 0px 10px' }}>
          <Col flex="1 1 200px">
            <Form
              layout="inline"
              colon={false}
              onValuesChange={(value, values) => {
                if (value.key) {
                  return;
                }
              }}
            >
              <StandardFormRow key="metricDate" title="日期区间:">
                <FormItem name="metricDate">
                  <MDatePicker
                    initialValues={getDatePickerDynamicInitialValues(7, DateRangeType.DAY)}
                    showCurrentDataRangeString={false}
                    onDateRangeChange={(value, config) => {
                      const [startDate, endDate] = value;
                      const { dateSettingType, dynamicParams, staticParams } = config;
                      let dateField = DateFieldMap[DateRangeType.DAY];
                      let period = DateRangeType.DAY;
                      if (DateSettingType.DYNAMIC === dateSettingType) {
                        dateField = DateFieldMap[dynamicParams.dateRangeType];
                        period = dynamicParams.dateRangeType;
                      }
                      if (DateSettingType.STATIC === dateSettingType) {
                        dateField = DateFieldMap[staticParams.dateRangeType];
                        period = staticParams.dateRangeType;
                      }
                      setPeriodDate({ startDate, endDate, dateField, period });
                    }}
                    disabledAdvanceSetting={true}
                  />
                </FormItem>
              </StandardFormRow>
              <StandardFormRow key="dimensionSelected" title="维度下钻:">
                <FormItem name="dimensionSelected">
                  <Select
                    style={{ minWidth: 150, maxWidth: 200 }}
                    options={relationDimensionOptions}
                    showSearch
                    filterOption={(input, option) =>
                      ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
                    }
                    mode="multiple"
                    placeholder="请选择下钻维度"
                    onChange={(value) => {
                      const params = { ...queryParams, dimensionGroup: value || [] };
                      setQueryParams(params);
                    }}
                  />
                </FormItem>
              </StandardFormRow>
              <StandardFormRow key="dimensionFilter" title="维度筛选:">
                <FormItem name="dimensionFilter">
                  <MetricTrendDimensionFilterContainer
                    modelId={metircData?.modelId || 0}
                    dimensionOptions={relationDimensionOptions}
                    periodDate={periodDate}
                    onChange={(filterList) => {
                      const dimensionFilters = filterList.map((item) => {
                        const { dimensionBizName, dimensionValue, operator } = item;
                        return {
                          bizName: dimensionBizName,
                          value: dimensionValue,
                          operator,
                        };
                      });
                      const params = {
                        ...queryParams,
                        dimensionFilters,
                      };
                      setQueryParams(params);
                    }}
                    afterSolt={
                      <Button
                        type="primary"
                        icon={<SearchOutlined />}
                        size="middle"
                        loading={metricTrendLoading}
                        onClick={() => {
                          getMetricTrendData({ ...queryParams });
                        }}
                      >
                        查 询
                      </Button>
                    }
                  />
                </FormItem>
              </StandardFormRow>
            </Form>
          </Col>
          <Col flex="0 1" />
        </Row>
        {/* <Row style={{ paddingLeft: 82, paddingBottom: 8 }}>

        </Row> */}
      </div>

      <div className={styles.sectionBox}>
        <ProCard
          size="small"
          title={
            <>
              <span>数据趋势</span>
              {authMessage && <div style={{ color: '#d46b08' }}>{authMessage}</div>}
            </>
          }
        >
          <TrendChart
            data={metricTrendData}
            isPer={
              metricColumnConfig?.dataFormatType === 'percent' &&
              metricColumnConfig?.dataFormat?.needMultiply100 === false
                ? true
                : false
            }
            isPercent={
              metricColumnConfig?.dataFormatType === 'percent' &&
              metricColumnConfig?.dataFormat?.needMultiply100 === true
                ? true
                : false
            }
            rowNumber={rowNumber}
            fields={indicatorFields.current}
            loading={metricTrendLoading}
            dateFieldName={periodDate.dateField}
            groupByDimensionFieldName={groupByDimensionFieldName}
            height={350}
            renderType="clear"
            decimalPlaces={metricColumnConfig?.dataFormat?.decimalPlaces || 2}
          />
        </ProCard>
      </div>

      <div className={styles.sectionBox} style={{ paddingBottom: 0 }}>
        <ProCard
          size="small"
          title="数据明细"
          collapsible
          extra={
            <Space.Compact block>
              <Button
                size="middle"
                type="primary"
                key="download"
                loading={downloadLoding}
                disabled={downloadBtnDisabledState}
                onClick={() => {
                  getMetricTrendData({ download: true, ...queryParams });
                }}
              >
                <Space>
                  <DownloadOutlined />下 载
                </Space>
              </Button>

              <Tooltip title="开启转置">
                <Button
                  size="middle"
                  type={transformState ? 'primary' : 'default'}
                  icon={<PoweroffOutlined />}
                  onClick={() => {
                    setTransformState(!transformState);
                  }}
                />
              </Tooltip>
            </Space.Compact>
          }
        >
          <div style={{ minHeight: '450px' }}>
            <MetricTable
              loading={metricTrendLoading}
              columnConfig={tableColumnConfig}
              dataSource={metricTrendData}
              dateFieldName={periodDate.dateField}
              metricFieldName={indicatorFields.current?.[0]?.column}
            />
          </div>
        </ProCard>
      </div>
    </div>
  );
};

export default MetricTrendSection;
