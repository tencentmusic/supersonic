import { useEffect, forwardRef } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { Form, Input, Select, InputNumber } from 'antd';
import { ISemantic } from '../../data';
import { ChatConfigType, DetailTypeDefaultConfig, TimeModeEnum, DatePeriod } from '../../enum';
// import TransTypeTag from '../../components/TransTypeTag';

type Props = {
  chatConfigType: ChatConfigType.TAG | ChatConfigType.METRIC;
  metricList?: ISemantic.IMetricItem[];
  dimensionList?: ISemantic.IDimensionItem[];
  form: any;
  formData: Record<string, any>;
};

const FormItem = Form.Item;
const Option = Select.Option;

const formDefaultValue = {
  unit: 7,
  period: 'DAY',
  timeMode: 'LAST',
};

const DefaultSettingForm: ForwardRefRenderFunction<any, Props> = (
  { metricList, dimensionList, formData, chatConfigType, form },
  ref,
) => {
  // const [dataItemListOptions, setDataItemListOptions] = useState<any>([]);

  const initData = () => {
    form.setFieldsValue({
      queryConfig: {
        [DetailTypeDefaultConfig[chatConfigType]]: {
          timeDefaultConfig: {
            ...formDefaultValue,
          },
        },
      },
    });
  };

  useEffect(() => {
    if (form && !form.getFieldValue('id')) {
      initData();
    }
  }, []);

  // useEffect(() => {
  //   if (Array.isArray(dimensionList) && Array.isArray(metricList)) {
  // const dimensionEnum = dimensionList.map((item: ISemantic.IDimensionItem) => {
  //   const { name, id, bizName } = item;
  //   return {
  //     name,
  //     label: (
  //       <>
  //         <TransTypeTag type={SemanticNodeType.DIMENSION} />
  //         {name}
  //       </>
  //     ),
  //     value: wrapperTransTypeAndId(TransType.DIMENSION, id),
  //     bizName,
  //     id,
  //     transType: TransType.DIMENSION,
  //   };
  // });
  // const metricEnum = metricList.map((item: ISemantic.IMetricItem) => {
  //   const { name, id, bizName } = item;
  //   return {
  //     name,
  //     label: (
  //       <>
  //         <TransTypeTag type={SemanticNodeType.METRIC} />
  //         {name}
  //       </>
  //     ),
  //     value: wrapperTransTypeAndId(TransType.METRIC, id),
  //     bizName,
  //     id,
  //     transType: TransType.METRIC,
  //   };
  // });
  // setDataItemListOptions([...dimensionEnum, ...metricEnum]);
  //   }
  // }, [dimensionList, metricList]);

  return (
    <>
      {/* {chatConfigType === ChatConfigType.TAG && (
        <FormItem
          name={['queryConfig', DetailTypeDefaultConfig[ChatConfigType.TAG], 'defaultDisplayInfo']}
          label="明细查询结果展示字段"
          getValueFromEvent={(value, items) => {
            const result: { dimensionIds: number[]; metricIds: number[] } = {
              dimensionIds: [],
              metricIds: [],
            };
            items.forEach((item: any) => {
              if (item.transType === TransType.DIMENSION) {
                result.dimensionIds.push(item.id);
              }
              if (item.transType === TransType.METRIC) {
                result.metricIds.push(item.id);
              }
            });
            return result;
          }}
          getValueProps={(value) => {
            const { dimensionIds, metricIds } = value || {};
            const dimensionValues = Array.isArray(dimensionIds)
              ? dimensionIds.map((id: number) => {
                  return wrapperTransTypeAndId(TransType.DIMENSION, id);
                })
              : [];
            const metricValues = Array.isArray(metricIds)
              ? metricIds.map((id: number) => {
                  return wrapperTransTypeAndId(TransType.METRIC, id);
                })
              : [];
            return {
              value: [...dimensionValues, ...metricValues],
            };
          }}
        >
          <Select
            mode="multiple"
            allowClear
            style={{ width: '100%' }}
            optionLabelProp="name"
            filterOption={(inputValue: string, item: any) => {
              const { name } = item;
              if (name.includes(inputValue)) {
                return true;
              }
              return false;
            }}
            placeholder="请选择明细查询结果展示字段"
            options={dataItemListOptions}
          />
        </FormItem>
      )} */}
      <FormItem
        label={
          <FormItemTitle
            title={'时间范围'}
            subTitle={'问答搜索结果选择中，如果没有指定时间范围，将会采用默认时间范围'}
          />
        }
      >
        <Input.Group compact>
          {chatConfigType === ChatConfigType.TAG ? (
            <span
              style={{
                display: 'inline-block',
                lineHeight: '32px',
                marginRight: '8px',
              }}
            >
              前
            </span>
          ) : (
            <>
              <FormItem
                name={[
                  'queryConfig',
                  DetailTypeDefaultConfig[chatConfigType],
                  'timeDefaultConfig',
                  'timeMode',
                ]}
                noStyle
              >
                <Select style={{ width: '90px' }}>
                  <Option value={TimeModeEnum.LAST}>前</Option>
                  <Option value={TimeModeEnum.RECENT}>最近</Option>
                  <Option value={TimeModeEnum.CURRENT}>本</Option>
                </Select>
              </FormItem>
            </>
          )}
          {formData?.queryConfig?.[DetailTypeDefaultConfig[chatConfigType]]?.timeDefaultConfig
            ?.timeMode !== 'CURRENT' && (
            <FormItem
              name={[
                'queryConfig',
                DetailTypeDefaultConfig[chatConfigType],
                'timeDefaultConfig',
                'unit',
              ]}
              noStyle
            >
              <InputNumber style={{ width: '120px' }} />
            </FormItem>
          )}
          {/* {formData?.queryConfig?.[DetailTypeDefaultConfig[chatConfigType]]?.timeDefaultConfig
            ?.timeMode !== 'CURRENT' ? (
            <FormItem
              key="notCurrent"
              name={[
                'queryConfig',
                DetailTypeDefaultConfig[chatConfigType],
                'timeDefaultConfig',
                'period',
              ]}
              noStyle
            >
              <Select style={{ width: '90px' }}>
                <Option value={DatePeriod.DAY}>日</Option>
                <Option value={DatePeriod.WEEK}>周</Option>
                <Option value={DatePeriod.MONTH}>月</Option>
                <Option value={DatePeriod.YEAR}>年</Option>
              </Select>
            </FormItem>
          ) : (
            <FormItem
              key="isCurrent"
              name={[
                'queryConfig',
                DetailTypeDefaultConfig[chatConfigType],
                'timeDefaultConfig',
                'period',
              ]}
              noStyle
            >
              <Select style={{ width: '90px' }} defaultValue={DatePeriod.MONTH}>
                <Option value={DatePeriod.MONTH}>月</Option>
                <Option value={DatePeriod.YEAR}>年</Option>
              </Select>
            </FormItem>
          )} */}
          <FormItem
            name={[
              'queryConfig',
              DetailTypeDefaultConfig[chatConfigType],
              'timeDefaultConfig',
              'period',
            ]}
            noStyle
          >
            <Select style={{ width: '90px' }}>
              {formData?.queryConfig?.[DetailTypeDefaultConfig[chatConfigType]]?.timeDefaultConfig
                ?.timeMode !== 'CURRENT' && (
                <>
                  <Option value={DatePeriod.DAY}>日</Option>
                  <Option value={DatePeriod.WEEK}>周</Option>
                </>
              )}
              <Option value={DatePeriod.MONTH}>月</Option>
              <Option value={DatePeriod.YEAR}>年</Option>
            </Select>
          </FormItem>
        </Input.Group>
      </FormItem>
      <FormItem
        name={['queryConfig', DetailTypeDefaultConfig[chatConfigType], 'limit']}
        label={<FormItemTitle title={'查询Limit'} subTitle={'设置默认查询结果的限制行数'} />}
      >
        <InputNumber style={{ width: '120px' }} />
      </FormItem>
    </>
  );
};

export default forwardRef(DefaultSettingForm);
