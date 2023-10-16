import React, { useState, useEffect } from 'react';
import { Space, DatePicker } from 'antd';
import { DateMode, DateRangeType, DateRangePicker, DateRangeTypeToPickerMap } from './type';
import { getDateStrings } from './utils';
import { DateSettingType, StaticDateSelectMode } from './type';
import moment from 'moment';

const { RangePicker } = DatePicker;

type Props = {
  initialValues: any;
  currentDateSettingType?: DateSettingType;
  selectMode?: StaticDateSelectMode;
  dateRangeTypeProps?: DateRangeType;
  onDateRangeChange: (value: string[], config: any) => void;
};

const StaticDate: React.FC<Props> = ({
  initialValues,
  dateRangeTypeProps,
  currentDateSettingType = DateSettingType.STATIC,
  onDateRangeChange,
}: any) => {
  const [latestDateMap, setLatestDateMap] = useState<any>({
    maxPartition: moment().format('YYYY-MM-DD'),
  });

  const [staticFormData, setStaticFormData] = useState<any>(() => {
    return {
      dateSettingType: DateSettingType.STATIC,
      dateMode: initialValues?.dateMode || DateMode.RANGE,
      dateRangeType: initialValues?.dateRangeType || dateRangeTypeProps || DateRangeType.DAY,
      dateRange: initialValues?.dateRange || [],
      dateMultiple: initialValues?.dateMultiple || [],
    };
  });
  const [dateRangeValue, setDateRangeValue] = useState<any>([]);
  const [pickerType, setPickerType] = useState<PickerType>(() => {
    if (dateRangeTypeProps) {
      return DateRangeTypeToPickerMap[dateRangeTypeProps];
    }
    if (staticFormData.dateRangeType) {
      return DateRangeTypeToPickerMap[staticFormData.dateRangeType];
    }
    return DateRangePicker.DATE;
  });

  useEffect(() => {
    initDateRangeValue();
  }, []);

  useEffect(() => {
    if (currentDateSettingType === DateSettingType.STATIC) {
      handleDateRangeTypePropsChange(dateRangeTypeProps);
    }
    setPickerType(DateRangeTypeToPickerMap[dateRangeTypeProps]);
  }, [dateRangeTypeProps, latestDateMap]);

  const handleDateRangeTypePropsChange = async (dateRangeType: DateRangeType) => {
    if (!dateRangeType) {
      return;
    }
    setStaticFormData({
      ...staticFormData,
      dateRangeType,
    });
    dateRangeChange(dateRangeValue, dateRangeType, staticFormData.dateMode);
  };
  const initDateRangeValue = () => {
    const initDateRange = initialValues?.dateRange || [];
    const [startDate, endDate] = initDateRange;
    const { maxPartition } = latestDateMap;
    let dateRangeMoment = [moment(), moment()];
    // 如果initialValues时间存在则按initialValues时间初始化
    if (startDate && endDate) {
      dateRangeMoment = [moment(startDate), moment(endDate)];
    }
    // dateRangeValue未被初始化且maxPartition存在，则按maxPartition初始化
    if (dateRangeValue.length === 0 && !(startDate && endDate) && maxPartition) {
      dateRangeMoment = [moment(maxPartition), moment(maxPartition)];
    }
    // 否则按当前时间初始化
    setDateRangeValue(dateRangeMoment);
  };

  const updateStaticFormData = (formData: any) => {
    const mergeConfigTypeData = {
      ...staticFormData,
      ...formData,
      dateRangeStringDesc: '',
    };
    const { dateRange, dateMode } = mergeConfigTypeData;
    if (dateMode === DateMode.RANGE) {
      const [startDate, endDate] = dateRange;
      if (startDate && endDate) {
        mergeConfigTypeData.dateRangeStringDesc = `${startDate}至${endDate}`;
        mergeConfigTypeData.dateMultiple = [];
      }
    }
    if (dateMode === DateMode.LIST) {
      mergeConfigTypeData.dateRangeStringDesc = `日期多选`;
      mergeConfigTypeData.dateRange = [];
    }
    setStaticFormData(mergeConfigTypeData);
    onDateRangeChange(mergeConfigTypeData.dateRange, mergeConfigTypeData);
  };

  const dateRangeChange = (
    dates: any,
    dateRangeType: DateRangeType,
    dateMode: DateMode,
    isDateRangeChange?: boolean,
  ) => {
    if (!dates) {
      updateStaticFormData({ dateRange: [] });
      return;
    }
    const dateStrings = getDateStrings({
      dates,
      dateRangeType,
      latestDateMap,
      isDateRangeChange,
    });
    if (dateStrings[0] && dateStrings[1]) {
      setDateRangeValue([moment(dateStrings[0]), moment(dateStrings[1])]);
      updateStaticFormData({
        dateMode,
        dateRangeType,
        dateRange: dateStrings,
      });
    }
  };
  return (
    <Space>
      <RangePicker
        style={{ paddingBottom: 5 }}
        value={dateRangeValue}
        onChange={(date) => {
          setDateRangeValue(date);
          const dateString = getDateStrings({
            dates: date,
            latestDateMap,
            isDateRangeChange: true,
            dateRangeType: dateRangeTypeProps || staticFormData.dateRangeType,
          });
          updateStaticFormData({
            dateRange: dateString,
            dateRangeType: dateRangeTypeProps || staticFormData.dateRangeType,
          });
          return;
        }}
        allowClear={true}
        picker={pickerType}
      />
      {/* )} */}
    </Space>
  );
};

export default StaticDate;
