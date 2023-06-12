import { useState, forwardRef } from 'react';
import type { ForwardRefRenderFunction } from 'react';
import { Form, Button } from 'antd';
import FormItemTitle from '@/components/FormHelper/FormItemTitle';
import { formLayout } from '@/components/FormHelper/utils';
import DimensionMetricVisibleModal from './DimensionMetricVisibleModal';
import DimensionSearchVisibleModal from './DimensionSearchVisibleModal';

type Props = {
  themeData: any;
  metricList: any[];
  dimensionList: any[];
  domainId: number;
  onSubmit: (params?: any) => void;
};

const FormItem = Form.Item;

const DimensionMetricVisibleForm: ForwardRefRenderFunction<any, Props> = ({
  domainId,
  metricList,
  dimensionList,
  themeData,
  onSubmit,
}) => {
  const [dimensionModalVisible, setDimensionModalVisible] = useState(false);
  const [dimensionSearchModalVisible, setDimensionSearchModalVisible] = useState(false);
  const [metricModalVisible, setMetricModalVisible] = useState<boolean>(false);
  return (
    <>
      <Form {...formLayout}>
        <FormItem
          label={
            <FormItemTitle title={'可见维度'} subTitle={'设置可见后，维度将允许在问答中被使用'} />
          }
        >
          <Button
            type="primary"
            onClick={() => {
              setDimensionModalVisible(true);
            }}
          >
            设 置
          </Button>
        </FormItem>
        <FormItem
          label={
            <FormItemTitle title={'可见指标'} subTitle={'设置可见后，指标将允许在问答中被使用'} />
          }
        >
          <Button
            type="primary"
            onClick={() => {
              setMetricModalVisible(true);
            }}
          >
            设 置
          </Button>
        </FormItem>
        <FormItem
          label={
            <FormItemTitle
              title={'可见维度值'}
              subTitle={'设置可见后，在可见维度设置的基础上，维度值将在搜索时可以被联想出来'}
            />
          }
        >
          <Button
            type="primary"
            onClick={() => {
              setDimensionSearchModalVisible(true);
            }}
          >
            设 置
          </Button>
        </FormItem>
      </Form>
      {dimensionModalVisible && (
        <DimensionMetricVisibleModal
          domainId={domainId}
          themeData={themeData}
          settingSourceList={dimensionList}
          settingType="dimension"
          visible={dimensionModalVisible}
          onCancel={() => {
            setDimensionModalVisible(false);
          }}
          onSubmit={() => {
            onSubmit?.();
            setDimensionModalVisible(false);
          }}
        />
      )}
      {dimensionSearchModalVisible && (
        <DimensionSearchVisibleModal
          domainId={domainId}
          settingSourceList={dimensionList.filter((item) => {
            const blackDimensionList = themeData.visibility?.blackDimIdList;
            if (Array.isArray(blackDimensionList)) {
              return !blackDimensionList.includes(item.id);
            }
            return false;
          })}
          themeData={themeData}
          visible={dimensionSearchModalVisible}
          onCancel={() => {
            setDimensionSearchModalVisible(false);
          }}
          onSubmit={() => {
            onSubmit?.({ from: 'dimensionSearchVisible' });
            setDimensionSearchModalVisible(false);
          }}
        />
      )}
      {metricModalVisible && (
        <DimensionMetricVisibleModal
          domainId={domainId}
          themeData={themeData}
          settingSourceList={metricList}
          settingType="metric"
          visible={metricModalVisible}
          onCancel={() => {
            setMetricModalVisible(false);
          }}
          onSubmit={() => {
            onSubmit?.();
            setMetricModalVisible(false);
          }}
        />
      )}
    </>
  );
};

export default forwardRef(DimensionMetricVisibleForm);
