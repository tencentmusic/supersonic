import { TreeGraphData } from '@antv/g6-core';
import { StatusEnum } from './enum';
import { SENSITIVE_LEVEL, TAG_DEFINE_TYPE, METRIC_DEFINE_TYPE } from './constant';

export type ISODateString =
  `${number}-${number}-${number}T${number}:${number}:${number}.${number}+${number}:${number}`;

export type GraphConfigType = 'datasource' | 'dimension' | 'metric';
export type UserName = string;

export type SensitiveLevel = 0 | 1 | 2 | null;

// export type RefreshGraphData = (graphRootData: TreeGraphData) => void;

export type ToolBarSearchCallBack = (text: string) => void;

export declare namespace IDataSource {
  interface IExecuteSqlColumn {
    name?: string;
    type: string;
    nameEn: string;
    showType?: string;
    authorized?: boolean;
    dataFormatType?: string;
    dataFormat?: string;
    comment?: string;
  }

  interface IIdentifiersItem {
    name: string;
    type: string;
  }

  interface IDimensionsItem {
    name: string;
    type: string;
    expr: null;
    dateFormat: 'YYYY-MM-DD';
    typeParams: {
      isPrimary: boolean;
      timeGranularity: string;
    };
    isCreateDimension: number;
    nameCh: string;
  }

  interface IMeasuresItem {
    name: string;
    agg: string;
    expr: string;
    constraint: string;
    alias: string;
    create_metric: string;
    nameCh: string;
    isCreateMetric: number;
  }

  interface IDataSourceDetailFieldsItem {
    dataType: string;
    fieldName: string;
  }

  type ISqlParamsValueType = 'STRING' | 'EXPR' | 'NUMBER';
  interface ISqlParamsItem {
    index?: number;
    defaultValues: (boolean | string | number)[];
    name: string;
    // type: string;
    valueType: ISqlParamsValueType;
    udf?: boolean;
  }

  interface IDataSourceDetail {
    queryType: string;
    sqlQuery: string;
    tableQuery: string;
    identifiers: IIdentifiersItem[];
    fields: IDataSourceDetailFieldsItem[];
    dimensions: IDimensionsItem[];
    measures: IMeasuresItem[];
    sqlVariables: ISqlParamsItem[];
  }

  interface IDataSourceItem {
    createdBy: UserName;
    updatedBy: UserName;
    createdAt: ISODateString;
    updatedAt: ISODateString;
    id: number;
    name: string;
    bizName: string;
    description: string;
    modelId: number;
    status: number | null;
    sensitiveLevel: SensitiveLevel;
    domainId: number;
    databaseId: number;
    modelDetail: IDataSourceDetail;
  }
  type IDataSourceList = IDataSourceItem[];
}

export declare namespace ISemantic {
  interface IDomainItem {
    createdBy?: string;
    updatedBy?: string;
    createdAt?: string;
    updatedAt?: string;
    id: number;
    name: string;
    bizName: string;
    description: any;
    children: IDomainItem[];
    hasEditPermission: boolean;
    status?: number;
    typeEnum?: any;
    sensitiveLevel?: number;
    parentId: number;
    hasModel: boolean;
    fullPath?: string;
    viewers?: any[];
    viewOrgs?: any[];
    admins?: string[];
    adminOrgs?: any[];
    isOpen?: number;
    entity?: { entityId: number; names: string[] };
    dimensionCnt?: number;
    metricCnt?: number;
  }

  interface IModelItem {
    id: number;
    name: string;
    bizName: string;
    description: string;
    sensitiveLevel: SensitiveLevel;
    domainId: number;
    databaseId: number;
    modelDetail: IDataSourceDetail;
    status?: StatusEnum;
    typeEnum?: any;
    sensitiveLevel?: number;
    parentId: number;
    filterSql: string;
    alias: string;
    fullPath?: string;
    viewers?: any[];
    viewOrgs?: any[];
    admins?: string[];
    adminOrgs?: any[];
    tagObjectId?: number;
    drillDownDimensions: IDrillDownDimensionItem[];
    createdBy: UserName;
    updatedBy: UserName;
    createdAt: ISODateString;
    updatedAt: ISODateString;
  }

