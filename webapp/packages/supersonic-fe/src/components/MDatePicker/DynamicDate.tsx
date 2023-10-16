import React, { useState, useEffect } from 'react';
import type { RadioChangeEvent } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { objToList } from '@/utils/utils';
// import { LatestDateMap } from '@/services/global/type';
import { DateRangeType, DateRangeTypeToPickerMap, DateRangePicker } from './type';
import {
  SHORT_CUT_ITEM_LIST,
  datePeriodTypeWordingMap,
  getDynamicDateRangeStringByParams,
  getDateStrings,
  datePeriodTypeMap,
  dateRangeTypeExchangeDatePeriodTypeMap,
} from './utils';
import {
  DynamicAdvancedConfigType,
  DatePeriodType,
  PerDatePeriodType,
  DateSettingType,
} from './type';

import {
  Typography,
  Space,
  Collapse,
  Tag,
  Row,
  Col,
  Radio,
  InputNumber,
  Select,
  Checkbox,
  Tooltip,
  DatePicker,
} from 'antd';

import moment from 'moment';
import styles from './style.less';

const { CheckableTag } = Tag;
const { Panel } = Collapse;
const { Link } = Typography;
const { Option } = Select;

type Props = {
  initialValues?: any;
  submitFormDataState?: boolean;
  dateRangeTypeProps?: DateRangeType;
  onDateRangeChange: (value: string[], config: any) => void;
  onAdvanceSettingCollapsedChange?: (collapse: boolean) => void;
  onShortCutClick?: (shortCutId: string) => void;
  onDateRangeStringAndDescChange?: ({
    dateRangeString,
    dateRangeStringDesc,
  }: {
    dateRangeString: string[];
    dateRangeStringDesc: string;
  }) => void;
  disabledAdvanceSetting?: boolean;
};

