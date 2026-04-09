import type { NsGraph } from '@antv/xflow'

/** 实体数据模型 */
export interface EntityModel {
  /** 节点id（画布/图引擎使用） */
  id?: string | number
  /** 实体id */
  entityId: string
  /** 实体name */
  entityName: string
  /** 实体类型 */
  entityType: string
  /** 实体的属性字段 */
  properties: EntityProperty[]
}

/** 属性字段数据模型 */
export interface EntityProperty {
  /** 属性id */
  propertyId: string
  /** 属性名称 */
  propertyName: string
  /** 属性类型 */
  propertyType: string
  /** 是否主键 */
  isPK?: boolean
  /** 是否外键 */
  isFK?: boolean
}

/** 画布实体渲染模型 */
export interface EntityCanvasModel extends EntityModel, NsGraph.INodeConfig {
  x?: number
  y?: number
  width?: number
  height?: number
  [key: string]: any
}

/** 画布连线渲染模型 */
export type RelationCanvasModel = NsGraph.IEdgeConfig