  interface IDatasetModelConfigItem {
    id: number;
    includesAll: boolean;
    metrics: number[];
    dimensions: number[];
    tagIds: number[];
  }

  interface IDatasetItem {
    createdBy: UserName;
    updatedBy: UserName;
    createdAt: ISODateString;
    updatedAt: ISODateString;
    id: number;
    name: string;
    bizName: string;
    description: string;
    status?: StatusEnum;
    typeEnum?: any;
    sensitiveLevel: number;
    domainId: number;
    dataSetDetail: {
      dataSetModelConfigs: IDatasetModelConfigItem[];
    };
  }

  interface IDimensionItem {
    createdBy: string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
    id: number;
    name: string;
    bizName: string;
    description: string;
    status: number;
    typeEnum: any;
    sensitiveLevel: number;
    domainId: number;
    type: string;
    expr: string;
    fullPath: string;
    datasourceId: number;
    modelId: number;
    modelName: string;
    datasourceName: string;
    datasourceBizName: string;
    commonDimensionId: number;
    semanticType: string;
    alias: string;
    useCnt: number;
    dimValueMaps: IDimensionValueSettingItem[];
  }

  interface IDimensionValueSettingItem {
    techName: string;
    bizName: string;
    alias?: string[];
  }
  interface IMeasure {
    name: string;
    agg?: string;
    expr: string;
    constraint?: string;
    alias?: string;
    createMetric?: string;
    bizName: string;
    isCreateMetric?: number;
    datasourceId: number;
  }

  interface IFieldTypeParamsItem {
    fieldName: string;
  }

  interface IMetricTypeParamsItem {
    id: number;
    bizName: string;
  }

  interface IDimensionTypeParamsItem {
    id: number;
    bizName: string;
  }

  interface IMeasureTypeParams {
    measures: IMeasure[];
    expr: string;
  }

  interface IMetricTypeParams {
    expr: string;
    metrics: IMetricTypeParamsItem[];
  }
  interface IFieldTypeParams {
    expr: string;
    fields: IFieldTypeParamsItem[];
  }

  interface IDrillDownDimensionItem {
    dimensionId: number;
    inheritedFromModel?: boolean;
    necessary?: boolean;
  }

  interface IRelateDimension {
    drillDownDimensions: IDrillDownDimensionItem[];
  }

  interface IMetricItem {
    createdBy: string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
    id: number;
    name: string;
    bizName: string;
    description: string;
    status: StatusEnum;
    typeEnum: string;
    sensitiveLevel: SENSITIVE_LEVEL;
    domainId: number;
    domainName: string;
    modelName: string;
    modelId: number;
    modelBizName: string;
    hasAdminRes: boolean;
    type: string;
    classifications: string[];
    isTag: 0 | 1;
    // typeParams: IMeasureTypeParams;
    metricDefineType: METRIC_DEFINE_TYPE;
    metricDefineByMeasureParams: IMeasureTypeParams;
    metricDefineByFieldParams: IFieldTypeParams;
    metricDefineByMetricParams: IMetricTypeParams;
    fullPath: string;
    dataFormatType: string;
    dataFormat: string;
    alias: string;
    useCnt: number;
    isCollect: boolean;
    isPublish: boolean;
    relateDimension?: IRelateDimension;
  }

  interface IMetricTrendColumn {
    name: string;
    type: string;
    nameEn: string;
    showType: string;
    authorized: boolean;
    dataFormatType: string;
    dataFormat: {
      needMultiply100: boolean;
      decimalPlaces: number;
    };
  }

  type IMetricTrendItem = Record<string, any>;
  interface IMetricTrend {
    columns: IMetricTrendColumn;
    resultList: IMetricTrendItem[];
    pageNo?: number;
    pageSize?: number;
    totalCount?: number;
    queryAuthorization?: string;
    sql?: string;
  }

