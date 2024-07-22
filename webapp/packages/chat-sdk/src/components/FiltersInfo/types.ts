interface IPillBase {
  id: string;
  selected?: boolean;
}

export interface ITextFilterPill extends IPillBase {
  type: 'text-filter';
  field: string;
  fieldId: number;
  fieldName: string;
  operator: string;
  value: string | string[] | null;
}

export interface IDateFilterPill extends IPillBase {
  type: 'date-filter';
  field: string;
  fieldName: string;
  value: [string | null, string | null] | null;
}

export interface INumberFilterPill extends IPillBase {
  type: 'number-filter';
  field: string;
  fieldName: string;
  operator: string;
  value: number | null;
}

export interface IGroupPill extends IPillBase {
  type: 'group';
  fields: {
    field: string;
    fieldName: string;
  }[];
}

export interface IAggregationPill extends IPillBase {
  type: 'aggregation';
  fields: {
    field: string;
    fieldName: string;
    operator: string;
  }[];
}

export interface ITopNPill extends IPillBase {
  type: 'top-n';
  value: number;
}

export type IPill =
  | ITextFilterPill
  | IDateFilterPill
  | INumberFilterPill
  | IGroupPill
  | IAggregationPill
  | ITopNPill;

interface ICommon {
  uuid: string;
}

interface IFilterConditionBase extends ICommon {
  type: 'filter';
  field?: string;
  operator?: string;
}

export interface IStringFilterCondition extends IFilterConditionBase {
  fieldType: 'string';
  fieldId?: number;
  value: string | string[] | null;
}

export interface INumberFilterCondition extends IFilterConditionBase {
  fieldType: 'number';
  value: number | null;
}

export interface IDateFilterCondition extends IFilterConditionBase {
  fieldType: 'date';
  value: string | null;
}

export type FilterCondition =
  | IStringFilterCondition
  | INumberFilterCondition
  | IDateFilterCondition;

interface IColumnConditionBase extends ICommon {
  type: 'column';
  field?: string;
  operator?: string;
}

export interface IStringColumnCondition extends IColumnConditionBase {
  fieldType: 'string';
  value: string;
}

export interface INumberColumnCondition extends IColumnConditionBase {
  fieldType: 'number';
  value: number | number[] | null;
}

export interface IDateColumnCondition extends IColumnConditionBase {
  fieldType: 'date';
  value: string;
}

export type ColumnCondition =
  | IStringColumnCondition
  | INumberColumnCondition
  | IDateColumnCondition;

export interface ITopNCondition extends ICommon {
  type: 'topN';
  value: number | null;
}

export type Condition = FilterCondition | ColumnCondition | ITopNCondition;