const DynamicDate: React.FC<Props> = ({
  initialValues,
  dateRangeTypeProps,
  submitFormDataState,
  onDateRangeChange,
  onAdvanceSettingCollapsedChange,
  onShortCutClick,
  onDateRangeStringAndDescChange,
  disabledAdvanceSetting = false,
}: any) => {
  const initAdvacedConfigPanelCollapsed = () => {
    return !initialValues?.dateSettingType || initialValues?.shortCutId ? [] : ['1'];
  };
  const [advancedPanelFormResetState, setAdvancedPanelFormResetState] =
    useState(dateRangeTypeProps);

  useEffect(() => {
    // 当时间粒度发生变化时重置高级设置面板
    resetAdvancedPanelForm(dateRangeTypeProps);
  }, [dateRangeTypeProps]);

  useEffect(() => {
    if (initialValues?.dateSettingType !== DateSettingType.DYNAMIC) {
      return;
    }
    initShortCutByDateRangeChange(dateRangeTypeProps);
  }, [advancedPanelFormResetState]);

  const resetAdvancedPanelForm = (dateRangeType: DateRangeType) => {
    const lastConfigTypeFormData = advancedPanelFormData[DynamicAdvancedConfigType.LAST];
    const historyConfigTypeFormData = advancedPanelFormData[DynamicAdvancedConfigType.HISTORY];
    switch (dateRangeType) {
      case DateRangeType.DAY:
        setAdvancedPanelFormData({
          ...advancedPanelFormData,
          [DynamicAdvancedConfigType.LAST]: {
            ...lastConfigTypeFormData,
            periodType: DatePeriodType.DAY,
          },
          [DynamicAdvancedConfigType.HISTORY]: {
            ...historyConfigTypeFormData,
            periodType: DatePeriodType.DAY,
          },
        });
        break;
      case DateRangeType.WEEK:
        setAdvancedPanelFormData({
          ...advancedPanelFormData,
          [DynamicAdvancedConfigType.LAST]: {
            ...lastConfigTypeFormData,
            periodType: DatePeriodType.WEEK,
          },
          [DynamicAdvancedConfigType.HISTORY]: {
            ...historyConfigTypeFormData,
            periodType: DatePeriodType.WEEK,
          },
        });
        break;
      case DateRangeType.MONTH:
        setAdvancedPanelFormData({
          ...advancedPanelFormData,
          [DynamicAdvancedConfigType.LAST]: {
            ...lastConfigTypeFormData,
            periodType: DatePeriodType.MONTH,
          },
          [DynamicAdvancedConfigType.HISTORY]: {
            ...historyConfigTypeFormData,
            periodType: DatePeriodType.MONTH,
          },
        });
        break;
      default:
        break;
    }
    setAdvancedPanelFormResetState(dateRangeType);
    setAdvacedConfigPanelCollapsed([]);
  };

  const initAdvancedConfigType = () => {
    return initialValues?.dynamicAdvancedConfigType;
  };
  const initAdvancedPanelFormData = () => {
    let defaultConfig = {
      [DynamicAdvancedConfigType.LAST]: {
        number: 1,
        periodType: dateRangeTypeExchangeDatePeriodTypeMap[dateRangeTypeProps],
        includesCurrentPeriod: false,
      },
      [DynamicAdvancedConfigType.HISTORY]: {
        number: 1,
        periodType: dateRangeTypeExchangeDatePeriodTypeMap[dateRangeTypeProps],
      },
      [DynamicAdvancedConfigType.FROM_DATE_PERIOD]: {
        perPeriodType: PerDatePeriodType.PERDAY,
      },
      [DynamicAdvancedConfigType.FROM_DATE]: { date: moment() },
    };
    if (advancedPanelFormData) {
      defaultConfig = { ...advancedPanelFormData };
    }
    if (initialValues?.dynamicAdvancedConfigType) {
      const { dynamicAdvancedConfigType } = initialValues;
      const targetConfig = defaultConfig[dynamicAdvancedConfigType];
      if (!targetConfig) {
        return defaultConfig;
      }
      const mergeConfig = Object.keys(targetConfig).reduce((result, key) => {
        return {
          ...result,
          [key]: initialValues[key],
        };
      }, {});
      defaultConfig[dynamicAdvancedConfigType] = mergeConfig;
    }
    return defaultConfig;
  };
  const initShortCutSettingChecked = () => {
    return initialValues?.shortCutId || '';
  };
  const [advacedConfigPanelCollapsed, setAdvacedConfigPanelCollapsed] = useState<string | string[]>(
    initAdvacedConfigPanelCollapsed(),
  );
  const [advancedConfigType, setAdvancedConfigType] = useState<
    DynamicAdvancedConfigType | undefined
  >(initAdvancedConfigType());
  const [advancedPanelFormData, setAdvancedPanelFormData] = useState<any>(
    initAdvancedPanelFormData(),
  );
  const [shortCutSettingChecked, setShortCutSettingChecked] = useState<string>(
    initShortCutSettingChecked(),
  );

  useEffect(() => {
    // 外部状态触发表单数据提交
    if (submitFormDataState && advancedConfigType) {
      updateAdvancedPanelFormData(
        advancedPanelFormData[advancedConfigType],
        advancedConfigType,
        true,
      );
    }
  }, [submitFormDataState]);

  const init = () => {
    setAdvacedConfigPanelCollapsed(initAdvacedConfigPanelCollapsed());
    setAdvancedConfigType(initAdvancedConfigType());
    setAdvancedPanelFormData(initAdvancedPanelFormData());
    setShortCutSettingChecked(initShortCutSettingChecked());
  };

  useEffect(() => {
    if (initialValues?.dateSettingType === DateSettingType.DYNAMIC) {
      init();
    }
  }, [initialValues]);

  const handleDateRangeChange = (dateRange: string[], config: any) => {
    onDateRangeChange(dateRange, {
      ...config,
      dateSettingType: DateSettingType.DYNAMIC,
    });
  };

  const updateAdvancedPanelFormData = (
    formData: any,
    configType: DynamicAdvancedConfigType,
    emitImmediately = false,
  ) => {
    const mergeConfigTypeData = {
      ...advancedPanelFormData[configType],
      ...formData,
      // shortCutId: shortCutSettingChecked,
      dateRangeType: dateRangeTypeProps,
      dynamicAdvancedConfigType: configType,
    };
    const { dateRangeString, dateRangeStringDesc } = getDynamicDateRangeStringByParams(
      mergeConfigTypeData,
      configType,
      { maxPartition: moment().format('YYYY-MM-DD') },
    );
    mergeConfigTypeData.dateRangeStringDesc = dateRangeStringDesc;
    onDateRangeStringAndDescChange?.({ dateRangeString, dateRangeStringDesc });
    if (emitImmediately) {
      handleDateRangeChange(dateRangeString, mergeConfigTypeData);
    }
    setAdvancedPanelFormData({
      ...advancedPanelFormData,
      [configType]: mergeConfigTypeData,
    });
  };

  // 根据当前时间粒度判断高级设置中时间区间选项哪些可用
  const isDisabledDatePeriodTypeOption = (
    datePeriodType: DatePeriodType,
    dateRangeType: DateRangeType,
  ) => {
    switch (datePeriodType) {
      case DatePeriodType.DAY:
        return ![DateRangeType.DAY].includes(dateRangeType);
      case DatePeriodType.WEEK:
        return ![DateRangeType.DAY, DateRangeType.WEEK].includes(dateRangeType);
      case DatePeriodType.MONTH:
        return false;
      case DatePeriodType.YEAR:
        return false;
      default:
        return false;
    }
  };

  const getDatePeriodTypeOptions = (dateRangeType: DateRangeType) => {
    const list = objToList(datePeriodTypeMap);
    const optionList = list.reduce((result: any[], { value, label }: any) => {
      const isDisabled = isDisabledDatePeriodTypeOption(value, dateRangeType);
      if (isDisabled) {
        return result;
      }
      result.push(
        <Option value={value} key={value}>
          {label}
        </Option>,
      );
      return result;
    }, []);
    return optionList;
  };

  const isAdvancedConfigTypeRadioDisabled = (type: DynamicAdvancedConfigType) => {
    return type !== advancedConfigType;
  };

  const initShortCutByDateRangeChange = (dateRangeType: DateRangeType, emitImmediately = false) => {
    const shortCutList = SHORT_CUT_ITEM_LIST.filter((item) => {
      return item.dateRangeType === dateRangeType;
    });
    const firstItem = shortCutList[0];
    if (firstItem) {
      handleShortCutChange(firstItem, emitImmediately);
    }
  };

  const handleShortCutChange = (item: any, emitImmediately = true) => {
    const { id, advancedConfigType, initData } = item;
    // 设置选中状态
    setShortCutSettingChecked(id);
    // 设置快捷选项AdvancedConfigType类型
    setAdvancedConfigType(advancedConfigType);
    // 更新数据至表单数据并立即向上层组件传递
    updateAdvancedPanelFormData(initData, advancedConfigType, emitImmediately);
    if (emitImmediately) {
      // 触发快捷选项点击时间，上层组件关闭配置浮窗
      onShortCutClick?.(id);
    }
  };

  const handleAdvancedPanelFormChange = () => {
    // 当高级面板表单发生变化时，重置快捷选项为空
    setShortCutSettingChecked('');
  };

  return (
    <>
      <div className={styles.dateShortCutSettingContent}>
        <Row>
          {SHORT_CUT_ITEM_LIST.map((item) => {
            const { id, text, dateRangeType } = item;
            if (dateRangeType === dateRangeTypeProps) {
              return (
                <Col key={`row-col-${id}`}>
                  <CheckableTag
                    className={styles['ant-tag-checkable']}
                    checked={shortCutSettingChecked === id}
                    key={`row-col-tag-${id}`}
                    onChange={() => {
                      handleShortCutChange(item);
                    }}
                  >
                    <div className={styles['tag-value-box']}>{text}</div>
                  </CheckableTag>
                </Col>
              );
            }
            return undefined;
          })}
        </Row>
      </div>
      {!disabledAdvanceSetting && (
        <div className={styles.dateAdvancedSettingContent}>
          <Collapse
            // defaultActiveKey={['1']}
            activeKey={advacedConfigPanelCollapsed}
            onChange={(key: string | string[]) => {
              setAdvacedConfigPanelCollapsed(key);
              if (key.length === 0) {
                onAdvanceSettingCollapsedChange?.(false);
                return;
              }
              onAdvanceSettingCollapsedChange?.(true);
            }}
            bordered={false}
            ghost={true}
            expandIconPosition="right"
          >
            <Panel
              header=""
              // collapsible={'disabled'}
              key="1"
              extra={
                <Space>
                  <Link>高级设置</Link>
                </Space>
              }
            >
              <div>
                <Space>
                  <div style={{ color: 'rgba(0, 0, 0, 0.85)' }}>动态时间</div>
                  <Tooltip
                    title={`日期随着时间推移而更新。 若在1月1日设置查询日期为“今天”， 则第二天的查询日期为1月2日。`}
                  >
                    <QuestionCircleOutlined />
                  </Tooltip>
                </Space>
              </div>
              <Radio.Group
                onChange={(e: RadioChangeEvent) => {
                  const configType = e.target.value;
                  setAdvancedConfigType(configType);
                  updateAdvancedPanelFormData(advancedPanelFormData[configType], configType);
                  handleAdvancedPanelFormChange();
                }}
                value={advancedConfigType}
              >
                <Space direction="vertical">
                  <Radio value={DynamicAdvancedConfigType.LAST}>
                    <Space size={10}>
                      <span className={styles.advancedSettingItemText}>最近</span>
                      <InputNumber
                        style={{ width: 120 }}
                        placeholder="请输入数字"
                        min={1}
                        disabled={isAdvancedConfigTypeRadioDisabled(DynamicAdvancedConfigType.LAST)}
                        value={advancedPanelFormData[DynamicAdvancedConfigType.LAST].number}
                        onChange={(value: number | null) => {
                          updateAdvancedPanelFormData(
                            { number: value },
                            DynamicAdvancedConfigType.LAST,
                          );
                          handleAdvancedPanelFormChange();
                        }}
                      />
                      <Select
                        // defaultValue={DatePeriodType.DAY}
                        style={{ width: 120 }}
                        disabled={isAdvancedConfigTypeRadioDisabled(DynamicAdvancedConfigType.LAST)}
                        value={advancedPanelFormData[DynamicAdvancedConfigType.LAST].periodType}
                        onClick={(e) => {
                          // 禁止冒泡触发Radio点击后续逻辑
                          e.preventDefault();
                          e.stopPropagation();
                        }}
                        onChange={(value: string) => {
                          updateAdvancedPanelFormData(
                            { periodType: value },
                            DynamicAdvancedConfigType.LAST,
                          );
                          handleAdvancedPanelFormChange();
                        }}
                      >
                        {getDatePeriodTypeOptions(dateRangeTypeProps)}
                      </Select>
                      <Checkbox
                        disabled={isAdvancedConfigTypeRadioDisabled(DynamicAdvancedConfigType.LAST)}
                        checked={
                          advancedPanelFormData[DynamicAdvancedConfigType.LAST]
                            .includesCurrentPeriod
                        }
                        onChange={(e) => {
                          const isChecked = e.target.checked;
                          updateAdvancedPanelFormData(
                            { includesCurrentPeriod: isChecked },
                            DynamicAdvancedConfigType.LAST,
                          );
                          handleAdvancedPanelFormChange();
                        }}
                      >
                        包含
                        {
                          datePeriodTypeWordingMap[
                            advancedPanelFormData[DynamicAdvancedConfigType.LAST].periodType
                          ]
                        }
                      </Checkbox>
                    </Space>
                  </Radio>
                  <Radio value={DynamicAdvancedConfigType.HISTORY}>
                    <Space size={10}>
                      <span className={styles.advancedSettingItemText}>过去第</span>

                      <InputNumber
                        style={{ width: 120 }}
                        placeholder="请输入数字"
                        min={1}
                        disabled={isAdvancedConfigTypeRadioDisabled(
                          DynamicAdvancedConfigType.HISTORY,
                        )}
                        value={advancedPanelFormData[DynamicAdvancedConfigType.HISTORY].number}
                        // defaultValue={3}
                        onChange={(value: number | null) => {
                          updateAdvancedPanelFormData(
                            { number: value },
                            DynamicAdvancedConfigType.HISTORY,
                          );
                          handleAdvancedPanelFormChange();
                        }}
                      />
                      <Select
                        // defaultValue={DatePeriodType.DAY}
                        style={{ width: 120 }}
                        disabled={isAdvancedConfigTypeRadioDisabled(
                          DynamicAdvancedConfigType.HISTORY,
                        )}
                        value={advancedPanelFormData[DynamicAdvancedConfigType.HISTORY].periodType}
                        onClick={(e) => {
                          // 禁止冒泡触发Radio点击后续逻辑
                          e.preventDefault();
                          e.stopPropagation();
                        }}
                        onChange={(value: string) => {
                          updateAdvancedPanelFormData(
                            { periodType: value },
                            DynamicAdvancedConfigType.HISTORY,
                          );
                          handleAdvancedPanelFormChange();
                        }}
                      >
                        {getDatePeriodTypeOptions(dateRangeTypeProps)}
                      </Select>
                    </Space>
                  </Radio>
                  <Radio value={DynamicAdvancedConfigType.FROM_DATE}>
                    <Space size={10}>
                      <span className={styles.advancedSettingItemText}>自从</span>
                      <DatePicker
                        disabled={isAdvancedConfigTypeRadioDisabled(
                          DynamicAdvancedConfigType.FROM_DATE,
                        )}
                        value={moment(
                          advancedPanelFormData[DynamicAdvancedConfigType.FROM_DATE].date,
                        )}
                        disabledDate={(current) => {
                          return current && current > moment().endOf('day');
                        }}
                        picker={DateRangeTypeToPickerMap[dateRangeTypeProps]}
                        onChange={(date, dateString) => {
                          if (!date) {
                            return;
                          }
                          const picker = DateRangeTypeToPickerMap[dateRangeTypeProps];

                          if (picker === DateRangePicker.WEEK) {
                            date.startOf('week').format('YYYY-MM-DD');
                          }
                          if (picker === DateRangePicker.MONTH) {
                            date.startOf('month').format('YYYY-MM-DD');
                          }
                          updateAdvancedPanelFormData(
                            { date },
                            DynamicAdvancedConfigType.FROM_DATE,
                          );
                          handleAdvancedPanelFormChange();
                        }}
                      />
                      至此刻
                    </Space>
                  </Radio>
                </Space>
              </Radio.Group>
            </Panel>
          </Collapse>
        </div>
      )}
    </>
  );
};

export default DynamicDate;
