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
