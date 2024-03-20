import type { EntityProperty, EntityCanvasModel, RelationCanvasModel } from './interface';

export const mockProperties: EntityProperty[] = [
  {
    propertyId: 'propertyId1',
    propertyName: '业务日期',
    propertyType: 'string',
    isPK: true,
  },
  {
    propertyId: 'propertyId2',
    propertyName: '交易号1',
    propertyType: 'bigint',
    isFK: true,
  },
  {
    propertyId: 'propertyId3',
    propertyName: '最长显示的表单名最长显示的表单名',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId4',
    propertyName: '交易支付外键',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId5',
    propertyName: '卖家支付日期',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId6',
    propertyName: '网商银行',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId7',
    propertyName: '业务日期',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId8',
    propertyName: '业务日期111',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId9',
    propertyName: '业务日期222',
    propertyType: 'string',
  },
  {
    propertyId: 'propertyId10',
    propertyName: '业务日期333',
    propertyType: 'string',
  },
];

export const mockEntityData: EntityCanvasModel[] = [
  {
    id: 'fact1',
    x: 450,
    y: 150,
    width: 214,
    height: 252,
    entityId: 'fact1',
    entityName: '模型',
    entityType: 'FACT',
    properties: mockProperties,
  },
  {
    id: 'fact2',
    x: 0,
    y: -20,
    width: 214,
    height: 252,
    entityId: 'fact2',
    entityName: '事实表2',
    entityType: 'FACT',
    properties: mockProperties,
  },
  {
    id: 'dim1',
    x: 0,
    y: 300,
    width: 214,
    height: 252,
    entityId: 'dim1',
    entityName: '维度表1',
    entityType: 'DIM',
    properties: mockProperties,
  },
  {
    id: 'other1',
    x: 180,
    y: 500,
    width: 214,
    height: 252,
    entityId: 'other1',
    entityName: '其他表1',
    entityType: 'OTHER',
    properties: mockProperties,
  },
  {
    id: 'other2',
    x: 810,
    y: 0,
    width: 214,
    height: 252,
    entityId: 'other2',
    entityName: '其他表2',
    entityType: 'OTHER',
    properties: mockProperties,
  },
];

export const mockRelationData: RelationCanvasModel[] = [
  {
    id: 'fact1-fact2',
    source: 'fact1',
    target: 'fact2',
  },
  {
    id: 'fact1-dim1',
    source: 'fact1',
    target: 'dim1',
  },
];
