import React from 'react';
import { Radio, Tag } from 'antd';
import MetricMeasuresFormTable from './MetricMeasuresFormTable';
import MetricMetricFormTable from './MetricMetricFormTable';
import MetricFieldFormTable from './MetricFieldFormTable';
import { METRIC_DEFINE_TYPE } from '../constant';
import { ISemantic } from '../data';
import styles from './style.less';

interface MetricExpressionEditorProps {
  defineType: METRIC_DEFINE_TYPE;
  onDefineTypeChange: (type: METRIC_DEFINE_TYPE) => void;
  exprTypeParamsState: {
    [METRIC_DEFINE_TYPE.MEASURE]: ISemantic.IMeasureTypeParams;
    [METRIC_DEFINE_TYPE.METRIC]: ISemantic.IMetricTypeParams;
    [METRIC_DEFINE_TYPE.FIELD]: ISemantic.IFieldTypeParams;
  };
  onExprTypeParamsChange: (
    updater: (
      prevState: {
        [METRIC_DEFINE_TYPE.MEASURE]: ISemantic.IMeasureTypeParams;
        [METRIC_DEFINE_TYPE.METRIC]: ISemantic.IMetricTypeParams;
        [METRIC_DEFINE_TYPE.FIELD]: ISemantic.IFieldTypeParams;
      },
    ) => {
      [METRIC_DEFINE_TYPE.MEASURE]: ISemantic.IMeasureTypeParams;
      [METRIC_DEFINE_TYPE.METRIC]: ISemantic.IMetricTypeParams;
      [METRIC_DEFINE_TYPE.FIELD]: ISemantic.IFieldTypeParams;
    },
  ) => void;
  classMeasureList: ISemantic.IMeasure[];
  createNewMetricList: ISemantic.IMetricItem[];
  fieldList: ISemantic.IFieldTypeParamsItem[];
  datasourceId?: number;
}

const MetricExpressionEditor: React.FC<MetricExpressionEditorProps> = ({
  defineType,
  onDefineTypeChange,
  exprTypeParamsState,
  onExprTypeParamsChange,
  classMeasureList,
  createNewMetricList,
  fieldList,
  datasourceId,
}) => {
  return (
    <>
      <Radio.Group
        buttonStyle="solid"
        value={defineType}
        onChange={(e) => {
          onDefineTypeChange(e.target.value);
        }}
      >
        <Radio.Button value={METRIC_DEFINE_TYPE.MEASURE}>按度量</Radio.Button>
        <Radio.Button value={METRIC_DEFINE_TYPE.METRIC}>按指标</Radio.Button>
        <Radio.Button value={METRIC_DEFINE_TYPE.FIELD}>按字段</Radio.Button>
      </Radio.Group>

      {defineType === METRIC_DEFINE_TYPE.MEASURE && (
        <>
          <MetricMeasuresFormTable
            datasourceId={datasourceId}
            typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.MEASURE]}
            measuresList={classMeasureList}
            onFieldChange={(measures: ISemantic.IMeasure[]) => {
              onExprTypeParamsChange((prevState) => {
                return {
                  ...prevState,
                  [METRIC_DEFINE_TYPE.MEASURE]: {
                    ...prevState[METRIC_DEFINE_TYPE.MEASURE],
                    measures,
                  },
                };
              });
            }}
            onSqlChange={(expr: string) => {
              onExprTypeParamsChange((prevState) => {
                return {
                  ...prevState,
                  [METRIC_DEFINE_TYPE.MEASURE]: {
                    ...prevState[METRIC_DEFINE_TYPE.MEASURE],
                    expr,
                  },
                };
              });
            }}
          />
        </>
      )}
      {defineType === METRIC_DEFINE_TYPE.METRIC && (
        <>
          <p className={styles.desc}>
            基于
            <Tag color="#2499ef14" className={styles.markerTag}>
              已有
            </Tag>
            指标来衍生新的指标
          </p>

          <MetricMetricFormTable
            typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.METRIC]}
            metricList={createNewMetricList}
            onFieldChange={(metrics: ISemantic.IMetricTypeParamsItem[]) => {
              onExprTypeParamsChange((prevState) => {
                return {
                  ...prevState,
                  [METRIC_DEFINE_TYPE.METRIC]: {
                    ...prevState[METRIC_DEFINE_TYPE.METRIC],
                    metrics,
                  },
                };
              });
            }}
            onSqlChange={(expr: string) => {
              onExprTypeParamsChange((prevState) => {
                return {
                  ...prevState,
                  [METRIC_DEFINE_TYPE.METRIC]: {
                    ...prevState[METRIC_DEFINE_TYPE.METRIC],
                    expr,
                  },
                };
              });
            }}
          />
        </>
      )}
      {defineType === METRIC_DEFINE_TYPE.FIELD && (
        <>
          <MetricFieldFormTable
            typeParams={exprTypeParamsState[METRIC_DEFINE_TYPE.FIELD]}
            fieldList={fieldList}
            onFieldChange={(fields: ISemantic.IFieldTypeParamsItem[]) => {
              onExprTypeParamsChange((prevState) => {
                return {
                  ...prevState,
                  [METRIC_DEFINE_TYPE.FIELD]: {
                    ...prevState[METRIC_DEFINE_TYPE.FIELD],
                    fields,
                  },
                };
              });
            }}
            onSqlChange={(expr: string) => {
              onExprTypeParamsChange((prevState) => {
                return {
                  ...prevState,
                  [METRIC_DEFINE_TYPE.FIELD]: {
                    ...prevState[METRIC_DEFINE_TYPE.FIELD],
                    expr,
                  },
                };
              });
            }}
          />
        </>
      )}
    </>
  );
};

export default MetricExpressionEditor;
