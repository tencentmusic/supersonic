import React, { useState, useEffect, useRef } from 'react';
import { SemanticNodeType } from '../../enum';
import moment from 'moment';
import { message, Row, Col, Button, Space, Select, Form } from 'antd';
import {
  queryStruct,
  downloadCosFile,
  getDrillDownDimension,
  getDimensionList,
} from '@/pages/SemanticModel/service';
import TrendChart from '@/pages/SemanticModel/Metric/components/MetricTrend';
import MetricTrendDimensionFilter from './MetricTrendDimensionFilter';
import MDatePicker from '@/components/MDatePicker';
import { useModel } from 'umi';
import { DateRangeType, DateSettingType } from '@/components/MDatePicker/type';

import StandardFormRow from '@/components/StandardFormRow';

import { ISemantic } from '../../data';

const FormItem = Form.Item;

type Props = {
  nodeData: any;
  [key: string]: any;
};

const MetricTrendSection: React.FC<Props> = ({ nodeData }) => {
  const dateFieldMap = {
    [DateRangeType.DAY]: 'sys_imp_date',
    [DateRangeType.WEEK]: 'sys_imp_week',
    [DateRangeType.MONTH]: 'sys_imp_month',
  };
  const indicatorFields = useRef<{ name: string; column: string }[]>([]);
  const [metricTrendData, setMetricTrendData] = useState<ISemantic.IMetricTrendItem[]>([]);
  const [metricTrendLoading, setMetricTrendLoading] = useState<boolean>(false);
  const [metricColumnConfig, setMetricColumnConfig] = useState<ISemantic.IMetricTrendColumn>();
  const [authMessage, setAuthMessage] = useState<string>('');
  const [downloadLoding, setDownloadLoding] = useState<boolean>(false);
  const [relationDimensionOptions, setRelationDimensionOptions] = useState<
    { value: string; label: string }[]
  >([]);
  const [queryParams, setQueryParams] = useState<any>({});
  const [downloadBtnDisabledState, setDownloadBtnDisabledState] = useState<boolean>(true);
  const [periodDate, setPeriodDate] = useState<{
    startDate: string;
    endDate: string;
    dateField: string;
  }>({
    startDate: moment().subtract('7', 'days').format('YYYY-MM-DD'),
    endDate: moment().format('YYYY-MM-DD'),
    dateField: dateFieldMap[DateRangeType.DAY],
  });

  const getMetricTrendData = async (params: any = { download: false }) => {
    const { download, dimensionGroup, dimensionFilters } = params;
    if (download) {
      setDownloadLoding(true);
    } else {
      setMetricTrendLoading(true);
    }

    const { modelId, bizName, name } = nodeData;
    indicatorFields.current = [{ name, column: bizName }];
    const res = await queryStruct({
      modelId,
      bizName,
      groups: dimensionGroup,
      dimensionFilters,
      dateField: periodDate.dateField,
      startDate: periodDate.startDate,
      endDate: periodDate.endDate,
      download,
    });
    if (download) {
      setDownloadLoding(false);
      return;
    }
    const { code, data, msg } = res;
    setMetricTrendLoading(false);
    if (code === 200) {
      const { resultList, columns, queryAuthorization } = data;
      setMetricTrendData(resultList);
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
      setDownloadBtnDisabledState(false);
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

  const queryDimensionList = async (modelId: number) => {
    const { code, data, msg } = await getDimensionList({ modelId });
    if (code === 200 && Array.isArray(data?.list)) {
      return data.list;
    }
    message.error(msg);
    return [];
  };

  const queryDrillDownDimension = async (metricId: number) => {
    const { code, data, msg } = await getDrillDownDimension(metricId);
    if (code === 200 && Array.isArray(data)) {
      return data;
    }
    message.error(msg);
    return [];
  };

  const initDimensionData = async (metricItem: ISemantic.IMetricItem) => {
    const dimensionList = await queryDimensionList(metricItem.modelId);
    const drillDownDimension = await queryDrillDownDimension(metricItem.id);
    const drillDownDimensionIds = drillDownDimension.map(
      (item: ISemantic.IDrillDownDimensionItem) => item.dimensionId,
    );
    const drillDownDimensionList = dimensionList.filter((metricItem: ISemantic.IMetricItem) => {
      return drillDownDimensionIds.includes(metricItem.id);
    });
    setRelationDimensionOptions(
      drillDownDimensionList.map((item: ISemantic.IMetricItem) => {
        return { label: item.name, value: item.bizName };
      }),
    );
  };

  useEffect(() => {
    if (nodeData?.id && nodeData?.nodeType === SemanticNodeType.METRIC) {
      getMetricTrendData();
      initDimensionData(nodeData);
    }
  }, [nodeData, periodDate]);

  return (
    <>
      <div style={{ marginBottom: 5, display: 'grid', gap: 10 }}>
        {/* <StandardFormRow key="showType" title="维度下钻" block>
          <FormItem name="showType" valuePropName="checked">
            <Select
              style={{ minWidth: 150 }}
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
                getMetricTrendData({ ...params });
              }}
            />
          </FormItem>
        </StandardFormRow> */}
        {/* <Row>
          <Col flex="1 1 200px">
            <Space>
              <span>维度下钻: </span>
              <Select
                style={{ minWidth: 150 }}
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
                  getMetricTrendData({ ...params });
                }}
              />
            </Space>
          </Col>
        </Row>
        <Row>
          <Col flex="1 1 200px">
            <Space>
              <span>维度筛选: </span>
              <MetricTrendDimensionFilter
                modelId={nodeData.modelId}
                dimensionOptions={relationDimensionOptions}
                onFiltersChange={() => {}}
              />
            </Space>
          </Col>
        </Row> */}
        <Row>
          <Col flex="1 1 200px">
            <MDatePicker
              initialValues={{
                dateSettingType: 'DYNAMIC',
                dynamicParams: {
                  number: 7,
                  periodType: 'DAYS',
                  includesCurrentPeriod: true,
                  shortCutId: 'last7Days',
                  dateRangeType: 'DAY',
                  dynamicAdvancedConfigType: 'last',
                  dateRangeStringDesc: '最近7天',
                  dateSettingType: DateSettingType.DYNAMIC,
                },
                staticParams: {},
              }}
              onDateRangeChange={(value, config) => {
                const [startDate, endDate] = value;
                const { dateSettingType, dynamicParams, staticParams } = config;
                let dateField = dateFieldMap[DateRangeType.DAY];
                if (DateSettingType.DYNAMIC === dateSettingType) {
                  dateField = dateFieldMap[dynamicParams.dateRangeType];
                }
                if (DateSettingType.STATIC === dateSettingType) {
                  dateField = dateFieldMap[staticParams.dateRangeType];
                }
                setPeriodDate({ startDate, endDate, dateField });
              }}
              disabledAdvanceSetting={true}
            />
            {/* <Select
                style={{ minWidth: 150 }}
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
                  getMetricTrendData({ ...params });
                }}
              />
              <Select
                style={{ minWidth: 150 }}
                options={relationDimensionOptions}
                showSearch
                filterOption={(input, option) =>
                  ((option?.label ?? '') as string).toLowerCase().includes(input.toLowerCase())
                }
                mode="multiple"
                placeholder="请选择筛选维度"
                onChange={(value) => {
                  const params = { ...queryParams, dimensionFilters: value || [] };
                  setQueryParams(params);
                  getMetricTrendData({ ...params });
                }}
              /> */}
          </Col>
          <Col flex="0 1">
            <Button
              type="primary"
              loading={downloadLoding}
              disabled={downloadBtnDisabledState}
              onClick={() => {
                getMetricTrendData({ download: true, ...queryParams });
              }}
            >
              下载
            </Button>
          </Col>
        </Row>
      </div>
      {authMessage && <div style={{ color: '#d46b08', marginBottom: 15 }}>{authMessage}</div>}
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
        fields={indicatorFields.current}
        loading={metricTrendLoading}
        dateFieldName={periodDate.dateField}
        height={350}
        renderType="clear"
        decimalPlaces={metricColumnConfig?.dataFormat?.decimalPlaces || 2}
      />
    </>
  );
};

export default MetricTrendSection;
