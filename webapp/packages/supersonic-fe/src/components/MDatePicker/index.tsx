import React, { useState, useEffect } from 'react';
import { InfoCircleOutlined, CalendarOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { Input, Tooltip, Popover, Space, Button, Select, Row, Col, Tag } from 'antd';
import styles from './style.less';
import { DateMode, DateRangeType } from './type';
import {
  LATEST_TEXT,
  DATE_RANGE_TYPE_ITEM_LIST,
  getWeekDateRangeString,
  getMonthDateRangeString,
  generatorDateRangesParams,
  getDynamicDateRangeStringByParams,
} from './utils';
import { DateSettingType, DateRangeParams, DynamicAdvancedConfigType } from './type';
import { LatestDateMap } from './type';
import StaticDate from './StaticDate';
import DynamicDate from './DynamicDate';
import moment from 'moment';
import { ProCard } from '@ant-design/pro-card';

type Props = {
  disabledAdvanceSetting?: boolean;
  initialValues?: any;
  showCurrentDataRangeString?: boolean;
  onDateRangeChange: (value: string[], from: any) => void;
  onDateRangeTypeChange?: (dateRangeType: DateRangeType) => void;
  onInit?: (params: { dateStringRange: string[] }) => void;
};

const { CheckableTag } = Tag;
const MDatePicker: React.FC<Props> = ({
  disabledAdvanceSetting,
  initialValues,
  showCurrentDataRangeString = true,
  onDateRangeChange,
  onDateRangeTypeChange,
  onInit,
}: any) => {
  const getDynamicDefaultConfig = (dateRangeType: DateRangeType) => {
    const dynamicDefaultConfig = {
      shortCutId: 'last7Days',
      dateRangeType: DateRangeType.DAY,
      dynamicAdvancedConfigType: DynamicAdvancedConfigType.LATEST,
      dateRangeStringDesc: LATEST_TEXT,
      number: 7,
      dateSettingType: DateSettingType.DYNAMIC,
    };
    switch (dateRangeType) {
      case DateRangeType.DAY:
        return dynamicDefaultConfig;
      case DateRangeType.WEEK:
        return {
          shortCutId: 'last4Weeks',
          dateRangeType: DateRangeType.WEEK,
          dynamicAdvancedConfigType: DynamicAdvancedConfigType.LAST,
          dateRangeStringDesc: '最近4周',
          dateSettingType: DateSettingType.DYNAMIC,
          includesCurrentPeriod: false,
          number: 4,
          periodType: 'WEEK',
        };
      case DateRangeType.MONTH:
        return {
          shortCutId: 'last6Months',
          dateRangeType: DateRangeType.MONTH,
          dynamicAdvancedConfigType: DynamicAdvancedConfigType.LAST,
          dateRangeStringDesc: '最近6月',
          includesCurrentPeriod: false,
          number: 6,
          periodType: 'MONTH',
          dateSettingType: DateSettingType.DYNAMIC,
        };
      default:
        return dynamicDefaultConfig;
    }
  };
  const [dateRangeType, setDateRangeType] = useState<DateRangeType>(
    initialValues?.dynamicParams?.dateRangeType ||
      initialValues?.staticParams?.dateRangeType ||
      DateRangeType.DAY,
  );

  const staticDefaultConfig = {
    dateSettingType: DateSettingType.STATIC,
    dateMode: DateMode.RANGE,
    dateRangeType: DateRangeType.DAY,
    dateRange: [],
    dateMultiple: [],
    dateRangeStringDesc: '',
  };

  const [latestDateMap, setLatestDateMap] = useState<LatestDateMap>({
    maxPartition: moment().format('YYYY-MM-DD'),
  });
  const [dateRangesParams] = useState(() => {
    return initialValues ? generatorDateRangesParams(initialValues) : {};
  });
  const [confirmBtnClickState, setConfirmBtnClickState] = useState(false);

  const [visible, setVisible] = useState(false);

  const [staticParams, setStaticParams] = useState(() => {
    return initialValues?.staticParams || {};
  });
  const [dynamicParams, setDynamicParams] = useState(() => {
    return initialValues?.dynamicParams || {};
  });

  const [currentDateMode, setCurrentDateMode] = useState<DateMode>(
    initialValues?.staticParams?.dateMode || DateMode.RANGE,
  );

  const [currentDateSettingType, setCurrentDateSettingType] = useState(
    initialValues?.dateSettingType || DateSettingType.STATIC,
  );
  const [currentDateRange, setCurrentDateRange] = useState<string[]>(() => {
    return dateRangesParams.dateRange || [];
  });
  const [selectedDateRangeString, setSelectedDateRangeString] = useState(() => {
    return getSelectedDateRangeString();
  });
  // const [advanceSettingCollapsed, setAdvanceSettingCollapsed] = useState(false);
  function getSelectedDateRangeString() {
    const [startTime, endTime] = currentDateRange;
    if (currentDateSettingType === DateSettingType.DYNAMIC) {
      if (startTime && endTime) {
        if (dateRangeType === DateRangeType.WEEK) {
          return getWeekDateRangeString(startTime, endTime);
        }
        if (dateRangeType === DateRangeType.MONTH) {
          return getMonthDateRangeString(startTime, endTime);
        }
        return `${startTime}至${endTime}`;
      }
      return '';
    }
    if (currentDateSettingType === DateSettingType.STATIC) {
      const { dateMode, dateMultiple } = staticParams;
      if (dateMode === DateMode.RANGE) {
        if (startTime && endTime) {
          if (dateRangeType === DateRangeType.WEEK) {
            return getWeekDateRangeString(startTime, endTime);
          }
          if (dateRangeType === DateRangeType.MONTH) {
            return getMonthDateRangeString(startTime, endTime);
          }
          return `${startTime} 至 ${endTime}`;
        }
        return '';
      }
      if (dateMode === DateMode.LIST) {
        return dateMultiple.join(',');
      }
    }
  }
  useEffect(() => {
    onInit?.({ dateRange: currentDateRange });
  }, []);

  useEffect(() => {
    setSelectedDateRangeString(getSelectedDateRangeString());
  }, [staticParams, dynamicParams, currentDateRange]);

  const handleDateModeChange = (dateMode: DateMode) => {
    if (!dateMode) {
      return;
    }
    setCurrentDateMode(dateMode);
    if (dateMode === DateMode.LIST) {
      setDateRangeType(DateRangeType.DAY);
    }
  };

  const handleDateRangeChange = (value: string[] | boolean, config: any) => {
    const { dateRangeStringDesc, dateSettingType, dateMode } = config;
    handleDateModeChange(dateMode);
    setDateRangeStringShow(dateRangeStringDesc);
    setCurrentDateSettingType(dateSettingType);
    if (Array.isArray(value)) {
      setCurrentDateRange(value);
    }
    let dateParamsConfig: DateRangeParams = {
      latestDateMap,
      dateSettingType,
      dynamicParams: {},
      staticParams: {},
    };
    if (dateSettingType === DateSettingType.DYNAMIC) {
      dateParamsConfig = {
        ...dateParamsConfig,
        dateSettingType,
        dynamicParams: config,
        staticParams: {},
      };
    }
    if (dateSettingType === DateSettingType.STATIC) {
      dateParamsConfig = {
        ...dateParamsConfig,
        dateSettingType,
        dynamicParams: {},
        staticParams: config,
      };
    }
    setDynamicParams({ ...dateParamsConfig.dynamicParams });
    setStaticParams({ ...dateParamsConfig.staticParams });
    onDateRangeChange(value, dateParamsConfig);
  };
  const getDateRangeStringShow = (dateRange: string[]) => {
    if (currentDateSettingType === DateSettingType.DYNAMIC) {
      const { dateRangeStringDesc } = getDynamicDateRangeStringByParams(
        dynamicParams,
        dynamicParams.dynamicAdvancedConfigType,
        latestDateMap,
      );
      return dateRangeStringDesc;
    }
    if (currentDateSettingType === DateSettingType.STATIC) {
      const { dateMode } = staticParams;
      const [startTime, endTime] = currentDateRange || [];
      if (dateMode === DateMode.RANGE) {
        if (startTime && endTime) {
          return `${startTime} 至 ${endTime}`;
        }
        return '';
      }
      if (dateMode === DateMode.LIST) {
        return '日期多选';
      }
    }

    const [startTime, endTime] = dateRange || [];
    if (startTime && endTime) {
      return `${startTime} 至 ${endTime}`;
    }
    return '';
  };
  const [dateRangeStringShow, setDateRangeStringShow] = useState(() => {
    return getDateRangeStringShow(dateRangesParams.dateRange);
  });

  const initDefaultDynamicData = ({ latestDateMap }: any) => {
    if (!initialValues) {
      const defaultConfig = getDynamicDefaultConfig(dateRangeType);
      const { maxPartition } = latestDateMap;
      const dateRange: string[] = [maxPartition, maxPartition];
      const config = {
        ...defaultConfig,
        dateRange,
      };
      handleDateRangeChange(dateRange, config);
    }
  };

  useEffect(() => {
    initDefaultDynamicData({ latestDateMap });
  }, []);

  useEffect(() => {
    if (visible) {
      setConfirmBtnClickState(false);
    }
  }, [visible]);

  useEffect(() => {
    const { dateRange } = dateRangesParams;
    setDateRangeStringShow(getDateRangeStringShow(dateRange));
  }, [dateRangesParams]);

  const content = (
    <>
      <ProCard
        className={styles.dateProCard}
        title={
          <Space>
            时间粒度
            {/* <Tooltip
              title={``}
            >
              <QuestionCircleOutlined />
            </Tooltip> */}
          </Space>
        }
      >
        <div className={styles.dateShortCutSettingContent}>
          <Row>
            {DATE_RANGE_TYPE_ITEM_LIST.map((item: any) => {
              const { value, label, toolTips } = item;
              if (currentDateMode === DateMode.LIST && value !== DateRangeType.DAY) {
                // 在多选模式只允许选择天粒度
                return undefined;
              }
              return (
                <Col key={`row-col-${value}`}>
                  <Tooltip title={toolTips}>
                    <CheckableTag
                      className={styles['ant-tag-checkable']}
                      checked={dateRangeType === value}
                      key={`row-col-tag-${value}`}
                      onChange={() => {
                        setDateRangeType(value);
                        onDateRangeTypeChange?.(value);
                      }}
                    >
                      <div className={styles['tag-value-box']}>{label}</div>
                    </CheckableTag>
                  </Tooltip>
                </Col>
              );
            })}
          </Row>
        </div>
      </ProCard>

      <ProCard
        className={styles.dateProCard}
        title={'快捷选项'}
        // title={`动态时间${
        //   currentDateSettingType === DateSettingType.DYNAMIC ? '(当前选中)' : ''
        // }`}
        // extra="2019年9月28日"
      >
        <DynamicDate
          disabledAdvanceSetting={disabledAdvanceSetting}
          initialValues={dynamicParams}
          dateRangeTypeProps={dateRangeType}
          submitFormDataState={confirmBtnClickState}
          onDateRangeChange={handleDateRangeChange}
          onDateRangeStringAndDescChange={({ dateRangeString }) => {
            setCurrentDateRange(dateRangeString);
          }}
          onShortCutClick={() => {
            setVisible(false);
          }}
        />
      </ProCard>

      <ProCard
        className={styles.dateProCard}
        title={
          <Space>
            静态时间
            {/* <Tooltip
              title={``}
            >
              <QuestionCircleOutlined />
            </Tooltip> */}
          </Space>
        }
      >
        <StaticDate
          currentDateSettingType={currentDateSettingType}
          initialValues={staticParams}
          dateRangeTypeProps={dateRangeType}
          onDateRangeChange={handleDateRangeChange}
        />
      </ProCard>

      <div
        style={{
          display: 'flex',
          borderTop: '1px solid #eee',
          paddingTop: '10px',
        }}
      >
        <Space style={{ fontSize: 12, marginRight: 20 }}>
          <div style={{ width: 60 }}>已选时间：</div>
          <div>{selectedDateRangeString}</div>
        </Space>

        <Space style={{ marginLeft: 'auto' }}>
          <Button
            type="primary"
            onClick={() => {
              if (currentDateSettingType === DateSettingType.DYNAMIC) {
                setConfirmBtnClickState(true);
              }
              setVisible(false);
            }}
          >
            确 认
          </Button>
          <Button
            onClick={() => {
              setVisible(false);
            }}
          >
            取 消
          </Button>
        </Space>
      </div>
    </>
  );
  return (
    <Space direction="vertical">
      <Popover
        content={content}
        // destroyTooltipOnHide={true}
        // title="Title"
        open={visible}
        trigger="click"
        onOpenChange={(newVisible) => {
          setVisible(newVisible);
          if (!newVisible) {
            // 当界面关闭时,如果是动态模式需检测用户所确认选中数据和当前面板显示数据（切换了时间粒度，但是没有保存配置数据）
            // 是否为同一时间粒度，如果不是，则需将当前时间粒度调整为动态时间组件所保存的时间粒度
            if (currentDateSettingType === DateSettingType.DYNAMIC) {
              const paramsDateRangeType = dynamicParams.dateRangeType;
              if (paramsDateRangeType && paramsDateRangeType !== dateRangeType) {
                setDateRangeType(paramsDateRangeType);
              }
            }
          }
        }}
        overlayClassName={styles.popverOverlayContent}
        placement="left"
      >
        <Input
          className={styles.dateTimeShowInput}
          value={dateRangeStringShow}
          placeholder="请选择日期时间"
          prefix={<CalendarOutlined />}
          readOnly
          style={{ width: 280 }}
          suffix={
            <Tooltip title={`${selectedDateRangeString}`}>
              <InfoCircleOutlined style={{}} />
            </Tooltip>
          }
        />
      </Popover>
      {showCurrentDataRangeString &&
        !(
          currentDateSettingType === DateSettingType.STATIC &&
          currentDateMode === DateMode.RANGE &&
          dateRangeType === DateRangeType.DAY
        ) && <div style={{ color: '#0e73ff' }}>当前时间: {selectedDateRangeString}</div>}
    </Space>
  );
};

export default MDatePicker;