  type IDimensionList = IDimensionItem[];
  type IMetricList = IMetricItem[];

  interface IDomainSchemaRelaItem {
    domainId: number;
    dimensions: IDimensionList;
    metrics: IMetricList;
    model: IDataSourceItem;
  }
  type IDomainSchemaRelaList = IDomainSchemaRelaItem[];

  interface IDatabaseItem {
    createdBy?: string;
    updatedBy?: string;
    createdAt?: string;
    updatedAt?: string;
    id: number;
    name: string;
    admins: string[];
    type: string;
    url: string;
    username: string;
    password: string;
    version: string;
    hasEditPermission: boolean;
    hasUsePermission: boolean;
    host: string;
    port: string;
    database?: string;
    description?: string;
  }
  type IDatabaseItemList = IDatabaseItem[];

  interface IDictKnowledgeConfigItemConfig {
    metricId?: number;
    blackList: string[];
    whiteList: string[];
    ruleList: any[];
    limit?: number;
  }
  interface IDictKnowledgeConfigItem {
    id: number;
    modelId: number;
    bizName: string;
    type: string;
    itemId: number;
    config: IDictKnowledgeConfigItemConfig;
    status: string;
    nature?: string;
  }

  interface IDictKnowledgeTaskItem {
    id: number;
    modelId: number;
    bizName: string;
    type: string;
    itemId: number;
    config: IDictKnowledgeConfigItemConfig;
    status: string | null;
    name: string;
    description: string | null;
    taskStatus: string;
    createdAt: string;
    createdBy: string;
    elapsedMs: number | null;
    nature: string;
  }

  interface ITagDefineParams {
    expr: string;
    dependencies: (number | string)[];
  }
  interface ITagItem {
    createdBy: string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
    id: number;
    name: string;
    modelName: string;
    bizName: string;
    description: string;
    status: number;
    typeEnum: null;
    sensitiveLevel: SENSITIVE_LEVEL;
    modelId: number;
    type: string;
    isCollect: boolean;
    hasAdminRes: boolean;
    ext: any;
    tagDefineType: TAG_DEFINE_TYPE;
    tagDefineParams: ITagDefineParams;
    expr: string;
  }

  interface ITagObjectItem {
    createdBy: string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
    id: number;
    name: string;
    bizName: string;
    description: string;
    status: number;
    typeEnum: null;
    sensitiveLevel: SENSITIVE_LEVEL;
    domainId: number;
    ext: null;
  }

  interface ITermItem {
    id: number;
    name: string;
    description: string;
    similarTerms: string[];
  }
}

export declare namespace IChatConfig {
  interface IEntity {
    domainId: number;
    domainName: string;
    domainBizName: string;
    names: string[];
    entityIds: ISemantic.IMetricItem[];
    entityInternalDetailDesc: {
      dimensionList: ISemantic.IDimensionList;
      metricList: ISemantic.IMetricList;
    };
  }

  interface IKnowledgeInfosItem {
    itemId: number;
    bizName: string;
    type?: string;
    searchEnable?: boolean;
    knowledgeAdvancedConfig?: IKnowledgeConfig;
  }

  type IKnowledgeInfosItemMap = Record<IKnowledgeInfosItem.bizName, IKnowledgeInfosItem>;

  interface IKnowledgeConfig {
    blackList: string[];
    whiteList: string[];
    ruleList: string[];
  }

  interface IChatRichConfig {
    id?: number;
    visibility: {
      blackDimIdList: number[];
      blackMetricIdList: number[];
      whiteDimIdList: number[];
      whiteMetricIdList: number[];
    };
    entity: {
      names: string[];
      dimItem: ISemantic.IDimensionItem;
    };
    knowledgeInfos: IKnowledgeInfosItem[];
    globalKnowledgeConfig: IKnowledgeConfig;
    chatDefaultConfig: {
      dimensions: ISemantic.IDimensionList;
      metrics: ISemantic.IMetricList;
      ratioMetrics: ISemantic.IMetricList;
      unit: number;
      period: string;
    };
  }
}
