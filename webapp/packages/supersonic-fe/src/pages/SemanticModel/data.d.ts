export type ISODateString =
  `${number}-${number}-${number}T${number}:${number}:${number}.${number}+${number}:${number}`;

export type GraphConfigType = 'datasource' | 'dimension' | 'metric';
export type UserName = string;

export type SensitiveLevel = 0 | 1 | 2 | null;

export declare namespace IDataSource {
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
  interface IDataSourceDetail {
    queryType: string;
    sqlQuery: string;
    tableQuery: string;
    identifiers: IIdentifiersItem[];

    dimensions: IDimensionsItem[];
    measures: IMeasuresItem[];
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
    status: number | null;
    sensitiveLevel: SensitiveLevel;
    domainId: number;
    databaseId: number;
    datasourceDetail: IDataSourceDetail;
  }
  type IDataSourceList = IDataSourceItem[];
}

export declare namespace ISemantic {
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
    datasourceName: string;
    datasourceBizName: string;
    semanticType: string;
    alias: string;
    useCnt: number;
  }

  interface IMeasure {
    name: string;
    agg: string;
    expr: string;
    constraint: string;
    alias: string;
    createMetric: string;
    bizName: string;
    isCreateMetric: number;
    datasourceId: number;
  }
  interface ITypeParams {
    measures: IMeasure[];
    expr: string;
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
    status: number;
    typeEnum: string;
    sensitiveLevel: number;
    domainId: number;
    domainName: string;
    type: string;
    typeParams: TypeParams;
    fullPath: string;
    dataFormatType: string;
    dataFormat: string;
    alias: string;
    useCnt: number;
  }

  type IDimensionList = IDimensionItem[];
  type IMetricList = IMetricItem[];
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
}
