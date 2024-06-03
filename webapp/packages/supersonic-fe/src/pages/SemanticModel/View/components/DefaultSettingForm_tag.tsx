import { useEffect, useState, forwardRef, useImperativeHandle } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';

import { Form, Input, Select, InputNumber } from 'antd';

import { wrapperTransTypeAndId } from '../../utils';

import { ISemantic } from '../../data';
import { TransType, SemanticNodeType } from '../../enum';
import TransTypeTag from '../../components/TransTypeTag';

type Props = {
  chatConfigType: TransType;
  metricList?: ISemantic.IMetricItem[];
  dimensionList?: ISemantic.IDimensionItem[];
  tagList?: ISemantic.ITagItem[];
  form: any;
};

const FormItem = Form.Item;
const Option = Select.Option;

const formDefaultValue = {
  unit: 7,
  period: 'DAY',
  timeMode: 'LAST',
};

const DefaultSettingForm: ForwardRefRenderFunction<any, Props> = (
  { metricList, dimensionList, tagList, chatConfigType, form },
  ref,
) => {
  const [dataItemListOptions, setDataItemListOptions] = useState<any>([]);

  const initData = () => {
    form.setFieldsValue({
      queryConfig: {
        [defaultConfigKeyMap[chatConfigType]]: {
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

  const defaultConfigKeyMap: any = {
    [TransType.TAG]: 'tagTypeDefaultConfig',
    [TransType.METRIC]: 'metricTypeDefaultConfig',
  };

  useEffect(() => {
    if (Array.isArray(tagList)) {
      const tagEnum = tagList.map((item: ISemantic.ITagItem) => {
        const { name, id, bizName } = item;
        return {
          name,
          label: (
            <>
              <TransTypeTag type={SemanticNodeType.TAG} />
              {name}
            </>
          ),
          value: wrapperTransTypeAndId(TransType.TAG, id),
          bizName,
          id,
          transType: TransType.TAG,
        };
      });
      setDataItemListOptions([...tagEnum]);
    }
  }, [tagList]);

  return (
    <>
      {chatConfigType === TransType.TAG && (
        <FormItem
          name={['queryConfig', defaultConfigKeyMap[TransType.TAG], 'defaultDisplayInfo']}
          label="明细查询结果展示字段"
          getValueFromEvent={(value, items) => {
            const result: { tagIds: number[] } = {
              tagIds: [],
            };
            items.forEach((item: any) => {
              result.tagIds.push(item.id);
            });
            return result;
          }}
          getValueProps={(value) => {
            const { tagIds } = value || {};
            const tagValues = Array.isArray(tagIds)
              ? tagIds.map((id: number) => {
                  return wrapperTransTypeAndId(TransType.TAG, id);
                })
              : [];
            return {
              value: [...tagValues],
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
      )}
      <FormItem
        label={
          <FormItemTitle
            title={'时间范围'}
            subTitle={'问答搜索结果选择中，如果没有指定时间范围，将会采用默认时间范围'}
          />
        }
      >
        <Input.Group compact>
          {chatConfigType === TransType.TAG ? (
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
                  defaultConfigKeyMap[chatConfigType],
                  'timeDefaultConfig',
                  'timeMode',
                ]}
                noStyle
              >
                <Select style={{ width: '90px' }}>
                  <Option value="LAST">前</Option>
                  <Option value="RECENT">最近</Option>
                </Select>
              </FormItem>
            </>
          )}
          <FormItem
            name={['queryConfig', defaultConfigKeyMap[chatConfigType], 'timeDefaultConfig', 'unit']}
            noStyle
          >
            <InputNumber style={{ width: '120px' }} />
          </FormItem>
          <FormItem
            name={[
              'queryConfig',
              defaultConfigKeyMap[chatConfigType],
              'timeDefaultConfig',
              'period',
            ]}
            noStyle
          >
            <Select style={{ width: '90px' }}>
              <Option value="DAY">天</Option>
              <Option value="WEEK">周</Option>
              <Option value="MONTH">月</Option>
              <Option value="YEAR">年</Option>
            </Select>
          </FormItem>
        </Input.Group>
      </FormItem>
    </>
  );
};

export default forwardRef(DefaultSettingForm);
